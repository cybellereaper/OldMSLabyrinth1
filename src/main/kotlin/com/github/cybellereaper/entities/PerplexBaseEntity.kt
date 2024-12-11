package com.github.cybellereaper.entities

import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType

abstract class PerplexBaseEntity(
    override val id: String,
    override val type: PerplexEntityType,
    override val level: Int,
    override val maxHealth: Double,
    override val name: String
) : PerplexEntity {
    protected var entity: Entity = EntityCreature(EntityType.ZOMBIE)

    override fun remove() {
        entity.remove()
    }
}