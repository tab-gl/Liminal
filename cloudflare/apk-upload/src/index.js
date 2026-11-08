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
/** How many builds one account may keep. A quota is the cheapest abuse control. */
const MAX_OBJECTS_PER_USER = 40;

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

/**
 * Is this account allowed to publish?
 *
 * Reads the caller's profile from Firestore with the caller's own token, so the
 * answer comes from the database rather than from anything the client claims.
 */
async function sellerCheck(uid, env) {
  const project = env.FIREBASE_PROJECT_ID;
  const key = env.FIREBASE_API_KEY;
  if (!project || !key) {
    return { ok: false, reason: "The Worker is not configured with FIREBASE_PROJECT_ID." };
  }
  try {
    const response = await fetch(
      `https://firestore.googleapis.com/v1/projects/${project}/databases/(default)/documents/users/${uid}?key=${key}`,
    );
    if (!response.ok) return { ok: false, reason: "Could not read your profile." };
    const data = await response.json();
    const role = data?.fields?.role?.stringValue ?? "";
    const storeId = data?.fields?.storeId?.stringValue ?? "";
    if (!["DEVELOPER", "ADMIN"].includes(role) || !storeId) {
      return { ok: false, reason: "Only approved developers can upload builds." };
    }
    return { ok: true, storeId };
  } catch (error) {
    return { ok: false, reason: "Could not verify your developer profile." };
  }
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
          // No message or stack: this endpoint is public, and an internal error
          // string is a map of the deployment.
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
      let readable = null;
      let sampleCount = null;
      if (bound) {
        try {
          // Only a count leaves this endpoint: keys carry user identifiers and
          // this URL is public.
          const list = await env.APKS.list({ limit: 20 });
          readable = true;
          sampleCount = list.objects.length;
        } catch (error) {
          return json({
            ok: false,
            binding: "APKS",
            bucketError: "Bucket could not be listed.",
            hint: "The binding exists but the bucket cannot be read. Check that the bucket name in the binding matches the bucket you created.",
          }, 500);
        }
      }
      return json({
        ok: bound && Boolean(env.FIREBASE_API_KEY),
        binding: bound ? "APKS" : "MISSING",
        firebaseKey: env.FIREBASE_API_KEY ? "set" : "MISSING",
        bucketReadable: readable,
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

      // A publisher must be an approved studio: an authenticated account is not
      // the same thing as a seller, and this endpoint used to accept either.
      const seller = await sellerCheck(uid, env);
      if (!seller.ok) return json({ error: seller.reason }, 403);

      // Quota: count what this account already stores before taking more.
      const existing = await env.APKS.list({ prefix: `apks/${uid}/`, limit: 1000 });
      if (existing.objects.length >= MAX_OBJECTS_PER_USER) {
        return json({
          error: `You already have ${existing.objects.length} builds stored. ` +
            `The limit is ${MAX_OBJECTS_PER_USER}. Delete one before uploading another.`,
        }, 429);
      }

      const fileName = safeName(request.headers.get("X-File-Name"));
      const key = `apks/${uid}/${Date.now()}-${fileName}`;

      // Streamed, not buffered: a 100 MB body used to become a 100 MB array in
      // the Worker before one byte reached R2. The counter fails the upload the
      // moment it crosses the limit, so a lying Content-Length cannot help.
      let written = 0;
      const counter = new TransformStream({
        transform(chunk, controller) {
          written += chunk.byteLength;
          if (written > MAX_BYTES) {
            controller.error(new Error("Build is over 100 MB."));
            return;
          }
          controller.enqueue(chunk);
        },
      });

      try {
        await env.APKS.put(key, request.body.pipeThrough(counter), {
          httpMetadata: {
            contentType: "application/vnd.android.package-archive",
            contentDisposition: `attachment; filename="${fileName}"`,
          },
        });
      } catch (error) {
        return json({
          error: written > MAX_BYTES
            ? "That build is over 100 MB."
            : "Upload failed before it reached storage.",
        }, 413);
      }

      if (written === 0) return json({ error: "That file is empty." }, 400);

      // The key already begins with "apks/". Prefixing it again produced
      // /apks/apks/<uid>/… and the download then looked up a key that did not
      // exist, so every build link 404'd.
      return json({ url: `${url.origin}/${key}`, path: key, size: written });
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
