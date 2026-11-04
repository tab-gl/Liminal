# Build uploader

Two things must exist before the app can publish a build:

```bash
# 1. the bucket
wrangler r2 bucket create sproutly-apks

# 2. the worker (from this folder)
wrangler deploy
```

Then put the Worker URL in `keystore.properties` (or `local.properties`) as
`SPROUTLY_APK_UPLOAD_URL=https://sproutly-apk-upload.<subdomain>.workers.dev`
and rebuild. Without it, the *Paste link* path still works and the app says so.

Why a Worker instead of uploading straight to R2: writing to a bucket needs
credentials, and credentials inside an APK can be extracted by anyone. The Worker
verifies the caller's Firebase ID token with Google and writes with its own
binding, so the app never holds a secret.

`FIREBASE_API_KEY` in `wrangler.toml` is the project's web API key — public by
design, it identifies the project rather than a user.
