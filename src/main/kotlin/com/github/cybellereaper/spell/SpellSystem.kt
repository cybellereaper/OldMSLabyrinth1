package com.github.cybellereaper.spell

import com.github.cybellereaper.database.MongoStorage
import com.github.cybellereaper.spell.player.PlayerSpellStorage
import com.github.cybellereaper.spell.player.SpellSelection
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
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
    private val WAND_MATERIAL = Material.STICK
    private val interpreter by lazy { PythonInterpreter() }
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Serializable
    enum class ClickType { LEFT, RIGHT }

    @Serializable
    enum class SpellMode { SINGLE, AOE, SELF }

    @Serializable
    data class SpellDocument(
        @SerialName("_id") @BsonId val _id: Id<SpellDocument>,
        val mode: SpellMode,
        var scriptContent: String,
        var disabled: Boolean = false
    )

    private val playerSpellStorage = PlayerSpellStorage()
    private val spellStorage = MongoStorage(SpellDocument::class.java)
    private val clickStates = ConcurrentHashMap<Player, ClickState>()

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

    private fun updateActionBar(player: Player) {
        player.sendActionBar(Component.text(clickStates[player]?.clicks?.joinToString(" ") ?: ""))
    }

    private suspend fun checkAndCastSpell(player: Player) = withContext(Dispatchers.Default) {
        val state = clickStates[player] ?: return@withContext
        if (state.clicks.size < SPELL_SEQUENCE_LENGTH) return@withContext

        val playerId = player.identity().uuid().toString()
        val spellSelections = playerSpellStorage.getAllSpells(playerId)
        val spellSelection = spellSelections.find { it.sequence == state.clicks } ?: return@withContext
        val spell = playerSpellStorage.getSpell(playerId, spellSelection) ?: return@withContext

        if (!spell.disabled) {
            executeSpell(spell, player)
        }
        state.clicks.clear()
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
            caster.sendMessage(Component.text("Failed to execute spell: ${e.message}"))
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