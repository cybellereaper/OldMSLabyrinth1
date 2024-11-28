package com.github.cybellereaper.database

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.UpdateOptions
import org.litote.kmongo.Id
import org.litote.kmongo.deleteOneById
import org.litote.kmongo.findOneById
import org.litote.kmongo.updateOneById
import java.util.*


open class MongoStorage<T : Any>(private val entityClass: Class<T>) : Storage<T> {
    companion object {
        private const val DATABASE_NAME = "Perplex"
    }

    private val collection: MongoCollection<T> by lazy {
        MongoManager.mongodbClient
            .getDatabase(DATABASE_NAME)
            .getCollection(entityClass.simpleName.lowercase(Locale.getDefault()), entityClass)
    }

    override suspend fun insertOrUpdate(id: Id<T>, entity: T) {
       collection.replaceOne(
           eq("_id", id),
           entity,
           ReplaceOptions().upsert(true)
       )
    }

    override suspend fun get(id: Id<T>): T? = collection.findOneById(id)

    override suspend fun getAll(): List<T> = collection.find().toList()

    override suspend fun remove(id: Id<T>) {
        collection.deleteOneById(id)
    }
}