package com.github.cybellereaper.commands

import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player

object GamemodeCommand : Command("gamemode", "gm") {
    init {
        setCondition { sender, _ ->
            sender is Player
        }

        addSyntax({ sender, context ->
            val player = sender as Player
            val gameModeStr = context.get<String>("gamemode").uppercase()

            try {
                val gameMode = GameMode.valueOf(gameModeStr)
                player.gameMode = gameMode
                player.sendMessage("Set game mode to $gameMode")
            } catch (e: IllegalArgumentException) {
                player.sendMessage("Invalid gamemode: $gameModeStr")
            }

        }, ArgumentType.String("gamemode").apply {
            setSuggestionCallback { _, _, suggestion ->
                GameMode.entries.forEach { mode ->
                    suggestion.addEntry(SuggestionEntry(mode.name.lowercase()))
                }
            }
        })
    }
}
