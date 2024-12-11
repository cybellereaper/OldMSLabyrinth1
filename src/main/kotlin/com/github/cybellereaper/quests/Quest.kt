package com.github.cybellereaper.quests

data class Quest(
    val id: String,
    val name: String,
    val description: String,
    val type: QuestType,
    val level: Int,
    val objectives: List<QuestObjective>,
    val rewards: QuestRewards
) {
    var status: QuestStatus = QuestStatus.NOT_STARTED

    fun isCompleted(): Boolean = objectives.all { it.isComplete }
}