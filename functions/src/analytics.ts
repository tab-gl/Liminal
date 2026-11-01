import { onSchedule } from "firebase-functions/v2/scheduler";
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { db, COLLECTIONS, FieldValue, Timestamp } from "./config";
import { dayKey } from "./util";

/**
 * Rolls raw application events into per-application counters and daily
 * analytics documents, then deletes the raw events.
 *
 * Everything a developer sees on the dashboard comes from this job - which is
 * why those numbers are trustworthy, and why the client is forbidden from
 * writing them.
 */
export const aggregateApplicationEvents = onSchedule(
  { schedule: "every 60 minutes", timeZone: "Asia/Kolkata", timeoutSeconds: 540 },
  async () => {
    const stateRef = db.collection("config").doc("aggregation");
    const stateSnap = await stateRef.get();
    const since: Date = stateSnap.data()?.lastRun?.toDate?.() ?? new Date(Date.now() - 24 * 3600 * 1000);

    const events = await db
      .collection(COLLECTIONS.events)
      .where("createdAt", ">", Timestamp.fromDate(since))
      .orderBy("createdAt")
      .limit(4000)
      .get();

    if (events.empty) {
      await stateRef.set({ lastRun: FieldValue.serverTimestamp() }, { merge: true });
      return;
    }

    type Bucket = {
      views: number;
      clicks: number;
      downloads: number;
      shares: number;
      viewers: Set<string>;
      byDay: Map<string, { views: number; clicks: number; downloads: number }>;
    };

    const buckets = new Map<string, Bucket>();

    for (const doc of events.docs) {
      const data = doc.data();
      const applicationId = String(data.applicationId ?? "");
      if (!applicationId) continue;

      const bucket =
        buckets.get(applicationId) ??
        { views: 0, clicks: 0, downloads: 0, shares: 0, viewers: new Set<string>(), byDay: new Map() };
      const createdAt: Date = data.createdAt?.toDate?.() ?? new Date();
      const key = dayKey(createdAt);
      const day = bucket.byDay.get(key) ?? { views: 0, clicks: 0, downloads: 0 };

      switch (data.type) {
        case "view":
          bucket.views += 1;
          day.views += 1;
          if (typeof data.userId === "string") bucket.viewers.add(data.userId);
          break;
        case "click":
          bucket.clicks += 1;
          day.clicks += 1;
          break;
        case "download":
          bucket.downloads += 1;
          day.downloads += 1;
          break;
        case "share":
          bucket.shares += 1;
          break;
        default:
          break;
      }

      bucket.byDay.set(key, day);
      buckets.set(applicationId, bucket);
    }

    const batch = db.batch();
    const now = Date.now();

    for (const [applicationId, bucket] of buckets) {
      const appRef = db.collection(COLLECTIONS.applications).doc(applicationId);

      // Trending blends recent views and clicks with a decay so old spikes fade.
      const trendingScore = Math.round(
        bucket.views * 1.0 + bucket.clicks * 2.5 + bucket.viewers.size * 1.5,
      );

      batch.set(
        appRef,
        {
          viewCount: FieldValue.increment(bucket.views),
          uniqueViewerCount: FieldValue.increment(bucket.viewers.size),
          clickCount: FieldValue.increment(bucket.clicks),
          downloadCount: FieldValue.increment(bucket.downloads),
          trendingScore: FieldValue.increment(trendingScore),
          lastActivityAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );

      for (const [day, stats] of bucket.byDay) {
        batch.set(
          appRef.collection("analytics").doc(day),
          {
            views: FieldValue.increment(stats.views),
            clicks: FieldValue.increment(stats.clicks),
            downloads: FieldValue.increment(stats.downloads),
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true },
        );
      }
    }

    // Raw events have served their purpose; keeping them would grow forever.
    for (const doc of events.docs) {
      batch.delete(doc.ref);
    }
    batch.set(stateRef, { lastRun: FieldValue.serverTimestamp(), processed: events.size }, { merge: true });

    await batch.commit();

    // Decay stale trending scores so the "Trending" rail cannot ossify.
    const stale = await db
      .collection(COLLECTIONS.applications)
      .where("trendingScore", ">", 0)
      .where("lastActivityAt", "<", Timestamp.fromDate(new Date(now - 7 * 24 * 3600 * 1000)))
      .limit(200)
      .get();
    await Promise.all(
      stale.docs.map((doc) =>
        doc.ref.update({ trendingScore: Math.floor(Number(doc.data().trendingScore ?? 0) * 0.5) }),
      ),
    );
  },
);

/** Bookmark counters, maintained immediately rather than on the hourly job. */
export const onBookmarkWritten = onDocumentWritten("bookmarks/{bookmarkId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  const applicationId = after?.applicationId ?? before?.applicationId;
  if (!applicationId) return;

  const delta = (after ? 1 : 0) - (before ? 1 : 0);
  if (delta === 0) return;

  await db.collection(COLLECTIONS.applications).doc(applicationId).set(
    { bookmarkCount: FieldValue.increment(delta) },
    { merge: true },
  );
});

/** Follower counters. */
export const onFollowWritten = onDocumentWritten("developer_follows/{followId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  const developerId = after?.developerId ?? before?.developerId;
  if (!developerId) return;

  const delta = (after ? 1 : 0) - (before ? 1 : 0);
  if (delta === 0) return;

  await db.collection(COLLECTIONS.users).doc(developerId).set(
    { followerCount: FieldValue.increment(delta) },
    { merge: true },
  );
});
