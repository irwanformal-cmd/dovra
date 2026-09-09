# Dovra — Data Safety mapping (from actual implementation)

Prepared from code only. The developer enters the final answers in
Play Console → App content → Data safety; this file is the evidence base.

## Bottom line

- **Data collected: none.** Dovra has no analytics, no ads SDKs, no crash
  SDKs, no network calls, no accounts. Nothing leaves the device except
  through explicit user-initiated Share / Open / Save-As actions handled by
  the OS.
- **Data shared with third parties: none.**
- **Documents (user files): accessed, never collected.** Under Google's
  definitions, "Files and docs" counts as *collected* only if transmitted off
  the device or accessed beyond what the user directly picks. Dovra reads
  only user-selected files via SAF, processes them in memory / private cache,
  and deletes working copies. Recommended declaration: **Files and docs —
  collected: NO** (processed on-device only, not transmitted). ⚠️ DEVELOPER
  DECISION: confirm this reading against the current Data safety form at
  submission time; if the form's file-access question demands disclosure of
  on-device access, declare it as "processed on device, not transmitted".
- **Account / identity / contacts / location / photos / audio / health /
  financial / messages / app activity beyond local history: none** — the app
  has no such features or permissions.
- **Local conversion history** (file names, formats, sizes, timestamps) is
  stored in the app-private Room DB, never transmitted, deletable per item /
  clear-all / toggle-off, removed on uninstall. This is on-device storage,
  not collection.

## Evidence pointers

- Zero `<uses-permission>` in `AndroidManifest.xml`; `usesCleartextTraffic=false`.
- SAF-only access (`OpenDocument`, `OpenDocumentTree`, `ACTION_CREATE_DOCUMENT`).
- `FileProvider` exposes only cache `converted/shared/temp` + `outputs/`.
- No WebView, no HTTP client, no analytics/tracking libraries in
  `app/build.gradle.kts` (audit each release).
- Temp cleanup: `TemporaryFileManager` + orchestrator timeout/cancel paths.
- History = metadata only (`HistoryEntities.kt`).

## Developer decisions required

1. Confirm the Files-and-docs answer against the live Data safety form.
2. The optional external donation links open in the user's browser on third-
   party sites — that traffic belongs to those sites, not to Dovra's
   declaration, but mention it if the form asks about external links.
