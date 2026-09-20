import math

# Project 3D (X, Y, Z) to 2D isometric screen coordinates (sx, sy):
# Angle: 30 degrees
cos30 = math.cos(math.radians(30))
sin30 = math.sin(math.radians(30))

def project(x, y, z, cx=500, cy=500, scale=55):
    # standard isometric:
    sx = cx + (x - y) * cos30 * scale
    sy = cy + (x + y) * sin30 * scale - z * scale
    return sx, sy

print("Projection function defined.")
