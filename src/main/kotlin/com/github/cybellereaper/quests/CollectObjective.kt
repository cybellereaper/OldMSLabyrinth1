package com.github.cybellereaper.quests

data class CollectObjective(
    override val description: String,
    override val required: Int,
    val itemId: String
) : QuestObjective {
    override var progress: Int = 0
}