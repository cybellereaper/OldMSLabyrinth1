# spells/random_teleport.py
# -*- coding: utf-8 -*-
from net.minestom.server.coordinate import Pos
from net.kyori.adventure.text import Component
from net.kyori.adventure.text.format.TextColor import color
import random

# Get current position
current_pos = caster.getPosition()

# Random offset between -20 and 20 blocks for x and z
x_offset = random.uniform(-20, 20)
z_offset = random.uniform(-20, 20)

# Create new position (keeping y the same to avoid teleporting into ground/air)
newPos = Pos(
    current_pos.x() + x_offset,
    current_pos.y(),
    current_pos.z() + z_offset
)

caster.teleport(newPos)
caster.sendMessage(Component.text("Teleported to random location!").color(color(0x0000FF)))
