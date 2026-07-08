# Ersetzt das App-Icon mit dem angegebenen Bild.
# Voraussetzung: pip install Pillow
# Ausfuehren:  python set_icon.py <Pfad zum Logo>

import sys
import os
import math
from pathlib import Path
from PIL import Image, ImageDraw

if len(sys.argv) < 2:
    print("Verwendung: python set_icon.py <Pfad zum Logo>")
    sys.exit(1)

logo_path = sys.argv[1]
if not os.path.exists(logo_path):
    print(f"Datei nicht gefunden: {logo_path}")
    sys.exit(1)

res_dir = Path(__file__).parent / "app" / "src" / "main" / "res"

TEAL = (0, 200, 224, 255)  # #00C8E0

def make_legacy_icon(img, size):
    """Logo zentriert auf tealem Kreis — übersteht jeden kreisförmigen Launcher-Mask."""
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    # Teal Kreis als Hintergrund
    circle = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(circle)
    draw.ellipse([0, 0, size - 1, size - 1], fill=TEAL)
    canvas.paste(circle, (0, 0), circle)

    # Logo bei 65% des Kreisdurchmessers — Ecken des Logo-Quadrats liegen knapp
    # innerhalb des Kreises (65% * sqrt(2)/2 ≈ 0.46 < 0.5 Radius)
    logo_size = int(size * 0.65)
    offset = (size - logo_size) // 2
    logo_scaled = img.resize((logo_size, logo_size), Image.LANCZOS)
    canvas.paste(logo_scaled, (offset, offset), logo_scaled)
    return canvas

# Größen für Legacy-Icons
DENSITIES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

img = Image.open(logo_path).convert("RGBA")

print("Erstelle Legacy-Icons (Logo auf Teal-Kreis)...")
for folder, size in DENSITIES.items():
    target_dir = res_dir / folder
    target_dir.mkdir(exist_ok=True)
    for old in target_dir.glob("ic_launcher*.webp"):
        old.unlink()
    icon = make_legacy_icon(img, size)
    icon.save(target_dir / "ic_launcher.png", "PNG")
    icon.save(target_dir / "ic_launcher_round.png", "PNG")
    print(f"  OK {folder}: {size}x{size}px")

# Adaptives Icon: Vordergrundebene als PNG (432×432 für xxxhdpi)
# Safe Zone = Kreis mit Ø 264px (132px Radius ab Mitte).
# Logo-Quadrat-Ecken müssen innerhalb des Kreises liegen:
#   Eckenabstand = logo_size/2 * sqrt(2) ≤ 132  →  logo_size ≤ 186px  →  43% von 432
# Mit Sicherheitspuffer: 38% = 164px (Eckenabstand = 116px < 132px OK)
print("\nErstelle adaptives Icon-Foreground (432×432 mit Safe-Zone-Padding)...")
drawable_dir = res_dir / "drawable-xxxhdpi"
drawable_dir.mkdir(exist_ok=True)
FULL = 432
LOGO_SIZE = int(FULL * 0.38)   # 164px — Ecken bei 116px Abstand, Safe-Zone-Radius 132px
OFFSET = (FULL - LOGO_SIZE) // 2

canvas = Image.new("RGBA", (FULL, FULL), (0, 0, 0, 0))
logo_scaled = img.resize((LOGO_SIZE, LOGO_SIZE), Image.LANCZOS)
canvas.paste(logo_scaled, (OFFSET, OFFSET), logo_scaled)
canvas.save(drawable_dir / "ic_launcher_fg.png", "PNG")
print(f"  OK drawable-xxxhdpi/ic_launcher_fg.png  (Logo {LOGO_SIZE}px, Eckenabstand {int(LOGO_SIZE/2*math.sqrt(2))}px < 132px Safe-Zone-Radius)")

# Foreground-XML (BitmapDrawable)
foreground_xml = res_dir / "drawable" / "ic_launcher_foreground.xml"
foreground_xml.write_text(
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<bitmap xmlns:android="http://schemas.android.com/apk/res/android"\n'
    '    android:src="@drawable/ic_launcher_fg"\n'
    '    android:gravity="center"\n'
    '    android:tileMode="disabled"/>\n',
    encoding="utf-8"
)
print("  OK drawable/ic_launcher_foreground.xml aktualisiert")

# Background-XML (Teal)
background_xml = res_dir / "drawable" / "ic_launcher_background.xml"
background_xml.write_text(
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
    '    android:width="108dp"\n'
    '    android:height="108dp"\n'
    '    android:viewportWidth="108"\n'
    '    android:viewportHeight="108">\n'
    '    <path android:fillColor="#00C8E0"\n'
    '          android:pathData="M0,0L108,0L108,108L0,108Z"/>\n'
    '</vector>\n',
    encoding="utf-8"
)
print("  OK drawable/ic_launcher_background.xml aktualisiert")

# app_logo.png für die Sidebar
app_logo_dir = res_dir / "drawable"
app_logo_dir.mkdir(exist_ok=True)
app_logo = img.resize((144, 144), Image.LANCZOS)
app_logo.save(app_logo_dir / "app_logo.png", "PNG")
print("  OK drawable/app_logo.png aktualisiert")

print("\nFertig! App neu bauen um das Icon zu sehen.")
