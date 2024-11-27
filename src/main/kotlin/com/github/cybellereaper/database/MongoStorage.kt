package com.github.cybellereaper.database

import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.changestream.ChangeStreamDocument
import org.litote.kmongo.Id
import org.litote.kmongo.deleteOneById
import org.litote.kmongo.findOneById
import org.litote.kmongo.updateOneById
import java.util.*


open class MongoStorage<T : Any>(private val entityClass: Class<T>) : Storage<T> {
    companion object {
        private const val DATABASE_NAME = "Perplex"
    }

    private val collection by lazy {
        val database = MongoManager.mongodbClient.getDatabase(DATABASE_NAME)
        database.getCollection(collectionName, entityClass)
    }

    protected val collectionName: String = entityClass.simpleName.lowercase(Locale.getDefault())

    override fun insertOrUpdate(id: Id<T>, entity: T) {
        collection.updateOneById(id, entity, UpdateOptions().upsert(true))
    }

    override fun get(id: Id<T>): T? = collection.findOneById(id)

    override fun getAll(): List<T> = collection.find().toList()

    override fun remove(id: Id<T>) {
        collection.deleteOneById(id)
    }

    override fun listenForChanges(onChange: (ChangeStreamDocument<T>) -> Unit) {
        TODO("Not yet implemented")
    }
}