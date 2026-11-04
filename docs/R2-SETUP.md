# R2 setup — step by step

Everything below is done once. After it, publishing a build from the app works.

## Values you will need

| Where | Name | Value |
|---|---|---|
| R2 bucket | bucket name | `sproutly-apks` |
| Worker | script name | `sproutly-apk-upload` |
| Worker | R2 binding | `APKS` (already in `wrangler.toml`) |
| Worker secret | `FIREBASE_API_KEY` | your Firebase **Web API key** |
| `keystore.properties` | `apkUploadUrl` | the Worker URL |
| GitHub secret | `SPROUTLY_APK_UPLOAD_URL` | the Worker URL |

The Firebase Web API key is at **Firebase Console → Project settings → General →
Web API key**. It is the same value as `api_key[0].current_key` in
`google-services.json`. It is public by design — it identifies the project, not a
user — but it is not a password, so nothing is lost if it is seen.

## 1. Install wrangler and log in

```bash
npm install -g wrangler
wrangler login
```

`wrangler login` opens a browser and uses your Cloudflare account directly. It
does not depend on any API token.

## 2. Create the bucket

```bash
wrangler r2 bucket create sproutly-apks
```

The name must match `bucket_name` in `cloudflare/apk-upload/wrangler.toml`.

## 3. Store the Firebase key as a Worker secret

```bash
cd cloudflare/apk-upload
wrangler secret put FIREBASE_API_KEY
```

Paste the Web API key when prompted. It is stored encrypted on Cloudflare's side
and never enters the repository or the APK.

## 4. Deploy the Worker

```bash
wrangler deploy
```

It prints the URL:

```
https://sproutly-apk-upload.<your-subdomain>.workers.dev
```

Your subdomain is shown by `wrangler whoami`, or in the dashboard under
Workers & Pages.

## 5. Check that it is live

```bash
curl -i https://sproutly-apk-upload.<your-subdomain>.workers.dev/upload
```

A healthy Worker answers:

```
HTTP/1.1 401
{"error":"Sign in first."}
```

401 here is correct: it means the Worker is up and refusing anonymous uploads.

## 6. Put the URL in the two places that build the app

Local builds — `keystore.properties` in the project root:

```properties
apkUploadUrl=https://sproutly-apk-upload.<your-subdomain>.workers.dev
```

CI builds — GitHub → repository **Settings → Secrets and variables → Actions →
New repository secret**:

```
Name:  SPROUTLY_APK_UPLOAD_URL
Value: https://sproutly-apk-upload.<your-subdomain>.workers.dev
```

Both are read by `app/build.gradle.kts` into `BuildConfig.APK_UPLOAD_URL`.

## 7. Deploy the Firestore rules

The listing rules changed (the build link must be an `https` URL, the report
count is now exact):

```bash
firebase deploy --only firestore:rules,firestore:indexes
```

## 8. Test it for real

1. Open the app as a developer whose store is **ACTIVE**.
2. Publish → **The build** tile → *Upload APK* → pick the file.
3. The bar fills; the tile shows the file name and size.
4. **Publish app**, then open the listing → **Download the build**.

If step 3 fails with `Sign in first.`, the Worker rejected the token: check that
`FIREBASE_API_KEY` is the Web API key for the `tab-gl` project.

## Notes

- Downloads are served by the Worker (`/apks/...`), so the bucket stays private.
  No public bucket URL and no CORS configuration are needed.
- The limit is 100 MB per build, enforced in the Worker and in the app.
- **Paste link** stays available and is the fallback whenever uploads are off.
- Listing images continue to go to Cloudinary; only builds use R2.
