package com.github.cybellereaper.inventory

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.TagStringIO
import net.minestom.server.item.ItemStack

object InventorySerializer : KSerializer<List<ItemStack>> {
    override val descriptor = PrimitiveSerialDescriptor("Inventory", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: List<ItemStack>) {
        val serialized = value.mapIndexed { index, itemStack ->
            index.toString() to itemStack.toItemNBT()
        }.toMap()
        encoder.encodeString(TagStringIO.get().asString(CompoundBinaryTag.from(serialized)))
    }

    override fun deserialize(decoder: Decoder): List<ItemStack> {
        val compound = TagStringIO.get().asCompound(decoder.decodeString())
        return (0 until compound.size()).mapNotNull { index ->
            (compound.get(index.toString()) as? CompoundBinaryTag)?.let { ItemStack.fromItemNBT(it) }
        }
    }
}