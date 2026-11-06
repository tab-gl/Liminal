# Redeploying the Worker without wrangler

The version running on Cloudflare is the **old** one: it answers download URLs of
the form `/apks/apks/…`, which never resolves. The corrected script is
`src/index.js`. It has to be pasted in, because wrangler will not run on arm32 and
the Cloudflare API refuses calls from here.

## 1. Paste the new script

Dashboard → **Workers & Pages** → `sproutly-apk-upload` → **Edit code**.

Delete everything in the editor, paste all of `src/index.js`, then **Deploy**.

## 2. Check the binding is still there

Settings → **Bindings**. There must be an **R2 bucket** binding:

```
Variable name:  APKS        ← exactly this, capitalised
R2 bucket:      sproutly-apks
```

Editing code in the dashboard keeps bindings, but re-creating the Worker does not.

## 3. Check the secret

Settings → **Variables and Secrets**. There must be a **Secret** named
`FIREBASE_API_KEY` holding the project's **Web API key**.

## 4. Ask the Worker what it thinks

Open this in a browser:

```
https://sproutly-apk-upload.aceyash-dev.workers.dev/health
```

A working deployment answers:

```json
{
  "ok": true,
  "binding": "APKS",
  "firebaseKey": "set",
  "sampleKeys": ["apks/…/1793…-the-visitor.apk"],
  "sampleCount": 1
}
```

What each answer means:

- `"binding": "MISSING"` — the R2 binding is not on this version. Step 2.
- `"firebaseKey": "MISSING"` — uploads will answer 401. Step 3.
- `ok: false` with `bucketError` — the binding points at a bucket that does not
  exist, or the name is misspelled.
- `sampleKeys: []` — the bucket is empty: nothing was ever stored, so re-upload
  the build from the app.

## 5. Download one key directly

Copy a `sampleKeys` value and open:

```
https://sproutly-apk-upload.aceyash-dev.workers.dev/<key>
```

That must download the APK. If it 404s with a `key` in the body, the object is
stored under a different key than the one requested — send me both strings.

## Why the app said "could not be downloaded"

The old Worker built `${origin}/apks/${key}` while the key already began with
`apks/`, so the download looked up `apks/apks/…` and found nothing. The new script
uses `${origin}/${key}` and also collapses a doubled `apks/` prefix, so links
already saved in published listings resolve without re-uploading.
