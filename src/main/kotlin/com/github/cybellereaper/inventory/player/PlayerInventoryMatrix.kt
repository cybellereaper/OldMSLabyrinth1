package com.github.cybellereaper.inventory.player

import com.github.cybellereaper.inventory.InventorySerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minestom.server.item.ItemStack
import org.bson.codecs.pojo.annotations.BsonId

@Serializable
data class PlayerInventoryMatrix(
    @BsonId @SerialName("_id") val uuid: String,
    @Serializable(with = InventorySerializer::class) val inventory: List<ItemStack>
)
