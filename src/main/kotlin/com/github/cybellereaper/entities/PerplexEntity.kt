package com.github.cybellereaper.entities

import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance


interface PerplexEntity {
    val id: String
    val type: PerplexEntityType
    val level: Int
    val maxHealth: Double
    val name: String

    fun spawn(instance: Instance, position: Pos)
    fun remove()
}


