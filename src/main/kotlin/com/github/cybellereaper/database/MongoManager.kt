package com.github.cybellereaper.database

import com.mongodb.MongoClientSettings.builder
import com.mongodb.client.MongoClient
import org.bson.UuidRepresentation
import org.litote.kmongo.KMongo

object MongoManager {
    private val mongodbClientSettings = builder().uuidRepresentation(UuidRepresentation.STANDARD)
        .build()

    val mongodbClient: MongoClient = KMongo.createClient(mongodbClientSettings)
}