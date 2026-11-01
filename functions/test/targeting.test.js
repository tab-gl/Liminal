const { test } = require("node:test");
const assert = require("node:assert");
const { matchesTargeting, buildAdContext } = require("../lib/targeting");

test("empty targeting matches everyone", () => {
  assert.ok(matchesTargeting({}, {}));
  assert.ok(matchesTargeting(undefined, { platform: "ios" }));
  assert.ok(matchesTargeting({ categoryIds: [], platforms: [], countries: [] }, {}));
});

test("category targeting only matches the requested category", () => {
  const t = { categoryIds: ["design"] };
  assert.ok(matchesTargeting(t, { categoryId: "design" }));
  assert.ok(!matchesTargeting(t, { categoryId: "finance" }));
  assert.ok(!matchesTargeting(t, {}), "a category campaign must not leak onto the general feed");
});

test("platform and country targeting are enforced", () => {
  const t = { platforms: ["android"], countries: ["IN", "us"] };
  assert.ok(matchesTargeting(t, { platform: "android", country: "IN" }));
  assert.ok(matchesTargeting(t, { platform: "android", country: "US" }));
  assert.ok(!matchesTargeting(t, { platform: "ios", country: "IN" }));
  assert.ok(!matchesTargeting(t, { platform: "android", country: "DE" }));
  assert.ok(!matchesTargeting(t, { platform: "android" }));
});

test("every dimension must match, not just one", () => {
  const t = { categoryIds: ["design"], platforms: ["ios"] };
  assert.ok(!matchesTargeting(t, { categoryId: "design", platform: "android" }));
  assert.ok(matchesTargeting(t, { categoryId: "design", platform: "ios" }));
});

test("context building normalises and rejects junk", () => {
  assert.deepStrictEqual(buildAdContext({ platform: "IOS", country: "in", categoryId: "x" }), {
    platform: "ios", country: "IN", categoryId: "x",
  });
  assert.strictEqual(buildAdContext({ platform: "toaster" }).platform, undefined);
  assert.strictEqual(buildAdContext({ country: "India" }).country, undefined);
  assert.strictEqual(buildAdContext({ country: "IN" }, { "x-appengine-country": "us" }).country, "US");
});
