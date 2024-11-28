package com.github.cybellereaper

import com.github.cybellereaper.inventory.player.PlayerInventoryManager
import com.github.cybellereaper.spell.SpellSystem
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerHandAnimationEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block


suspend fun main() {
    val minecraftServer = MinecraftServer.init()
    val instanceManager = MinecraftServer.getInstanceManager()
    val instanceContainer = instanceManager.createInstanceContainer()

    instanceContainer.setChunkSupplier(::LightingChunk)

    instanceContainer.setGenerator { unit ->
        unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK)
    }

    val globalEventHandler = MinecraftServer.getGlobalEventHandler()

    globalEventHandler.addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
        with(event) {
            spawningInstance = instanceContainer
            player.respawnPoint = Pos(0.0, 42.0, 0.0)
            player.gameMode = GameMode.CREATIVE
        }
    }

    PlayerInventoryManager.setupEvents(globalEventHandler)


    globalEventHandler.apply {
        addListener(PlayerHandAnimationEvent::class.java) { event ->
            SpellSystem.handleWandAnimation(event)
        }

        addListener(PlayerUseItemEvent::class.java) { event ->
            SpellSystem.handleWandUse(event)
        }
    }

    MojangAuth.init()
    minecraftServer.start("0.0.0.0", 25565)
}


