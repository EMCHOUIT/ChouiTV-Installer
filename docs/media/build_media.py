#!/usr/bin/env python3
"""Regenerates the drawn images of the READMEs (never the real phone screenshots).

    python docs/media/build_media.py            # everything
    python docs/media/build_media.py whatsnew   # one kind: whatsnew, migration, hero, tabs, qr

What it makes
  whatsnew-{ar,fr,en}.png   the "what's new" card of the current version   (docs/media/src/whatsnew.html)
  migration-{ar,fr,en}.png  the 3.x -> 4.0 diagram                          (docs/media/src/migration.html)
  qr-download.png           the QR code of the STABLE link (releases/latest/download/ChouiTV-Installer.apk): it never needs redoing
                            for a new version. Needs the 'qrcode' package (pip install qrcode).
  hero-{ar,fr,en}.png       the top image of the READMEs (docs/media/src/hero.html); the phone inside is a real screenshot
                            (docs/media/src/shots/<lang>-remote.png). The version number is in the pill of hero.html.
  tabs-{ar,fr,en}.png       the three tabs side by side (docs/media/src/tabs.html), real screenshots in docs/media/src/shots/

Needs: Python 3 with Pillow (pip install pillow), and Microsoft Edge or Google Chrome (set CHROME to its path if it is not found).
Fonts are the ones in app/src/main/assets/fonts (licences: THIRD_PARTY_NOTICES.md). The HTML in docs/media/src is the source of truth:
edit the texts there (one block per language), then run this again.
"""
import os
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
FONTS = os.path.join(HERE, "..", "..", "app", "src", "main", "assets", "fonts")
LANGS = ("ar", "fr", "en")
SIZES = {"whatsnew": (1800, 980), "migration": (1800, 780), "hero": (1800, 980), "tabs": (1800, 1300)}


def browser():
    for c in (os.environ.get("CHROME"),
              r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
              r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
              r"C:\Program Files\Google\Chrome\Application\chrome.exe",
              shutil.which("google-chrome"), shutil.which("chromium"), shutil.which("chromium-browser"), shutil.which("microsoft-edge")):
        if c and os.path.exists(c):
            return c
    sys.exit("No Edge/Chrome found: set the CHROME environment variable.")


def shot(kind, lang):
    w, h = SIZES[kind]
    page = os.path.join(HERE, "src", kind + ".html")
    out = os.path.join(HERE, "%s-%s.png" % (kind, lang))
    url = "file:///" + os.path.abspath(page).replace("\\", "/").lstrip("/") + "?lang=" + lang
    if os.path.abspath(page).startswith("\\\\"):   # a UNC path (WSL): file://host/share/...
        url = "file:" + os.path.abspath(page).replace("\\", "/") + "?lang=" + lang
    tmp = tempfile.mkdtemp()
    tmpout = os.path.join(tmp, "o.png")
    try:
        subprocess.run([browser(), "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=1",
                        "--allow-file-access-from-files", "--user-data-dir=" + os.path.join(tmp, "p"),
                        "--virtual-time-budget=4000", "--window-size=%d,%d" % (w, h), "--screenshot=" + tmpout, url],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)
        shutil.copyfile(tmpout, out)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    print("wrote", out)


def qr():
    import qrcode
    from PIL import Image, ImageDraw
    url = "https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk"
    q = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_H, box_size=20, border=3)
    q.add_data(url)
    q.make(fit=True)
    img = q.make_image(fill_color=(10, 29, 20), back_color=(246, 239, 224)).convert("RGBA")
    W = 826
    img = img.resize((W, W), Image.NEAREST)
    d = ImageDraw.Draw(img)
    c = W // 2
    d.ellipse((c - 98, c - 98, c + 98, c + 98), fill=(255, 253, 238))      # the quiet ring around the logo
    logo = Image.open(os.path.join(HERE, "logo.png")).convert("RGBA").resize((170, 170), Image.LANCZOS)
    img.alpha_composite(logo, (c - 85, c - 85))
    out = Image.new("RGBA", (W, W), (0, 0, 0, 0))                          # gold rounded frame, as before
    m = Image.new("L", (W, W), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, W - 1, W - 1), radius=34, fill=255)
    out.paste(img, (0, 0), m)
    ImageDraw.Draw(out).rounded_rectangle((1, 1, W - 2, W - 2), radius=34, outline=(199, 148, 69, 255), width=5)
    out.save(os.path.join(HERE, "qr-download.png"), optimize=True)
    print("wrote qr-download.png ->", url)


if __name__ == "__main__":
    what = sys.argv[1:] or ["whatsnew", "migration", "hero", "tabs", "qr"]
    for k in what:
        if k == "qr":
            qr()
        else:
            for lang in LANGS:
                shot(k, lang)
