package com.github.cybellereaper.inventory.itemstack

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import net.kyori.adventure.nbt.TagStringIO
import net.minestom.server.item.ItemStack

class ItemStackDeserializer : StdDeserializer<ItemStack>(ItemStack::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ItemStack =
        ItemStack.fromItemNBT(TagStringIO.get().asCompound(p.text))
}