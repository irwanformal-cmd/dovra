# Privacy Policy Publication — Release Blocker

## Status: ACTION REQUIRED (external, developer-side)

The complete policy text exists at `docs/privacy-policy.md` and the matching
web page exists at `website/privacy/index.html`. **Neither is publicly hosted
yet**, and no public URL has been claimed.

## What the developer must do

1. Deploy the `website/` folder to a public static host (instructions:
   `website/DEPLOYMENT.md`, e.g. GitHub Pages — free).
2. Verify the policy is reachable at the final public URL, e.g.
   `https://<your-pages-site>/privacy/`.
3. Fill the two placeholders in `docs/privacy-policy.md` (effective date,
   contact channel), regenerate `website/privacy/index.html` accordingly.
4. Update the Android app privacy URL (currently the unconfirmed
   `https://documint.app/privacy` in
   `app/src/main/java/com/docuconvert/app/presentation/AppNavHost.kt`)
   to the real public URL.
5. Enter the **same** public URL in Google Play Console → App content →
   Privacy policy.

## Why this blocks release

Google Play requires a valid, publicly accessible privacy-policy URL for apps
that access user files — even fully offline ones. Submitting with the old
unverified URL (which currently 404s on an unrelated third party's domain)
risks rejection. Do not submit until steps 1–5 are done.
