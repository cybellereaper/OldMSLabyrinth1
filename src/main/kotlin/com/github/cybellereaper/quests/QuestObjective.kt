package com.github.cybellereaper.quests

interface QuestObjective {
    val description: String
    val required: Int
    var progress: Int
    val isComplete: Boolean
        get() = progress >= required
}