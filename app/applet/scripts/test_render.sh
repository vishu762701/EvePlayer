import subprocess

# Canvas size: 1024x1024
# Background: black
# Line color: white
# Stroke width: 22

# Let's define the points precisely:
# All down-right lines have slope +0.50 (angle ~26.57 deg or ~28 deg)
# All up-right lines have slope -0.50 (angle ~-26.57 deg or ~-28 deg)
# All vertical lines are dx = 0

# Let's test the isometric geometry:
# Right vertical boundary X_R = 630
# Top-left apex: A = (350, 395)
# Top-right corner: B = (630, 255)  [dx = 280, dy = -140, slope = -0.50]
# Right inner apex: C = (630, 435)  [dx = 280, dy = +140, slope = +0.50 from A(350, 295)? Wait!]
# If A is at (350, 395):
# A -> B: dx=280, dy=-140 -> B = (630, 255)
# A -> C: dx=280, dy=+140 -> C = (630, 535)
# Vertical drop H = 105:
# D = (350, 500)
# E = (630, 640)

# But wait, look at the proportions in the image:
# Is C lower than B? Yes, B is at top-right, C is directly below B on the right edge!
# Length of B->C: about 140-160px.
# Then below C, vertical line to E.
# Let's measure:

script = """
convert -size 1024x1024 xc:black \\
  -stroke white -strokewidth 24 -strokelinecap round -strokelinejoin round -fill none \\
  -draw "path 'M 354,395 L 628,268 L 628,575 L 354,490 Z'" \\
  -draw "line 354,395 628,480" \\
  logo_test1.png
"""
