# Ersetzt das App-Icon mit dem angegebenen Bild.
# Voraussetzung: pip install Pillow
# Ausfuehren:  python set_icon.py <Pfad zum Logo>

import sys
import os
from pathlib import Path
from PIL import Image

if len(sys.argv) < 2:
    print("Verwendung: python set_icon.py <Pfad zum Logo>")
    sys.exit(1)

logo_path = sys.argv[1]
if not os.path.exists(logo_path):
    print(f"Datei nicht gefunden: {logo_path}")
    sys.exit(1)

res_dir = Path(__file__).parent / "app" / "src" / "main" / "res"

# Größen für Legacy-Icons
DENSITIES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

img = Image.open(logo_path).convert("RGBA")

print("Erstelle Legacy-Icons...")
for folder, size in DENSITIES.items():
    target_dir = res_dir / folder
    target_dir.mkdir(exist_ok=True)
    # Alte .webp Dateien löschen
    for old in target_dir.glob("ic_launcher*.webp"):
        old.unlink()
    resized = img.resize((size, size), Image.LANCZOS)
    resized.save(target_dir / "ic_launcher.png", "PNG")
    resized.save(target_dir / "ic_launcher_round.png", "PNG")
    print(f"  ✓ {folder}: {size}×{size}px")

# Adaptives Icon: Vordergrundebene als PNG (432×432 für xxxhdpi)
# Safe Zone = 66% → Logo auf 66% skalieren und mittig platzieren
print("\nErstelle adaptives Icon-Foreground (432×432 mit Safe-Zone-Padding)...")
drawable_dir = res_dir / "drawable-xxxhdpi"
drawable_dir.mkdir(exist_ok=True)
FULL = 432
LOGO_SIZE = int(FULL * 0.70)   # 302px — größer, noch innerhalb der Safe Zone
OFFSET = (FULL - LOGO_SIZE) // 2

canvas = Image.new("RGBA", (FULL, FULL), (0, 0, 0, 0))
logo_scaled = img.resize((LOGO_SIZE, LOGO_SIZE), Image.LANCZOS)
canvas.paste(logo_scaled, (OFFSET, OFFSET), logo_scaled)
canvas.save(drawable_dir / "ic_launcher_fg.png", "PNG")
print(f"  ✓ drawable-xxxhdpi/ic_launcher_fg.png  (Logo {LOGO_SIZE}px zentriert)")

# Schreibe Foreground-XML (BitmapDrawable)
foreground_xml = res_dir / "drawable" / "ic_launcher_foreground.xml"
foreground_xml.write_text(
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<bitmap xmlns:android="http://schemas.android.com/apk/res/android"\n'
    '    android:src="@drawable/ic_launcher_fg"\n'
    '    android:gravity="center"\n'
    '    android:tileMode="disabled"/>\n',
    encoding="utf-8"
)
print("  ✓ drawable/ic_launcher_foreground.xml aktualisiert")

# Schreibe Background-XML (weiß, da das Bild selbst den Verlauf hat)
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
print("  ✓ drawable/ic_launcher_background.xml aktualisiert")

print("\nFertig! App neu bauen um das Icon zu sehen.")
