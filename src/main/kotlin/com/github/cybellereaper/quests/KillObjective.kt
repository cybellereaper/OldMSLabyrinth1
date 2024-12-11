package com.github.cybellereaper.quests

data class KillObjective(
    override val description: String,
    override val required: Int,
    val entityId: String
) : QuestObjective {
    override var progress: Int = 0
}
