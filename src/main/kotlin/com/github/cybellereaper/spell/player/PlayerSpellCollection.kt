package com.github.cybellereaper.spell.player

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Id

@Serializable
data class PlayerSpellCollection(
    @SerialName("_id") @BsonId val _id: Id<PlayerSpellCollection>,
    val spells: MutableSet<SpellSelection>
)