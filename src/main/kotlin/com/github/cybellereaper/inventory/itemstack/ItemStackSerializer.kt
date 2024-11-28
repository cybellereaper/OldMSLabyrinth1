package com.github.cybellereaper.inventory.itemstack

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import net.kyori.adventure.nbt.TagStringIO
import net.minestom.server.item.ItemStack

class ItemStackSerializer : StdSerializer<ItemStack>(ItemStack::class.java) {
    override fun serialize(value: ItemStack, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(TagStringIO.get().asString(value.toItemNBT()))
    }
}