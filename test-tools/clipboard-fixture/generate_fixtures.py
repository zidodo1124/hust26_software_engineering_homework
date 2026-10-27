"""Create synthetic media for the phone attachment test; no external inputs."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import json

from PIL import Image, ImageDraw, ImageOps

BASE = Path(__file__).resolve().parent
ROOT = BASE / "assets"
ROOT.mkdir(parents=True, exist_ok=True)

landscape = Image.new("RGB", (640, 360), "#176b87")
draw = ImageDraw.Draw(landscape)
draw.rectangle((0, 0, 639, 69), fill="#a8e6ed")
draw.rectangle((32, 104, 210, 325), fill="#ffffff")
draw.ellipse((300, 105, 555, 325), fill="#f5c95b")
draw.text((28, 26), "QA1027 - BLUE LANDSCAPE - TOP", fill="#122333", stroke_width=1)
draw.text((40, 212), "LEFT", fill="#122333", stroke_width=1)
landscape.save(ROOT / "QA1027-blue-landscape.png", format="PNG")

portrait = Image.new("RGB", (320, 640), "#e66b3d")
draw = ImageDraw.Draw(portrait)
draw.rectangle((0, 0, 319, 79), fill="#fff1d4")
draw.polygon(((160, 120), (280, 360), (40, 360)), fill="#186b39")
draw.rectangle((38, 440, 280, 585), fill="#ffffff")
draw.text((14, 28), "QA1027 - ORANGE PORTRAIT - TOP", fill="#432518")
draw.text((92, 505), "BOTTOM", fill="#432518", stroke_width=1)
portrait.save(ROOT / "QA1027-orange-portrait.png", format="PNG")

rotated = Image.new("RGB", (480, 240), "#fff6a5")
draw = ImageDraw.Draw(rotated)
draw.rectangle((0, 0, 89, 239), fill="#ad304f")
draw.polygon(((130, 80), (340, 80), (340, 40), (440, 120), (340, 200), (340, 160), (130, 160)), fill="#123d6a")
draw.text((125, 15), "QA1027 - EXIF ORIENTATION 6", fill="#16252e", stroke_width=1)
exif = Image.Exif()
exif[274] = 6
rotated.save(ROOT / "QA1027-exif-rotate90.jpg", quality=95, exif=exif)

(ROOT / "QA1027-broken.png").write_bytes(b"QA1027 synthetic invalid image: not PNG data.\n")

entries = []
for path in sorted(ROOT.iterdir()):
    if path.suffix not in {".png", ".jpg"}:
        continue
    entry = {
        "file": path.name,
        "byteSize": path.stat().st_size,
        "sha256": hashlib.sha256(path.read_bytes()).hexdigest().upper(),
        "synthetic": True,
        "expectedValidImage": path.name != "QA1027-broken.png",
    }
    if entry["expectedValidImage"]:
        with Image.open(path) as image:
            image.load()
            entry.update({
                "mimeType": Image.MIME[image.format],
                "encodedWidth": image.width,
                "encodedHeight": image.height,
                "exifOrientation": image.getexif().get(274, 1),
                "expectedOrientedDimensions": list(ImageOps.exif_transpose(image).size),
            })
        with Image.open(path) as image:
            image.verify()
    else:
        entry.update({"mimeTypeHint": "image/png", "expectedImport": "reject_and_preserve_existing_draft"})
        try:
            with Image.open(path) as image:
                image.verify()
        except Exception:
            entry["decoderRejectionVerified"] = True
        else:
            raise AssertionError("The intentionally broken fixture unexpectedly decoded.")
    entries.append(entry)

manifest = {
    "createdUtc": datetime.now(timezone.utc).isoformat(),
    "purpose": "Synthetic image fixtures for HONOR X50 attachment UI tests",
    "deviceOperationsPerformed": False,
    "files": entries,
}
(BASE / "assets-manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print(json.dumps(manifest, indent=2, ensure_ascii=False))
