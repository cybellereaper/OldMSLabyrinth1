package com.github.cybellereaper.commands

import com.github.cybellereaper.questManager
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player

object QuestCommand : Command("quest") {
    init {
        addSyntax({ sender, context ->
            if (sender !is Player) return@addSyntax

            val questId = context.get<String>("quest-id")
            val success = questManager.assignQuest(sender, questId)

            if (!success) {
                sender.sendMessage("Quest not found!")
                return@addSyntax
            }
        }, ArgumentType.String("quest-id"))
    }
}