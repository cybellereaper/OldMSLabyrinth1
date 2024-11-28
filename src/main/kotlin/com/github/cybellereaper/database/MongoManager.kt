package com.github.cybellereaper.database

import com.fasterxml.jackson.databind.module.SimpleModule
import com.github.cybellereaper.inventory.itemstack.ItemStackDeserializer
import com.github.cybellereaper.inventory.itemstack.ItemStackSerializer
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import net.minestom.server.item.ItemStack
import org.bson.UuidRepresentation
import org.litote.kmongo.KMongo
import org.litote.kmongo.id.jackson.IdJacksonModule
import org.litote.kmongo.util.KMongoConfiguration

object MongoManager {
    val mongodbClientSettings: MongoClient = KMongo.createClient(MongoClientSettings.builder().apply {
        applicationName("Perplex-Minestom")
        uuidRepresentation(UuidRepresentation.STANDARD)
        applyConnectionString(ConnectionString("mongodb://localhost:27017"))
    }.build())

    init {
        val module = SimpleModule().apply {
            addSerializer(ItemStack::class.java, ItemStackSerializer())
            addDeserializer(ItemStack::class.java, ItemStackDeserializer())
        }

        KMongoConfiguration.registerBsonModule(IdJacksonModule())
        KMongoConfiguration.registerBsonModule(module)
    }


}