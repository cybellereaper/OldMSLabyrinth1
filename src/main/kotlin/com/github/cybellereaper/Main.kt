package com.github.cybellereaper.com.github.cybellereaper

import com.github.cybellereaper.spell.SpellSystem
import com.github.cybellereaper.spell.SpellSystem.ClickType
import com.github.cybellereaper.spell.SpellSystem.SpellMode
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerHandAnimationEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block

fun main() {
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

    SpellSystem.registerSpell(
        name = "heal",
        sequence = listOf(ClickType.LEFT, ClickType.RIGHT, ClickType.LEFT),
        mode = SpellMode.SELF,
        scriptPath = "plugins/spells/heal.py"
    )

    SpellSystem.registerSpell(
        name = "Meteor Shower",
        sequence = listOf(ClickType.LEFT, ClickType.LEFT, ClickType.RIGHT),
        mode = SpellMode.AOE,
        scriptPath = "plugins/spells/meteor_shower.py"
    )


    SpellSystem.registerSpell(
        name = "random_teleport",
        sequence = listOf(ClickType.RIGHT, ClickType.RIGHT, ClickType.RIGHT),
        mode = SpellMode.SELF,
        scriptPath = "plugins/spells/random_teleport.py"
    )

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
