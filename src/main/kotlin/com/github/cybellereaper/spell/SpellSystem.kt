package com.github.cybellereaper.spell

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerHandAnimationEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.Material
import org.python.util.PythonInterpreter
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

object SpellSystem {
    private const val CLICK_TIMEOUT = 1000L
    private const val MAX_SIGHT_RANGE = 50.0
    private  val WAND_MATERIAL = Material.STICK
    private val interpreter = PythonInterpreter()
    enum class ClickType { LEFT, RIGHT }
    enum class SpellMode { SINGLE, AOE, SELF }

    data class Spell(
        val name: String,
        val sequence: List<ClickType>,
        val mode: SpellMode,
        val scriptPath: String
    )

    private val spells = mutableSetOf<Spell>()
    private val clickStates = mutableMapOf<Player, ClickState>()

    fun registerSpell(name: String, sequence: List<ClickType>, mode: SpellMode, scriptPath: String) {
        spells += Spell(name, sequence, mode, scriptPath)
    }

    fun handleWandInteraction(player: Player, material: Material, clickType: ClickType) {
        if (material != WAND_MATERIAL) return

        val clickState = clickStates.getOrPut(player) { ClickState() }

        with(clickState) {
            if (hasTimedOut()) clicks.clear()
            clicks += clickType
            lastClickTime = System.currentTimeMillis()
        }

        updateActionBar(player)
        checkAndCastSpell(player)
    }

    fun handleWandUse(event: PlayerUseItemEvent) {
        handleWandInteraction(event.player, event.itemStack.material(), ClickType.RIGHT)
    }

    fun handleWandAnimation(event: PlayerHandAnimationEvent) {
        handleWandInteraction(event.player, event.player.itemInMainHand.material(), ClickType.LEFT)
    }

    private fun updateActionBar(player: Player) {
        clickStates[player]?.let { state ->
            val sequence = state.clicks.joinToString(" ")
            player.sendActionBar(Component.text(sequence))
        }
    }

    private fun checkAndCastSpell(player: Player) {
        val state = clickStates[player] ?: return

        spells.firstOrNull { spell ->
            state.clicks.takeLast(spell.sequence.size) == spell.sequence
        }?.also { spell ->
            executeSpell(spell, player)
            state.clicks.clear()
        }
    }

    private fun executeSpell(spell: Spell, caster: Player) {
        val targets = when (spell.mode) {
            SpellMode.SINGLE -> caster.getLineOfSight(MAX_SIGHT_RANGE.toInt())
                .filterIsInstance<Entity>()
                .firstOrNull { it != caster }
                ?.let { listOf(it) }
                ?: emptyList()

            SpellMode.SELF -> listOf(caster)

            SpellMode.AOE -> caster.instance?.getNearbyEntities(caster.position, MAX_SIGHT_RANGE)
                ?.filterNot { it == caster }
                ?: emptyList()
        }

        try {
            interpreter.set("caster", caster)
            interpreter.set("targets", targets)
            interpreter.set("spellName", spell.name)
            val scriptFile = File(spell.scriptPath)
            if (!scriptFile.exists()) {
                caster.sendMessage(Component.text("Spell script not found: ${spell.scriptPath}"))
                return
            }
            interpreter.execfile(scriptFile.absolutePath)
        } catch (e: Exception) {
            caster.sendMessage(Component.text("Failed to execute spell: ${e.message}"))
            e.printStackTrace()
        }
    }

    private data class ClickState(
        var lastClickTime: Long = System.currentTimeMillis(),
        val clicks: MutableList<ClickType> = mutableListOf()
    ) {
        fun hasTimedOut() = System.currentTimeMillis() - lastClickTime > CLICK_TIMEOUT
    }
}