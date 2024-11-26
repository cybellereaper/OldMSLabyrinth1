# spells/heal.py
# -*- coding: utf-8 -*-
if len(targets) > 0:
    target = targets[0]
    if hasattr(target, 'heal'):
        target.heal()
    caster.sendMessage("Healing spell successful!")
