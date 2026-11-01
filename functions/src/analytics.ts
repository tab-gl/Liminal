import { onSchedule } from "firebase-functions/v2/scheduler";
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { db, COLLECTIONS, FieldValue, Timestamp } from "./config";
import { CHUNK, summarizeEvents, trendingDelta, type RawEvent } from "./aggregation";
import { dayKey } from "./util";

/** Stop starting new chunks after this long, leaving headroom under the 540s timeout. */
const BUDGET_MS = 450_000;

/**
 * Rolls raw application events into per-application counters and daily
 * analytics documents, then deletes the raw events.
 *
 * Each chunk applies its counters and deletes its events in ONE batch, so a chunk
 * is atomic: either the counters moved and the events are gone, or neither
 * happened. There is no checkpoint to lose and a retry can never double count.
 * A chunk is at most 150 events, i.e. at most 450 writes, under Firestore's
 * 500-write batch limit.
 */
export const aggregateApplicationEvents = onSchedule(
  { schedule: "every 30 minutes", timeZone: "Asia/Kolkata", timeoutSeconds: 540 },
  async () => {
    const started = Date.now();

    while (Date.now() - started < BUDGET_MS) {
      const events = await db
        .collection(COLLECTIONS.events)
        .orderBy("createdAt")
        .limit(CHUNK)
        .get();
      if (events.empty) break;

      const raw: RawEvent[] = events.docs.map((doc) => {
        const data = doc.data();
        return {
          applicationId: data.applicationId,
          userId: data.userId,
          type: data.type,
          day: dayKey(data.createdAt?.toDate?.() ?? new Date()),
        };
      });
      const buckets = summarizeEvents(raw);

      // Only credit applications that still exist; a merge-set would resurrect
      // a deleted listing as a ghost document.
      const appRefs = [...buckets.keys()].map((id) => db.collection(COLLECTIONS.applications).doc(id));
      const appSnaps = appRefs.length > 0 ? await db.getAll(...appRefs) : [];
      const existing = new Set(appSnaps.filter((snap) => snap.exists).map((snap) => snap.id));

      const batch = db.batch();
      for (const [applicationId, bucket] of buckets) {
        if (!existing.has(applicationId)) continue;
        const appRef = db.collection(COLLECTIONS.applications).doc(applicationId);

        batch.update(appRef, {
          viewCount: FieldValue.increment(bucket.views),
          uniqueViewerCount: FieldValue.increment(bucket.viewers.size),
          clickCount: FieldValue.increment(bucket.clicks),
          downloadCount: FieldValue.increment(bucket.downloads),
          trendingScore: FieldValue.increment(trendingDelta(bucket)),
          lastActivityAt: FieldValue.serverTimestamp(),
        });

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
      for (const doc of events.docs) batch.delete(doc.ref);
      await batch.commit();

      if (events.size < CHUNK) break;
    }

    await pruneMarkers();
    await decayTrending();
  },
);

/** Dedupe markers only need to outlive the longest bucket (a day). */
async function pruneMarkers(): Promise<void> {
  const cutoff = Timestamp.fromMillis(Date.now() - 2 * 24 * 3600 * 1000);
  for (let i = 0; i < 20; i++) {
    const stale = await db
      .collection(COLLECTIONS.eventMarkers)
      .where("createdAt", "<", cutoff)
      .limit(400)
      .get();
    if (stale.empty) return;
    const batch = db.batch();
    for (const doc of stale.docs) batch.delete(doc.ref);
    await batch.commit();
    if (stale.size < 400) return;
  }
}

/** Decay stale trending scores so the "Trending" rail cannot ossify. */
async function decayTrending(): Promise<void> {
  const stale = await db
    .collection(COLLECTIONS.applications)
    .where("trendingScore", ">", 0)
    .where("lastActivityAt", "<", Timestamp.fromMillis(Date.now() - 7 * 24 * 3600 * 1000))
    .limit(200)
    .get();
  await Promise.all(
    stale.docs.map((doc) =>
      doc.ref.update({ trendingScore: Math.floor(Number(doc.data().trendingScore ?? 0) * 0.5) }),
    ),
  );
}

/** Bookmark counters, maintained immediately rather than on the periodic job. */
export const onBookmarkWritten = onDocumentWritten("bookmarks/{bookmarkId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  const applicationId = after?.applicationId ?? before?.applicationId;
  if (!applicationId) return;

  const delta = (after ? 1 : 0) - (before ? 1 : 0);
  if (delta === 0) return;

  const ref = db.collection(COLLECTIONS.applications).doc(applicationId);
  if (!(await ref.get()).exists) return;
  await ref.update({ bookmarkCount: FieldValue.increment(delta) });
});

/** Follower counters. */
export const onFollowWritten = onDocumentWritten("developer_follows/{followId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  const developerId = after?.developerId ?? before?.developerId;
  if (!developerId) return;

  const delta = (after ? 1 : 0) - (before ? 1 : 0);
  if (delta === 0) return;

  const ref = db.collection(COLLECTIONS.users).doc(developerId);
  if (!(await ref.get()).exists) return;
  await ref.update({ followerCount: FieldValue.increment(delta) });
});
