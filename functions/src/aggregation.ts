/** Event roll-up maths. Pure, so it can be unit tested without Firebase. */

export interface RawEvent {
  applicationId?: unknown;
  userId?: unknown;
  type?: unknown;
  day: string;
}

export interface DayStats {
  views: number;
  clicks: number;
  downloads: number;
}

export interface AppBucket {
  views: number;
  clicks: number;
  downloads: number;
  shares: number;
  viewers: Set<string>;
  byDay: Map<string, DayStats>;
}

export function summarizeEvents(events: RawEvent[]): Map<string, AppBucket> {
  const buckets = new Map<string, AppBucket>();
  for (const event of events) {
    const applicationId = String(event.applicationId ?? "");
    if (!applicationId) continue;
    const bucket =
      buckets.get(applicationId) ??
      { views: 0, clicks: 0, downloads: 0, shares: 0, viewers: new Set<string>(), byDay: new Map() };
    const day = bucket.byDay.get(event.day) ?? { views: 0, clicks: 0, downloads: 0 };

    switch (event.type) {
      case "view":
        bucket.views += 1;
        day.views += 1;
        if (typeof event.userId === "string") bucket.viewers.add(event.userId);
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
    bucket.byDay.set(event.day, day);
    buckets.set(applicationId, bucket);
  }
  return buckets;
}

export function trendingDelta(bucket: AppBucket): number {
  return Math.round(bucket.views * 1.0 + bucket.clicks * 2.5 + bucket.viewers.size * 1.5);
}

/** Events per chunk. Worst case one chunk writes 3 x CHUNK docs, under Firestore's 500 limit. */
export const CHUNK = 150;
