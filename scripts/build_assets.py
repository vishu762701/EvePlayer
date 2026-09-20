import os
import subprocess

# Let's generate the vector paths and PNG
# We want clean, sharp, exact lines matching the user's uploaded logo.

# Points in 108x108 adaptive icon coordinate space:
# Centered at (54, 54)
# Left apex: (39.5, 43.5)
# Top-right: (68.0, 29.0)
# Top-arm right crease: (68.0, 50.5)
# Top-arm bottom-left: (39.5, 53.5)
# Top-arm bottom-right: (68.0, 60.5)

# Middle arm:
# Inner spine: (45.5, 58.0)
# Middle tip top: (61.0, 50.0)
# Middle tip vertical drop: (61.0, 60.0)
# Middle bottom crease: (45.5, 68.0)

# Bottom arm:
# Left spine: (45.5, 58.0) to (45.5, 68.0) to (45.5, 78.0)
# Bottom-left: (45.5, 69.5)
# Bottom-right: (68.0, 79.5)
# Bottom right top: (68.0, 69.5)

# Let's test the complete SVG / Vector paths:
vector_xml = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- Top Sloped Triangular Face -->
    <path
        android:pathData="M39.5,43.5 L68,29 L68,50.5 Z"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="2.5"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="#00000000" />

    <!-- Top Front Vertical Parallelogram -->
    <path
        android:pathData="M39.5,43.5 L39.5,53.5 L68,60.5 L68,50.5"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="2.5"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="#00000000" />

    <!-- Middle Fold / Recess -->
    <path
        android:pathData="M45.5,58 L61,50 L61,60"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="2.5"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="#00000000" />

    <!-- Bottom Front Face & Base -->
    <path
        android:pathData="M45.5,58 L45.5,69.5 L68,79.5 L68,69.5"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="2.5"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="#00000000" />

    <!-- Bottom arm top crease -->
    <path
        android:pathData="M45.5,69.5 L61,60 L68,69.5"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="2.5"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:fillColor="#00000000" />
</vector>
"""

with open("app/src/main/res/drawable/ic_launcher_foreground.xml", "w") as f:
    f.write(vector_xml)

with open("app/src/main/res/drawable/ic_eve_logo.xml", "w") as f:
    f.write(vector_xml)

# Pure Black Background for adaptive icon to match the user's logo exactly
bg_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:pathData="M0,0h108v108h-108z"
        android:fillColor="#000000" />
</vector>
"""

with open("app/src/main/res/drawable/ic_launcher_background.xml", "w") as f:
    f.write(bg_xml)

print("Vector drawables written.")

# Now generate a 512x512 PNG img_eve_logo.png
# Scale 108 coordinates to 512:
scale = 512.0 / 108.0

lines = [
    # Top triangle:
    ((39.5, 43.5), (68, 29)),
    ((68, 29), (68, 50.5)),
    ((68, 50.5), (39.5, 43.5)),
    # Top front face:
    ((39.5, 43.5), (39.5, 53.5)),
    ((39.5, 53.5), (68, 60.5)),
    ((68, 50.5), (68, 60.5)),
    # Middle fold:
    ((45.5, 58), (61, 50)),
    ((61, 50), (61, 60)),
    # Bottom front face:
    ((45.5, 58), (45.5, 69.5)),
    ((45.5, 69.5), (68, 79.5)),
    ((68, 79.5), (68, 69.5)),
    # Bottom crease:
    ((45.5, 69.5), (61, 60)),
    ((61, 60), (68, 69.5)),
]

draw_cmds = []
for p1, p2 in lines:
    x1, y1 = round(p1[0] * scale), round(p1[1] * scale)
    x2, y2 = round(p2[0] * scale), round(p2[1] * scale)
    draw_cmds.append(f"line {x1},{y1} {x2},{y2}")

draw_str = " -draw \"" + "\" -draw \"".join(draw_cmds) + "\""

cmd = f"""convert -size 512x512 xc:black \\
  -stroke white -strokewidth 12 -fill none \\
  -draw "stroke-linecap round stroke-linejoin round" \\
  {draw_str} \\
  app/src/main/res/drawable/img_eve_logo.png"""

subprocess.run(cmd, shell=True, check=True)
print("Generated img_eve_logo.png successfully.")
