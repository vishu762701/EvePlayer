import math
import subprocess

# Let's define the 3D vertices of an isometric wireframe E
# Let's check which faces are visible in standard top-left-front view:
# If viewer is at (+X, -Y, +Z):
# Top faces (+Z normal) are visible
# Front faces (-Y normal or +X normal) are visible
# Wireframe shows all outer silhouette and visible crease edges.

# Let's test a wireframe E where:
# Arms extend along +X (so they go down-right on screen)
# Spine is along Z (vertical) or Y
# Let's check the user image again:
# In the user image:
# The right side has:
# - Top right vertical line: from (628, 268) to (628, 575)
# - An internal line meeting at (628, 472)
# - Gap
# - Middle vertical line: at (575, 470) to (575, 585)
# - Gap
# - Bottom right vertical line: from (628, 610) to (628, 745)
#
# Let's test rendering these exact lines and see how it matches:

lines = [
    # Top arm:
    ((354, 395), (628, 268)), # top edge
    ((628, 268), (628, 575)), # right vertical edge
    ((354, 490), (628, 575)), # bottom edge of top front face
    ((354, 395), (354, 490)), # left vertical edge of top front face
    ((354, 395), (628, 472)), # crease dividing top sloped face and front face
    
    # Middle arm:
    ((405, 550), (575, 470)), # top edge of middle arm
    ((575, 470), (575, 585)), # right vertical edge of middle arm
    ((405, 665), (575, 585)), # bottom edge of middle arm? or fold?
    
    # Bottom arm:
    ((405, 550), (405, 665)), # left vertical edge
    ((405, 665), (628, 745)), # bottom-most edge
    ((628, 745), (628, 610)), # bottom right vertical edge
    ((575, 585), (628, 610)), # fold connecting to bottom arm
]

draw_cmds = []
for p1, p2 in lines:
    draw_cmds.append(f"line {p1[0]},{p1[1]} {p2[0]},{p2[1]}")

draw_str = " -draw \"" + "\" -draw \"".join(draw_cmds) + "\""

cmd = f"""convert -size 1024x1024 xc:black \\
  -stroke white -strokewidth 24 -fill none \\
  -draw "stroke-linecap round stroke-linejoin round" \\
  {draw_str} \\
  test_e_wireframe.png"""

subprocess.run(cmd, shell=True, check=True)
print("Rendered test_e_wireframe.png successfully")
