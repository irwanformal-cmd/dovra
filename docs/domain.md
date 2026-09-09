# Dovra — domain notes (no action taken in repo)

- A custom domain is **optional**. GitHub Pages hosting itself is free and
  the site works without one.
- A custom domain is more professional for the final product and for the
  Play-listed privacy-policy URL.
- The final privacy-policy URL **must be controlled by the developer**.
- Public site URL (live, verified 2026-09-09): https://irwanformal-cmd.github.io/dovra/ — privacy page at /privacy/. A custom domain remains optional.

After the public site URL exists: update the Android privacy URL in
  `app/src/main/java/com/docuconvert/app/presentation/AppNavHost.kt`
  (currently the unconfirmed `https://documint.app/privacy`), rebuild +
  retest, and point the Play Console privacy-policy field at the same URL.
- No imaginary domain has been hardcoded anywhere in this repo.
