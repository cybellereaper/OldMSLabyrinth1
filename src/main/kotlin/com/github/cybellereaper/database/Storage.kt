package com.github.cybellereaper.database

import com.mongodb.client.model.changestream.ChangeStreamDocument
import org.litote.kmongo.Id


interface Storage<T : Any> {
    fun insertOrUpdate(id: Id<T>, entity: T)
    fun get(id: Id<T>): T?
    fun getAll(): List<T>
    fun remove(id: Id<T>)
    fun listenForChanges(onChange: (ChangeStreamDocument<T>) -> Unit)
}