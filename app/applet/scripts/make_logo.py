import math

# We will generate an SVG and a PNG with ImageMagick directly.
# Let's test the complete wireframe paths:

# The logo consists of clean paths:
# Path 1: Top arm
#   (354, 395) -> (628, 268) -> (628, 575) -> (354, 490) -> (354, 395)
#   Internal line: (354, 395) -> (628, 480)
#
# Path 2: Bottom block
#   (405, 555) -> (405, 665) -> (628, 745) -> (628, 622)
#
# Path 3: Middle connection fold
#   (405, 555) -> (570, 485) -> (570, 595) -> (628, 622)
#   Wait, does (570, 595) connect to (405, 555)?
#   Let's check if there is a line from (405, 555) to (570, 595) or from (445, 535)

svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 1000" width="1000" height="1000">
  <rect width="1000" height="1000" fill="#000000"/>
  <g stroke="#FFFFFF" stroke-width="24" stroke-linecap="round" stroke-linejoin="miter" fill="none">
    <!-- Top arm outer boundary -->
    <path d="M 354 395 L 628 268 L 628 575 L 354 490 Z" />
    <!-- Top arm divider -->
    <path d="M 354 395 L 628 472" />
    
    <!-- Middle fold -->
    <path d="M 405 550 L 575 470" />
    <path d="M 575 470 L 575 585" />
    <path d="M 575 585 L 628 610" />
    
    <!-- Bottom block -->
    <path d="M 405 550 L 405 665 L 628 745 L 628 610" />
    <path d="M 405 550 L 575 610" />
  </g>
</svg>"""

with open("logo_v1.svg", "w") as f:
    f.write(svg)
print("Wrote logo_v1.svg")
