package com.github.cybellereaper.database

import org.litote.kmongo.Id


interface Storage<T : Any> {
    suspend fun insertOrUpdate(id: Id<T>, entity: T)
    suspend fun get(id: Id<T>): T?
    suspend fun getAll(): List<T>
    suspend fun remove(id: Id<T>)
}