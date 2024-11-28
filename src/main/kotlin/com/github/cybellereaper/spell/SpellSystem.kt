package com.github.cybellereaper.spell

import com.github.cybellereaper.database.MongoStorage
import com.github.cybellereaper.spell.player.PlayerSpellStorage
import com.github.cybellereaper.spell.player.SpellSelection
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerHandAnimationEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.Material
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id
import org.litote.kmongo.id.StringId
import org.python.util.PythonInterpreter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object SpellSystem {
    private const val SPELL_SEQUENCE_LENGTH = 3
    private const val CLICK_TIMEOUT = 700L
    private const val MAX_SIGHT_RANGE = 50.0
    private const val CAST_INFO_DISPLAY_TIME = 3000L
    private val WAND_MATERIAL = Material.STICK
    private val interpreter by lazy { PythonInterpreter() }
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val playerSpellStorage = PlayerSpellStorage()
    private val spellStorage = MongoStorage(SpellDocument::class.java)
    private val clickStates = ConcurrentHashMap<Player, ClickState>()
    private val castingStates = ConcurrentHashMap<Player, CastingState>()

    @Serializable
    enum class ClickType { LEFT, RIGHT }

    @Serializable
    enum class SpellMode { SINGLE, AOE, SELF }

    @Serializable
    data class SpellDocument(
        @SerialName("_id") @BsonId val _id: Id<SpellDocument>,
        val mode: SpellMode,
        val mana: Int,
        val cooldown: Float = 0.0f,
        var scriptContent: String,
        var disabled: Boolean = false
    )


    suspend fun getSpell(name: String): SpellDocument? = withContext(Dispatchers.IO) {
        spellStorage.get(StringId(name))
    }

    suspend fun addPlayerSpell(playerId: String, spell: SpellSelection) = withContext(Dispatchers.IO) {
        playerSpellStorage.addSpell(playerId, spell)
    }

    fun handleWandUse(event: PlayerUseItemEvent) =
        handleWandInteraction(event.player, event.itemStack.material(), ClickType.RIGHT)

    fun handleWandAnimation(event: PlayerHandAnimationEvent) =
        handleWandInteraction(event.player, event.player.itemInMainHand.material(), ClickType.LEFT)

    private fun handleWandInteraction(player: Player, material: Material, clickType: ClickType) {
        if (material != WAND_MATERIAL) return

        clickStates.compute(player) { _, state ->
            (state ?: ClickState()).apply {
                if (hasTimedOut()) clicks.clear()
                if (clicks.size < SPELL_SEQUENCE_LENGTH) {
                    clicks.add(clickType)
                    lastClickTime = System.currentTimeMillis()
                    updateActionBar(player)
                    coroutineScope.launch { checkAndCastSpell(player) }
                }
            }
        }
    }

    private data class CastingState(
        val spellName: String,
        val manaCost: Int
    )

    private fun buildActionBarComponent(state: ClickState?, castingState: CastingState?): Component {
        val component = Component.text()
            .append(Component.text("Spell: ", NamedTextColor.GOLD))
            .append(createClickSequence(state?.clicks ?: emptyList()))
            .append(createRemainingIndicators(SPELL_SEQUENCE_LENGTH - (state?.clicks?.size ?: 0)))

        if (castingState != null) {
            component.append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(castingState.spellName, NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text("${castingState.manaCost} mana", NamedTextColor.AQUA))
                .append(Component.text(")", NamedTextColor.GRAY))
        }

        return component.build()
    }

    private fun updateActionBar(player: Player) {
        val state = clickStates[player]
        val castingState = castingStates[player]
        val component = buildActionBarComponent(state, castingState)
        player.sendActionBar(component)

        if (castingState != null) {
            coroutineScope.launch {
                delay(CAST_INFO_DISPLAY_TIME)
                castingStates.remove(player)
                updateActionBar(player)
            }
        }
    }

    private fun createRemainingIndicators(count: Int): Component {
        return Component.join(
            Component.text(" "),
            List(count) { Component.text("○", NamedTextColor.GRAY) }
        )
    }

    private fun createClickSequence(clicks: List<ClickType>): Component {
        return Component.join(
            Component.text(" "),
            clicks.map { click ->
                when (click) {
                    ClickType.LEFT -> Component.text("L", NamedTextColor.RED)
                    ClickType.RIGHT -> Component.text("R", NamedTextColor.BLUE)
                }
            }
        )
    }

    private suspend fun checkAndCastSpell(player: Player) {
        val state = clickStates[player] ?: return
        if (state.clicks.size < SPELL_SEQUENCE_LENGTH) return

        val spell = getSpellFromClicks(player)
        if (spell != null && !spell.disabled) {
            castingStates[player] = CastingState(spell._id.toString(), spell.mana)
            executeSpell(spell, player)
        }
        state.clicks.clear()
        updateActionBar(player)
    }

    private suspend fun getSpellFromClicks(player: Player): SpellDocument? {
        val playerId = player.identity().uuid().toString()
        val spellSelections = playerSpellStorage.getAllSpells(playerId)
        val spellSelection = spellSelections.find { it.sequence == clickStates[player]?.clicks } ?: return null
        return playerSpellStorage.getSpell(playerId, spellSelection)
    }

    suspend fun addSpell(spell: SpellDocument) = withContext(Dispatchers.IO) {
        spell.scriptContent = Base64.getEncoder().encodeToString(spell.scriptContent.toByteArray(Charsets.UTF_8))
        spellStorage.insertOrUpdate(spell._id, spell)
    }

    suspend fun removeSpell(spell: SpellDocument) = withContext(Dispatchers.IO) {
        spellStorage.remove(spell._id)
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
            caster.sendActionBar(Component.text("Failed to cast spell: ${e.message}", NamedTextColor.RED))
            e.printStackTrace()
        }
    }

    private fun getSpellTargets(mode: SpellMode, caster: Player): List<Entity> = when (mode) {
        SpellMode.SINGLE -> caster.getLineOfSight(MAX_SIGHT_RANGE.toInt())
            .filterIsInstance<Entity>()
            .firstOrNull { it != caster }
            ?.let(::listOf) ?: emptyList()

        SpellMode.SELF -> listOf(caster)
        SpellMode.AOE -> caster.instance?.getNearbyEntities(caster.position, MAX_SIGHT_RANGE)
            ?.filterNot { it == caster } ?: emptyList()
    }

    private data class ClickState(
        @Volatile var lastClickTime: Long = System.currentTimeMillis(),
        val clicks: MutableList<ClickType> = Collections.synchronizedList(ArrayList(SPELL_SEQUENCE_LENGTH))
    ) {
        fun hasTimedOut() = System.currentTimeMillis() - lastClickTime > CLICK_TIMEOUT
    }
}