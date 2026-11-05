# Worker audit

## Fixed

**The download URL did not round-trip.** Upload stored the object at
`apks/<uid>/<ts>-<name>`, then answered with `${origin}/apks/${key}` — the key
already begins with `apks/`, so the URL read `/apks/apks/<uid>/…`. The download
branch strips only the leading `/` and looked up `apks/apks/<uid>/…`, which does
not exist, so every build link returned `404 {"error":"Not found."}` and the app
reported a broken link.

- The URL is now `${origin}/${key}`.
- Downloads collapse a duplicated `apks/` prefix, so links already stored in
  published listings still resolve without re-uploading.
- Responses now carry `Content-Length`, so the app can show a real percentage.
- `HEAD` is answered, for clients that check before downloading.

## Worth knowing

- The R2 binding **must** be named `APKS`. Any other name and `env.APKS` is
  undefined: uploads throw, downloads 500. This is the first thing to check if a
  deploy "does nothing".
- `FIREBASE_API_KEY` must be a Worker **secret**, and must be the project's Web
  API key. A key restricted to the Android app returns `API_KEY_INVALID`, and
  every upload answers `401 Sign in first.`
- Uploads are authenticated but not authorised per store: any signed-in user can
  write under their own uid. That is fine for attribution, but it does not stop
  someone using the Worker as free file hosting. If that matters, check the caller
  owns a store before accepting the write.
- The filename is truncated to its last 60 characters, which can drop the `.apk`
  extension on a long name. Android sniffs the file, and the app names its own
  copy, so this is cosmetic.
- `workers.dev` is blocked or throttled on some Indian networks. If uploads work
  on Wi-Fi and downloads fail on mobile data, that is the network, not the code —
  put the Worker on a custom domain to avoid it.

## What I could not check

The Cloudflare integration rejects every API call (`Invalid format for X-Auth-Key
header`), so I could not read the deployed script, its bindings, its secrets, or
whether the `workers.dev` route is enabled. Everything above is from the source
and from the URLs the app produced.
