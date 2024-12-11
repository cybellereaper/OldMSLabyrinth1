package com.github.cybellereaper.commands

import com.github.cybellereaper.entityRegistar
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import net.minestom.server.entity.Player

object SpawnEntity : Command("summon", "give") {
    init {
        addSyntax({ sender, context ->
            val player = sender as? Player ?: return@addSyntax

            val entityId = context.get<String>("entity-id")
            val success = entityRegistar.spawnEntity(
                entityId,
                player.instance,
                player.position
            )

            if (!success) {
                sender.sendMessage("Failed to spawn entity!")
                return@addSyntax
            }

            sender.sendMessage("Successfully summoned $entityId")
        }, ArgumentType.String("entity-id").apply {
            setSuggestionCallback { _, _, suggestion ->
                entityRegistar.getAvailableEntities().forEach { _ ->
                    for (entry in entityRegistar.getAvailableEntities()) suggestion.addEntry(SuggestionEntry(entry))
                }
            }
        })
    }
}


