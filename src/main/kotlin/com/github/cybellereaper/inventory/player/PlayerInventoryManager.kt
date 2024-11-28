package com.github.cybellereaper.inventory.player

import com.github.cybellereaper.database.MongoStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minestom.server.entity.Player
import net.minestom.server.event.GlobalEventHandler
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import org.litote.kmongo.id.StringId

object PlayerInventoryManager {
    private val mongoStorage = MongoStorage(PlayerInventoryMatrix::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    suspend fun deserialize(player: Player): Boolean =
        mongoStorage.get(StringId(player.uuid.toString()))?.let { playerInventory ->
            playerInventory.inventory.forEachIndexed { index, itemStack ->
                player.inventory.setItemStack(index, itemStack)
            }
            true
        } ?: false

    suspend fun serialize(player: Player) {
        val playerInventory = PlayerInventoryMatrix(player.uuid.toString(), player.inventory.itemStacks.toList())
        mongoStorage.insertOrUpdate(StringId(player.uuid.toString()), playerInventory)
    }

    fun setupEvents(globalEventHandler: GlobalEventHandler) {
        globalEventHandler.apply {
            addListener(PlayerSpawnEvent::class.java) { event ->
                coroutineScope.launch { deserialize(event.player) }
            }
            addListener(PlayerDisconnectEvent::class.java) { event ->
                coroutineScope.launch { serialize(event.player) }
            }
        }
    }
}



