# Deploying the Dovra website to GitHub Pages

No deployment has been performed. These are the exact steps for the developer.

## Option A — project site (recommended, keeps app + site in one repo)

1. Commit the `website/` folder to the GitHub repository.
2. On GitHub, open the repository → **Settings → Pages**.
3. Under **Build and deployment → Source**, choose **Deploy from a branch**.
4. Branch: your default branch (e.g. `main`), folder: `/website` — if your
   plan only offers `/(root)`, either move the site files to a dedicated
   `site` branch's root or use Option B below.
5. Save. GitHub will show the public URL, shaped like
   `https://<username>.github.io/<repo>/`.
6. Verify in a private window:
   - `<url>/` shows the Dovra homepage,
   - `<url>/privacy/` shows the privacy policy,
   - `<url>/support/` shows the support page.

## Option B — user/org site (cleanest URLs, separate repo)

1. Create a **new public repository** named exactly `<username>.github.io`.
2. Copy the *contents* of `website/` (index.html, privacy/, support/,
   styles/, assets/) into that repository's root and push.
3. The site is live at `https://<username>.github.io/` within minutes.
4. Verify `/`, `/privacy/`, `/support/` as above.

## Option C — automated (optional workflow)

A ready-made workflow is included at `.github/workflows/pages.yml`: it
publishes `website/` on every push to `main`. To use it, keep the site in
this repo, then in **Settings → Pages** select **GitHub Actions** as source
instead of a branch. Verify the same three URLs after the first run.

## After the URL exists

1. Set the effective date + contact in `docs/privacy-policy.md` and mirror
   them in `website/privacy/index.html`.
2. Update the Android privacy URL in
   `app/src/main/java/com/docuconvert/app/presentation/AppNavHost.kt`
   to the final public privacy URL.
3. Paste the same URL into Play Console → App content → Privacy policy.

Do NOT invent or guess the final URL anywhere in the repo before it exists.
