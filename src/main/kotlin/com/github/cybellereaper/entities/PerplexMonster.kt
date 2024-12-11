package com.github.cybellereaper.entities

import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.Player
import net.minestom.server.entity.ai.GoalSelector
import net.minestom.server.entity.ai.goal.MeleeAttackGoal
import net.minestom.server.entity.ai.goal.RandomStrollGoal
import net.minestom.server.instance.Instance
import org.python.icu.util.TimeUnit
import java.util.List


class PerplexMonster(
    override val id: String,
    override val type: PerplexEntityType,
    override val level: Int,
    override val maxHealth: Double,
    override val name: String,
    override val damage: Double,
    override val defense: Double,
    override val attackSpeed: Double
) : PerplexBaseEntity(id, type, level, maxHealth, name), PerplexEntityCombatStats {
    override fun spawn(instance: Instance, position: Pos) {
        entity.let {
            it.setInstance(instance, position)
            it.customName = Component.text("$name Lvl.$level")
            it.isCustomNameVisible = true
        }
        val monster = entity as? EntityCreature ?: return
        val randomStroll = RandomStrollGoal(monster, 5)
        monster.addAIGroup(listOf(randomStroll), listOf())
        val player = randomStroll.findTarget() as? Player ?: return
        player.sendMessage("I found you!")
        entity.spawn()
    }
}