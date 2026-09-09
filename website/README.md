# Dovra website (static, no backend)

Free static site for **Dovra – Document Reader & Converter**. Plain HTML +
one CSS file, no frameworks, no JavaScript, no analytics, no external fonts.

```
website/
  index.html        # homepage (EN)
  privacy/index.html
  support/index.html
  styles/main.css
  assets/           # icon 512, favicon, feature graphic, OG image
  README.md         # this file
  DEPLOYMENT.md     # GitHub Pages instructions
```

Preview locally: open `index.html` in a browser, or serve the folder:

```bash
cd website && python3 -m http.server 8080
# → http://localhost:8080/  (privacy/ and support/ resolve as directories)
```
