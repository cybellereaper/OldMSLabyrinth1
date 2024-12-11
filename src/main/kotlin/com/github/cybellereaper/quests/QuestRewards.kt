package com.github.cybellereaper.quests

data class QuestRewards(
    val experience: Int = 0,
    val gold: Int = 0,
    val items: Map<String, Int> = emptyMap() // ItemID to quantity
)
