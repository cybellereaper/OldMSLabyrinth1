package com.github.cybellereaper.spell.player

import com.github.cybellereaper.spell.SpellSystem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId


@Serializable
data class SpellSelection(
    @BsonId @SerialName("_id") val id: String,
    val sequence: List<SpellSystem.ClickType>,
    var active: Boolean = true
)
