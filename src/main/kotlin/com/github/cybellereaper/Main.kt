package com.github.cybellereaper

import com.github.cybellereaper.commands.GamemodeCommand
import com.github.cybellereaper.commands.QuestCommand
import com.github.cybellereaper.commands.SpawnEntity
import com.github.cybellereaper.entities.EntityFactory
import com.github.cybellereaper.entities.PerplexEntityRegistry
import com.github.cybellereaper.quests.*
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block

val entityRegistar = PerplexEntityRegistry()
val questManager = QuestManager()

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
    questManager.registerQuest(createExampleQuest())
    MinecraftServer.getCommandManager().apply {
        register(GamemodeCommand)
        register(QuestCommand)
        register(SpawnEntity)
    }
    entityRegistar.registerEntity(
        EntityFactory.createBasicMonster(
            id = "goblin",
            name = "goblin",
            level = 1,
            maxHealth = 200.0,
            damage = 5.0,
        )
    )


//    PlayerInventoryManager.setupEvents(globalEventHandler)


    MojangAuth.init()
    minecraftServer.start("0.0.0.0", 25565)
}

fun createExampleQuest(): Quest {
    return Quest(
        id = "kill_goblins",
        name = "Goblin Trouble",
        description = "Clear the nearby forest of goblins",
        type = QuestType.KILL_ENTITIES,
        level = 1,
        objectives = listOf(
            KillObjective(
                description = "Kill goblins",
                required = 5,
                entityId = "goblin"
            )
        ),
        rewards = QuestRewards(
            experience = 100,
            gold = 50
        )
    )
}


