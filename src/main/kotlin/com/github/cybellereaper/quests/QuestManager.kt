package com.github.cybellereaper.quests

import net.minestom.server.entity.Player
import java.util.*

class QuestManager {
    private val quests = mutableMapOf<String, Quest>()
    private val playerQuests = mutableMapOf<UUID, MutableList<Quest>>()

    fun registerQuest(quest: Quest) {
        quests[quest.id] = quest
    }

    fun assignQuest(player: Player, questId: String): Boolean {
        val quest = quests[questId] ?: return false
        playerQuests.getOrPut(player.uuid) { mutableListOf() }.add(quest)
        quest.status = QuestStatus.IN_PROGRESS
        player.sendMessage("Quest started: ${quest.name}")
        return true
    }

    fun updateProgress(player: Player, questId: String, objectiveIndex: Int, progress: Int) {
        playerQuests[player.uuid]?.find { it.id == questId }?.let { quest ->
            quest.objectives.getOrNull(objectiveIndex)?.let { objective ->
                objective.progress += progress
                if (quest.isCompleted() && quest.status != QuestStatus.COMPLETED) {
                    completeQuest(player, quest)
                }
            }
        }
    }

    private fun completeQuest(player: Player, quest: Quest) {
        quest.status = QuestStatus.COMPLETED
        // Apply rewards
        quest.rewards.let { rewards ->
            // Add experience
            // Add gold
            // Add items
            player.sendMessage("Quest completed: ${quest.name}")
        }
    }

    fun getPlayerQuests(player: Player): List<Quest> {
        return playerQuests[player.uuid] ?: emptyList()
    }
}