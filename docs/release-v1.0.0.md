# Dovra v1.0.0 — release notes (draft, local only — not published)

**Version:** 1.0.0 (`versionCode 1`) · **App ID:** `com.docuconvert.app`

Dovra – Document Reader & Converter. Read and convert your documents privately.

## Highlights

- Offline-first document reader + converter: 18 viewable formats (TXT, MD,
  HTML, XML, RTF, DOC, DOCX, ODT, XLS, XLSX, ODS, CSV, TSV, PPT, PPTX, ODP,
  PDF, EPUB) with 62 honestly-labelled conversion pairs (FULL / SIMPLIFIED).
- Privacy-first: on-device processing, no uploads, no account, no ads, no
  analytics, zero permissions, SAF-only file access.
- Material 3 UI with System/Light/Dark themes, Save As, share/open via
  FileProvider, metadata-only on-device history, optional Support section
  (external donation links).

## Honest limitations

- PDF/DOCX outputs are simplified renderings; presentations convert as
  text/outline extraction; legacy DOC/XLS/PPT are read-only sources
  (XLS also a simplified target); DjVu unsupported; large files can hit the
  2-minute timeout or memory limits on low-end devices.

## Upgrade / compatibility

- First public release; no migration notes.
- Internal storage identifiers (`docuconvert.db`, `Documents/Dovra` output
  folder default) are new-install defaults.
