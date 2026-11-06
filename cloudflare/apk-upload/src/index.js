/**
 * Sproutly build uploader.
 *
 * APKs are far too big for Firestore (1 MiB per document) and Cloudinary's
 * unsigned preset rejects ZIP content and caps at 10 MB, so builds go to R2.
 *
 * The bucket must never be writable with credentials that ship inside the app —
 * anyone could extract them and fill it. So the app proves who it is with its
 * Firebase ID token, this Worker verifies that token with Google, and only then
 * does the write happen with the Worker's own R2 binding.
 *
 *   POST /upload   Authorization: Bearer <firebase idToken>
 *                  X-File-Name: app-release.apk
 *                  body: raw APK bytes
 *   GET  /apks/<key...>   public download
 */
const MAX_BYTES = 100 * 1024 * 1024;

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Authorization, Content-Type, X-File-Name",
  "Access-Control-Max-Age": "86400",
};

const json = (body, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", ...CORS },
  });

/** Verifies the Firebase ID token and returns the uid, or null. */
async function verifyToken(token, apiKey) {
  if (!token || !apiKey) return null;
  const response = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${apiKey}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idToken: token }),
    },
  );
  if (!response.ok) return null;
  const data = await response.json();
  return data?.users?.[0]?.localId ?? null;
}

function safeName(name) {
  return (name || "build.apk").replace(/[^A-Za-z0-9._-]/g, "_").slice(-60);
}

export default {
  async fetch(request, env) {
    try {
      return await handle(request, env);
    } catch (error) {
      // Error 1101 (an uncaught exception) tells a person nothing and cannot be
      // read from a phone. Every failure becomes JSON that says what broke.
      return json(
        {
          error: "Worker threw an exception.",
          message: String(error && error.message ? error.message : error),
          stack: String(error && error.stack ? error.stack : "").split("\n").slice(0, 4),
          hint:
            "If this mentions APKS, the R2 binding is missing on this version. " +
            "If it mentions FIREBASE_API_KEY, the secret is missing. " +
            "Note that the dashboard Preview pane has no bindings — test the " +
            "production URL in a normal tab.",
        },
        500,
      );
    }
  },
};

async function handle(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: CORS });
    }

    // ---------------------------------------------------------------- health
    // Answers "is this deployment wired up?" without uploading anything. If a
    // download fails, open this URL first: it says whether the R2 binding and the
    // Firebase key are actually present on the running version.
    if (request.method === "GET" && url.pathname === "/health") {
      const bound = Boolean(env.APKS);
      let bucket = null;
      let sampleCount = null;
      if (bound) {
        try {
          const list = await env.APKS.list({ limit: 3 });
          bucket = list.objects.map((o) => o.key);
          sampleCount = bucket.length;
        } catch (error) {
          return json({
            ok: false,
            binding: "APKS",
            bucketError: String(error),
            hint: "The binding exists but the bucket cannot be read. Check that the bucket name in the binding matches the bucket you created.",
          }, 500);
        }
      }
      return json({
        ok: bound && Boolean(env.FIREBASE_API_KEY),
        binding: bound ? "APKS" : "MISSING",
        firebaseKey: env.FIREBASE_API_KEY ? "set" : "MISSING",
        sampleKeys: bucket,
        sampleCount,
        hint: bound
          ? "Binding present. A 404 on a download means the key is wrong; a 500 means the bucket read failed."
          : "No R2 binding called APKS. Add it in the Worker's Settings → Bindings, then redeploy.",
      });
    }

    // ---------------------------------------------------------------- upload
    if (request.method === "POST" && url.pathname === "/upload") {
      const auth = request.headers.get("Authorization") || "";
      const token = auth.startsWith("Bearer ") ? auth.slice(7) : "";
      const uid = await verifyToken(token, env.FIREBASE_API_KEY);
      if (!uid) return json({ error: "Sign in first." }, 401);

      const declared = Number(request.headers.get("Content-Length") || "0");
      if (declared > MAX_BYTES) {
        return json(
          { error: `That build is ${(declared / 1048576).toFixed(1)} MB. The limit is 100 MB.` },
          413,
        );
      }

      const bytes = await request.arrayBuffer();
      if (bytes.byteLength === 0) return json({ error: "That file is empty." }, 400);
      if (bytes.byteLength > MAX_BYTES) {
        return json({ error: "That build is over 100 MB." }, 413);
      }

      const key = `apks/${uid}/${Date.now()}-${safeName(request.headers.get("X-File-Name"))}`;
      await env.APKS.put(key, bytes, {
        httpMetadata: {
          contentType: "application/vnd.android.package-archive",
          contentDisposition: `attachment; filename="${safeName(request.headers.get("X-File-Name"))}"`,
        },
      });

      // The key already begins with "apks/". Prefixing it again produced
      // /apks/apks/<uid>/… and the download then looked up a key that did not
      // exist, so every build link 404'd.
      return json({
        url: `${url.origin}/${key}`,
        path: key,
        size: bytes.byteLength,
      });
    }

    // -------------------------------------------------------------- download
    if ((request.method === "GET" || request.method === "HEAD") &&
        url.pathname.startsWith("/apks/")) {
      let key = decodeURIComponent(url.pathname.slice(1));
      // Links issued before the fix carry an extra "apks/"; collapse it so
      // listings published yesterday still download today.
      if (key.startsWith("apks/apks/")) key = key.slice("apks/".length);

      const object = await env.APKS.get(key);
      if (!object) {
        return json({
          error: "Not found.",
          key,
          hint: "No object at this key. Open /health to list a few real keys and compare.",
        }, 404);
      }

      const headers = {
        "Content-Type":
          object.httpMetadata?.contentType || "application/vnd.android.package-archive",
        "Content-Disposition":
          object.httpMetadata?.contentDisposition || 'attachment; filename="build.apk"',
        // Lets the app show a real percentage instead of an indeterminate bar.
        "Content-Length": String(object.size),
        "Accept-Ranges": "bytes",
        "Cache-Control": "public, max-age=31536000, immutable",
        ...CORS,
      };

      if (request.method === "HEAD") {
        return new Response(null, { headers });
      }
      return new Response(object.body, { headers });
    }

    return json({ error: "Not found." }, 404);
}
