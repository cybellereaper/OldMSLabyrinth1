package com.github.cybellereaper.spell.player

import com.github.cybellereaper.database.MongoStorage
import com.github.cybellereaper.spell.SpellSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litote.kmongo.id.StringId

class PlayerSpellStorage {
    private val storage = MongoStorage(PlayerSpellCollection::class.java)

    suspend fun addSpell(playerId: String, spell: SpellSelection) = withContext(Dispatchers.IO) {
        val id = StringId<PlayerSpellCollection>(playerId)
        val collection = storage.get(id) ?: PlayerSpellCollection(
            _id = id,
            spells = mutableSetOf()
        )
        val updatedSpells = (collection.spells + spell).toMutableSet()
        storage.insertOrUpdate(id, collection.copy(spells = updatedSpells))
    }

    suspend fun getSpell(playerId: String, spell: SpellSelection): SpellSystem.SpellDocument? =
        withContext(Dispatchers.IO) {
            storage.get(StringId(playerId))?.spells
                ?.find { it.sequence == spell.sequence }
                ?.id
                ?.let { SpellSystem.getSpell(it) }
        }

    suspend fun removeSpell(playerId: String, combination: List<SpellSystem.ClickType>) = withContext(Dispatchers.IO) {
        val id = StringId<PlayerSpellCollection>(playerId)
        storage.get(id)?.let { collection ->
            val updatedSpells = collection.spells.filterNot { it.sequence == combination }.toMutableSet()
            storage.insertOrUpdate(id, collection.copy(spells = updatedSpells))
        }
    }

    suspend fun getAllSpells(playerId: String): List<SpellSelection> = withContext(Dispatchers.IO) {
        storage.get(StringId(playerId))?.spells?.toList() ?: emptyList()
    }
}