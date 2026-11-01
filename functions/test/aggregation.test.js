const { test } = require("node:test");
const assert = require("node:assert");
const { summarizeEvents, trendingDelta, CHUNK } = require("../lib/aggregation");

test("events are bucketed per application and day", () => {
  const out = summarizeEvents([
    { applicationId: "a", userId: "u1", type: "view", day: "2026-11-01" },
    { applicationId: "a", userId: "u2", type: "view", day: "2026-11-01" },
    { applicationId: "a", userId: "u1", type: "click", day: "2026-11-02" },
    { applicationId: "b", userId: "u1", type: "download", day: "2026-11-01" },
    { applicationId: "b", userId: "u1", type: "bogus", day: "2026-11-01" },
    { userId: "u1", type: "view", day: "2026-11-01" },
  ]);
  assert.deepStrictEqual([...out.keys()].sort(), ["a", "b"]);
  const a = out.get("a");
  assert.strictEqual(a.views, 2);
  assert.strictEqual(a.clicks, 1);
  assert.strictEqual(a.viewers.size, 2);
  assert.strictEqual(a.byDay.get("2026-11-01").views, 2);
  assert.strictEqual(a.byDay.get("2026-11-02").clicks, 1);
  assert.strictEqual(out.get("b").downloads, 1);
});

test("trending blends views, clicks and distinct viewers", () => {
  const out = summarizeEvents([
    { applicationId: "a", userId: "u1", type: "view", day: "d" },
    { applicationId: "a", userId: "u1", type: "click", day: "d" },
  ]);
  assert.strictEqual(trendingDelta(out.get("a")), Math.round(1 + 2.5 + 1.5));
});

test("a chunk can never exceed Firestore's 500-write batch limit", () => {
  // Worst case: one delete, one app update and one daily doc per event.
  assert.ok(CHUNK * 3 <= 500);
});
