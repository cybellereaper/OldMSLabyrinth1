package com.github.cybellereaper.entities

object EntityFactory {
    fun createBasicMonster(
        id: String,
        name: String,
        level: Int = 1,
        maxHealth: Double = 100.0,
        damage: Double = 10.0,
        defense: Double = 5.0,
        attackSpeed: Double = 1.0
    ): PerplexMonster {
        return PerplexMonster(
            id = id,
            type = PerplexEntityType.MONSTER,
            level = level,
            maxHealth = maxHealth,
            name = name,
            damage = damage,
            defense = defense,
            attackSpeed = attackSpeed
        )
    }
}