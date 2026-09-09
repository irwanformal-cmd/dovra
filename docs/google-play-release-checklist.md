# Dovra — Google Play release checklist

## 1. COMPLETED BY HARNESS (in-repo, verified)

- [x] Rebrand user-facing strings to Dovra (+ tagline intact).
- [x] Launcher/adaptive icons regenerated (no old-brand text) + 512px Play
      icon + 1024×500 feature graphic + OG image in `website/assets/`.
- [x] Store copy finalized: `docs/google-play/` (app name, EN+ID short
      ≤115 chars, EN+ID full descriptions, matrix-accurate, no over-claims).
- [x] Privacy policy written from implementation: `docs/privacy-policy.md` +
      `website/privacy/index.html`.
- [x] Static website ready: `website/` (home/privacy/support, responsive,
      semantic, no JS/analytics/fonts) + `DEPLOYMENT.md` + Pages workflow.
- [x] Data Safety evidence doc + assets checklist + domain notes.
- [x] Security fix: FileProvider share/open intents now set `ClipData` +
      `runCatching` on all chooser launches (robust grant + no crash when no
      handler).
- [x] OSS license screen verified complete vs `app/build.gradle.kts`
      (incl. LGPL epublib note, test-only deps marked).
- [x] Release config audited: minSdk 26, targetSdk/compileSdk 36,
      minify+shrink, non-debuggable release, zero permissions,
      `usesCleartextTraffic=false`, no secrets in repo, `.gitignore` added
      (keystores/build outputs excluded).
- [x] `clean` + `test` + `compileDebugKotlin` + `assembleDebug` +
      `bundleRelease` all SUCCESS; AAB 20 MB + APK 37 MB verified
      (label Dovra, ID `com.docuconvert.app`, targetSdk 36, no INTERNET).

## 2. DEVELOPER ACTION REQUIRED (exact instructions)

1. **Device smoke test** — install `app-debug.apk` on a real device; open
   one file per family (PDF, DOCX, XLSX, PPTX, EPUB), convert each, Save As,
   share once, toggle themes, clear history, tap Support links.
2. **Deploy website** — follow `website/DEPLOYMENT.md` (GitHub Pages,
   Options A/B/C); verify `/`, `/privacy/`, `/support/` load.
3. **Publish privacy policy** — set effective date + contact in
   `docs/privacy-policy.md`, mirror into `website/privacy/index.html`,
   redeploy, then update the app privacy URL in `AppNavHost.kt` and
   rebuild/retest (see `docs/privacy-policy-release-blocker.md`).
4. **Play Console setup** — create app (`com.docuconvert.app`), paste store
   copy from `docs/google-play/`, upload icon/screenshots per
   `docs/google-play-assets.md`, set Data Safety per
   `docs/google-play-data-safety.md`, complete Content Rating + Target
   Audience + App Access (no login needed: state “no account required”).
5. **Signing** — enroll in Play App Signing; keep the upload keystore offline
   and out of git (already gitignored).
6. **Upload AAB** from `app/build/outputs/bundle/release/`, roll out to
   closed testing first if required, then production.

## 3. EXTERNAL BLOCKERS (cannot be done in-repo)

- Google Play Developer account + identity verification.
- Play App Signing / upload-key management.
- Public website + privacy-policy hosting (+ optional custom domain).
- Physical-device testing and real screenshots.
- Play review itself (content rating, target audience, production access).
