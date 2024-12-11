package com.github.cybellereaper.entities

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.ai.goal.RandomStrollGoal
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.entity.EntityDeathEvent
import net.minestom.server.instance.Instance

// SUFFIX: A bit unstable on death.

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

    private companion object {
        private const val DEFAULT_STROLL_RADIUS = 5
        private const val DEFAULT_HEALTH = 20.0f
    }

    override fun spawn(instance: Instance, position: Pos) {
        spawnEntity(instance, position)
        setupAI()
        registerEventHandlers()
        entity.spawn()
    }

    private fun spawnEntity(instance: Instance, position: Pos) {
        entity.apply {
            setInstance(instance, position)
            customName = Component.text("$name Lvl.$level")
            isCustomNameVisible = true
        }
    }

    private fun setupAI() {
        (entity as? EntityCreature)?.let { monster ->
            val randomStroll = RandomStrollGoal(monster, DEFAULT_STROLL_RADIUS)
            monster.addAIGroup(listOf(randomStroll), emptyList())
        }
    }

    private fun registerEventHandlers() {
        val eventNode = MinecraftServer.getGlobalEventHandler()

        eventNode.apply {
            addListener(EntityDamageEvent::class.java) { event ->
                handleDamageEvent(event)
            }

            addListener(EntityDeathEvent::class.java) { event ->
                handleDeathEvent(event)
            }

            addListener(EntityAttackEvent::class.java) { event ->
                handleAttackEvent(event)
            }
        }
    }

    private fun handleDamageEvent(event: EntityDamageEvent) {
        val entity = event.entity as? LivingEntity ?: return
        if (entity.health <= event.damage.amount) {
            entity.eventNode().call(EntityDeathEvent(entity))
        }
    }

    private fun handleDeathEvent(event: EntityDeathEvent) {
        val targetCreature = event.entity as? EntityCreature ?: return
        targetCreature.health = DEFAULT_HEALTH
        event.instance.sendMessage(Component.text("Killed ${targetCreature.isDead}"))
    }

    private fun handleAttackEvent(event: EntityAttackEvent) {
        if (event.target != entity) return
        (event.target as? LivingEntity)?.let { target ->
            target.health -= damage.toFloat() // Use the monster's actual damage value instead of hardcoded value
        }
    }
}