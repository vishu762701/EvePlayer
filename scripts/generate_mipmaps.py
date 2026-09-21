import os
import subprocess

# Generate legacy mipmap webp files from the 512x512 logo
densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

src = "app/src/main/res/drawable/img_eve_logo.png"

for folder, size in densities.items():
    out_dir = os.path.join("app/src/main/res", folder)
    os.makedirs(out_dir, exist_ok=True)
    
    # Square launcher
    out_sq = os.path.join(out_dir, "ic_launcher.webp")
    cmd1 = f"convert {src} -resize {size}x{size} {out_sq}"
    subprocess.run(cmd1, shell=True, check=True)
    
    # Round launcher
    out_rd = os.path.join(out_dir, "ic_launcher_round.webp")
    cmd2 = f"convert {src} -resize {size}x{size} {out_rd}"
    subprocess.run(cmd2, shell=True, check=True)

print("Generated all mipmap webp files successfully.")
