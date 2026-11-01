/**
 * Storage security rule tests (run with the Firestore and Storage emulators:
 * `npm test` in this folder).
 */
const { test, before, after } = require("node:test");
const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");
const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { doc, setDoc } = require("firebase/firestore");
const { ref, uploadBytes } = require("firebase/storage");

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "sproutly-storage-test",
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, "../../firestore.rules"), "utf8"),
      host: "127.0.0.1",
      port: 8080,
    },
    storage: {
      rules: fs.readFileSync(path.resolve(__dirname, "../../storage.rules"), "utf8"),
      host: "127.0.0.1",
      port: 9199,
    },
  });
  await env.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), "applications/app-1"), { ownerId: "owner-1", status: "approved" });
  });
});

after(async () => {
  await env.cleanup();
});

const storageFor = (uid) => env.authenticatedContext(uid, { role: "user" }).storage();
const png = () => new Uint8Array([137, 80, 78, 71]);

test("image/jpeg uploads are accepted; image/jpg is not a valid type", async () => {
  const s = storageFor("owner-1");
  await assertSucceeds(uploadBytes(ref(s, "app_icons/app-1/a.jpg"), png(), { contentType: "image/jpeg" }));
  await assertFails(uploadBytes(ref(s, "app_icons/app-1/b.jpg"), png(), { contentType: "image/jpg" }));
});

test("png and webp are accepted, other types are not", async () => {
  const s = storageFor("owner-1");
  await assertSucceeds(uploadBytes(ref(s, "screenshots/app-1/a.png"), png(), { contentType: "image/png" }));
  await assertSucceeds(uploadBytes(ref(s, "screenshots/app-1/a.webp"), png(), { contentType: "image/webp" }));
  await assertFails(uploadBytes(ref(s, "screenshots/app-1/a.gif"), png(), { contentType: "image/gif" }));
  await assertFails(uploadBytes(ref(s, "screenshots/app-1/a.html"), png(), { contentType: "text/html" }));
});

test("only the listing owner can write to the listing's folders", async () => {
  await assertFails(
    uploadBytes(ref(storageFor("stranger-1"), "banners/app-1/x.png"), png(), { contentType: "image/png" }),
  );
});

test("users can stage uploads only under their own uid", async () => {
  await assertSucceeds(
    uploadBytes(ref(storageFor("owner-1"), "app_icons/owner-1/draft.png"), png(), { contentType: "image/png" }),
  );
  await assertFails(
    uploadBytes(ref(storageFor("owner-1"), "app_icons/someone-else/draft.png"), png(), { contentType: "image/png" }),
  );
});

test("avatars are writable only by their owner", async () => {
  await assertSucceeds(
    uploadBytes(ref(storageFor("owner-1"), "avatars/owner-1/me.png"), png(), { contentType: "image/png" }),
  );
  await assertFails(
    uploadBytes(ref(storageFor("stranger-1"), "avatars/owner-1/me.png"), png(), { contentType: "image/png" }),
  );
});

test("unknown paths are denied", async () => {
  await assertFails(
    uploadBytes(ref(storageFor("owner-1"), "misc/owner-1/x.png"), png(), { contentType: "image/png" }),
  );
});
