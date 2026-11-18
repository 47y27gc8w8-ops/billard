"""Renders the logo SVGs to the PNG app icons and the Windows .ico with headless Chrome or Edge,
and copies the logo to IntelliJ's project icon.

Usage: python logo/render_icons.py [path-to-chrome-or-edge]
"""
import pathlib
import shutil
import subprocess
import sys
import tempfile

from PIL import Image

LOGO = pathlib.Path(__file__).resolve().parent
ICONS = LOGO.parent / "src/main/resources/com/example/billard/icon"
BROWSERS = (
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    "google-chrome", "chromium", "microsoft-edge",
)
TINY_SIZE = 16
SMALL_SIZES = (TINY_SIZE, 24, 32)
LARGE_SIZES = (48, 64, 128, 256, 512)
BROWSER_TIMEOUT_SECONDS = 30
RENDER_ATTEMPTS = 3


def find_browser():
    if len(sys.argv) > 1:
        return sys.argv[1]
    for candidate in BROWSERS:
        if pathlib.Path(candidate).is_file() or shutil.which(candidate):
            return candidate
    sys.exit("No Chrome or Edge found; pass its path as the first argument.")


def render(browser, svg, size, out):
    """Headless browsers occasionally fail to exit, so a stuck render is retried."""
    for attempt in range(1, RENDER_ATTEMPTS + 1):
        try:
            render_once(browser, svg, size, out)
            return
        except subprocess.TimeoutExpired:
            if attempt == RENDER_ATTEMPTS:
                raise


def render_once(browser, svg, size, out):
    with tempfile.TemporaryDirectory() as work:
        page = pathlib.Path(work, "page.html")
        page.write_text(
            f'<html><body style="margin:0;background:transparent">'
            f'<img src="{svg.as_uri()}" width="{size}" height="{size}" style="display:block">'
            f"</body></html>")
        subprocess.run([browser, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                        f"--user-data-dir={work}/profile", "--force-device-scale-factor=1",
                        "--default-background-color=00000000", f"--window-size={size},{size}",
                        f"--screenshot={out}", page.as_uri()],
                       check=True, capture_output=True, timeout=BROWSER_TIMEOUT_SECONDS)
    Image.open(out).convert("RGBA").crop((0, 0, size, size)).save(out, optimize=True)


def svg_for(size):
    if size == TINY_SIZE:
        return "billard-tiny.svg"
    return "billard-small.svg" if size in SMALL_SIZES else "billard.svg"


def main():
    browser = find_browser()
    ICONS.mkdir(parents=True, exist_ok=True)
    pngs = []
    for size in SMALL_SIZES + LARGE_SIZES:
        svg = LOGO / svg_for(size)
        out = ICONS / f"billard-{size}.png"
        render(browser, svg, size, out)
        pngs.append(Image.open(out))
    ico = [image for image in pngs if image.width <= 256]
    ico[-1].save(LOGO / "billard.ico", append_images=ico[:-1], sizes=[(i.width, i.height) for i in ico])
    shutil.copyfile(LOGO / "billard.svg", LOGO.parent / ".idea/icon.svg")
    print("wrote", len(pngs), "PNGs to", ICONS, "and", LOGO / "billard.ico")


if __name__ == "__main__":
    main()
