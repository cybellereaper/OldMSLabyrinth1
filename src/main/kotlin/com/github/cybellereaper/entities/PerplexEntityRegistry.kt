package com.github.cybellereaper.entities

import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance

class PerplexEntityRegistry {
    private val entities = mutableMapOf<String, PerplexEntity>()

    fun registerEntity(entity: PerplexEntity) {
        entities[entity.id] = entity
        println("Registered $entity")
    }

    fun getEntity(id: String): PerplexEntity? = entities[id]

    fun spawnEntity(id: String, instance: Instance, position: Pos): Boolean {
        return entities[id]?.let {
            it.spawn(instance, position)
            true
        } ?: false
    }

    fun despawnAll() {
        entities.values.forEach { it.remove() }
    }

    fun getAvailableEntities(): List<String> {
        return entities.keys.toList()
    }
}