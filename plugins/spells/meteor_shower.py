# spells/meteor_shower.py
# -*- coding: utf-8 -*-
from net.minestom.server.coordinate import Vec
import random

# Get positions above targets for meteor spawning
for target in targets:
    if hasattr(target, 'damage'):
        # Create impact position at target location
        impact_pos = target.getPosition()

        # Create meteor starting position (15 blocks above target)
        start_pos = impact_pos.add(Vec(
            random.uniform(-5, 5),  # Random X offset
            15,                     # Height
            random.uniform(-5, 5)   # Random Z offset
        ))

        # Apply damage and fire effect
        target.damage(12.0)
        target.setFireForDuration(100)  # 5 seconds of fire

        # Visual/sound effects could be added here when available

caster.sendMessage("Called down a meteor shower!")
