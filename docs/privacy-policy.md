# Dovra Privacy Policy

**Effective date:** September 10, 2026

**App:** Dovra – Document Reader & Converter (`com.docuconvert.app`)

Dovra is a private, offline-first document reader and converter for Android.
This policy describes what the app does with your information, based on its
actual implementation.

## 1. Short version

- Your documents are processed **entirely on your device**. They are never
  uploaded to a server, never sent to a cloud API, and never shared by the app
  except when **you explicitly choose** to share or save a file.
- Dovra contains **no advertising SDKs, no analytics SDKs, and no tracking**.
- Dovra requests **no `INTERNET` permission** for document processing.
- No account is required. Dovra does not collect account information because
  there is nothing to sign in to.
- The app stores only local settings and conversion **metadata** (file names,
  formats, sizes, timestamps) — never document contents.

## 2. Document processing (local only)

When you open or convert a document:

1. You pick the file through Android's system file picker (Storage Access
   Framework) or share it from another app. Dovra reads only the file you
   selected.
2. Reading, format detection (extension, MIME type, magic-byte signature
   sniffing), and conversion run locally with on-device libraries
   (Apache POI, ODFDOM, PDFBox-Android, rtfparserkit, epublib, CommonMark,
   Jsoup, Commons CSV).
3. The app performs **no network requests** during document processing.
   External resources referenced inside a document (e.g. remote images in
   HTML) are **not fetched**.
4. Temporary conversion files are written to the app's private cache
   directory (`cacheDir/temp/`) and are deleted after conversion succeeds,
   fails, is cancelled, or times out (2-minute limit). Orphaned temporary
   files older than 24 hours are removed automatically on app start.

**Document contents never leave your device** unless you tap Share / Open /
Save As, in which case Android hands the file you chose to the destination
you chose (another app, or a folder you picked). That transfer is performed
by the operating system at your explicit request.

## 3. Data stored on your device

| Data | Where | Contents | Backup |
|---|---|---|---|
| App settings (appearance, output folder, history toggle, auto-open) | DataStore preferences | Your choices only | Covered by Android backup rules |
| Conversion history (optional, can be disabled/cleared) | Room database (`docuconvert.db`) | Source file name, source/target formats, result name, file size, timestamp, success flag — **metadata only, never file bytes** | Covered by Android backup rules |
| Temporary conversion files | App-private cache | Working copies during conversion | Never backed up; auto-deleted |
| Persisted URI permissions | Android system | Read/write grants for files/folders you picked | Managed by Android |

SharedPreferences backup is excluded by the app's data-extraction rules, and
document files themselves are never backed up by the app.

## 4. Permissions

Dovra declares **zero runtime permissions** in its manifest:

- No `INTERNET` permission.
- No `READ/WRITE_EXTERNAL_STORAGE`, no `MANAGE_EXTERNAL_STORAGE`.
- File access works through the Storage Access Framework: the system picker
  grants Dovra access to exactly the files and folders you select, and those
  grants can be revoked by you at any time in Android settings.
- Sharing uses Android's `FileProvider`, which exposes only the app's
  `converted/`, `shared/`, `temp/` cache paths and `outputs/` files path —
  never databases, preferences, or other internal directories.

## 5. What Dovra does NOT do

- No advertising of any kind; no advertising SDKs.
- No analytics, telemetry, crash-reporting SDKs, or remote configuration.
- No accounts, no sign-in, no contact or identity collection.
- No location access or collection.
- No sale or sharing of personal data with third parties — there is no data
  pipeline to sell or share from.

## 6. Third-party libraries

Dovra is built with open-source libraries (listed in full, with versions and
licenses, in the app under Settings → About → Open Source Licenses). These
libraries run locally as document parsers and UI frameworks; none of them is
configured to transmit your documents or personal data anywhere. No
third-party service receives data from the app.

## 7. Support / donations (external, optional)

Settings → About → Support Dovra offers two optional donation links
(Buy Me a Coffee, Saweria). Tapping one opens the link in **your own
external browser** via an Android `ACTION_VIEW` intent. Donating is entirely
optional and takes place on those third-party websites under their own
privacy policies. Dovra itself does not process payments and receives no
personal data from the donation flow inside the app.

## 8. Data retention and deletion

- Temporary files: deleted as described in §2.
- Conversion history: retained on your device until you delete individual
  entries, tap Clear, disable history in Settings, clear the app's data, or
  uninstall the app (which removes all app-private data).
- Settings: retained until you clear app data or uninstall.
- Because Dovra has no servers and no accounts, there is nothing to request
  from the developer beyond what you can delete on the device itself.

## 9. Children

Dovra processes documents locally and collects no personal data from anyone,
including children. There is no age-gated content and no communication
feature.

## 10. Changes to this policy

If Dovra's behavior changes, this policy will be updated and the effective
date above revised. Material changes will be reflected in the app listing
before or with the corresponding app update.

## 11. Contact

For privacy questions about Dovra, contact the developer at
bookirwan@gmail.com. You may also use the support channel listed on the
app's store listing / support page.
