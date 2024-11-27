package com.github.cybellereaper.spell

import com.github.cybellereaper.database.MongoStorage
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerHandAnimationEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.Material
import org.litote.kmongo.Id
import org.litote.kmongo.id.StringId
import org.python.util.PythonInterpreter
import java.util.*

object SpellSystem {
    private const val CLICK_TIMEOUT = 1000L
    private const val MAX_SIGHT_RANGE = 50.0
    private val WAND_MATERIAL = Material.STICK
    private val interpreter = PythonInterpreter()

    @Serializable
    enum class ClickType { LEFT, RIGHT }

    @Serializable
    enum class SpellMode { SINGLE, AOE, SELF }

    @Serializable
    data class SpellDocument(
        val _id: Id<SpellDocument>,
        val sequence: List<ClickType>,
        val mode: SpellMode,
        val scriptContent: String
    )

    private val spellStorage = MongoStorage(SpellDocument::class.java)
    private val clickStates = mutableMapOf<Player, ClickState>()

    fun registerSpell(name: String, sequence: List<ClickType>, mode: SpellMode, scriptContent: String): SpellDocument {
        val id = StringId<SpellDocument>(name)
        return spellStorage.get(id) ?: SpellDocument(
            _id = id,
            sequence = sequence,
            mode = mode,
            scriptContent = Base64.getEncoder().encodeToString(scriptContent.toByteArray())
        ).also { spellStorage.insertOrUpdate(id, it) }
    }

    fun handleWandUse(event: PlayerUseItemEvent) {
        handleWandInteraction(event.player, event.itemStack.material(), ClickType.RIGHT)
    }

    fun handleWandAnimation(event: PlayerHandAnimationEvent) {
        handleWandInteraction(event.player, event.player.itemInMainHand.material(), ClickType.LEFT)
    }

    private fun handleWandInteraction(player: Player, material: Material, clickType: ClickType) {
        if (material != WAND_MATERIAL) return

        clickStates.getOrPut(player) { ClickState() }.apply {
            if (hasTimedOut()) clicks.clear()
            clicks += clickType
            lastClickTime = System.currentTimeMillis()

            updateActionBar(player)
            checkAndCastSpell(player)
        }
    }

    private fun updateActionBar(player: Player) {
        clickStates[player]?.let { state ->
            player.sendActionBar(Component.text(state.clicks.joinToString(" ")))
        }
    }

    private fun checkAndCastSpell(player: Player) {
        val state = clickStates[player] ?: return

        spellStorage.getAll().firstOrNull { spell ->
            state.clicks.takeLast(spell.sequence.size) == spell.sequence
        }?.let { spell ->
            executeSpell(spell, player)
            state.clicks.clear()
        }
    }

    private fun executeSpell(spell: SpellDocument, caster: Player) {
        val targets = getSpellTargets(spell.mode, caster)

        try {
            val decodedScript = Base64.getDecoder().decode(spell.scriptContent).toString(Charsets.UTF_8)
            interpreter.apply {
                set("caster", caster)
                set("targets", targets)
                set("spellName", spell._id.toString())
                exec(decodedScript)
            }
        } catch (e: Exception) {
            caster.sendMessage(Component.text("Failed to execute spell: ${e.message}"))
            e.printStackTrace()
        }
    }

    private fun getSpellTargets(mode: SpellMode, caster: Player): List<Entity> = when (mode) {
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

    private data class ClickState(
        var lastClickTime: Long = System.currentTimeMillis(),
        val clicks: MutableList<ClickType> = mutableListOf()
    ) {
        fun hasTimedOut() = System.currentTimeMillis() - lastClickTime > CLICK_TIMEOUT
    }
}
