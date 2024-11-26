package com.github.cybellereaper.spell

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.ReplaceOptions
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerHandAnimationEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.Material
import org.bson.Document
import org.python.util.PythonInterpreter
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import java.util.Base64
object SpellSystem {
    private const val CLICK_TIMEOUT = 1000L
    private const val MAX_SIGHT_RANGE = 50.0
    private  val WAND_MATERIAL = Material.STICK
    private val interpreter = PythonInterpreter()
    enum class ClickType { LEFT, RIGHT }
    enum class SpellMode { SINGLE, AOE, SELF }
    enum class SpellSource { DATABASE }
    data class Spell(
        val name: String,
        val sequence: List<ClickType>,
        val mode: SpellMode,
        val scriptContent: String,
        val source: SpellSource = SpellSource.DATABASE
    )

    private val spells = mutableSetOf<Spell>()
    private val clickStates = mutableMapOf<Player, ClickState>()
    private lateinit var database: MongoDatabase

    fun initialize(mongoClient: MongoClient) {
        database = mongoClient.getDatabase("spells_db")
        loadSpellsFromDatabase()
    }

    private fun loadSpellsFromDatabase() {
        val collection = database.getCollection("spells")
        collection.find().forEach { doc ->
            spells += Spell(
                name = doc.getString("_id"),
                sequence = doc.getList("sequence", String::class.java)
                    .map { ClickType.valueOf(it) },
                mode = SpellMode.valueOf(doc.getString("mode")),
                scriptContent = String(Base64.getDecoder().decode(doc.getString("scriptContent"))),
            )
        }
    }
    fun registerSpell(name: String, sequence: List<ClickType>, mode: SpellMode, scriptContent: String): Spell {
        val collection = database.getCollection("spells")

        // Encode script content to base64
        val encodedScript = Base64.getEncoder().encodeToString(scriptContent.toByteArray())

        // Try to find existing spell
        val existingSpell = collection.find(Document("_id", name)).first()
        if (existingSpell != null) {
            // Convert document to Spell and return it
            return Spell(
                name = existingSpell.getString("_id"),
                sequence = existingSpell.getList("sequence", String::class.java)
                    .map { ClickType.valueOf(it) },
                mode = SpellMode.valueOf(existingSpell.getString("mode")),
                scriptContent = String(Base64.getDecoder().decode(existingSpell.getString("scriptContent")))
            )
        }

        val document = Document()
            .append("_id", name)
            .append("sequence", sequence.map { it.name })
            .append("mode", mode.name)
            .append("scriptContent", encodedScript)

        collection.insertOne(document)

        return Spell(name, sequence, mode, scriptContent)
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
            interpreter.exec(spell.scriptContent)
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