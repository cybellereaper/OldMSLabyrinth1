package com.github.cybellereaper.inventory

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.github.cybellereaper.database.MongoStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.TagStringIO
import net.minestom.server.entity.Player
import net.minestom.server.event.GlobalEventHandler
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.id.StringId


object PlayerInventoryManager {
    private val mongoStorage: MongoStorage<TPlayerInventory> = MongoStorage(TPlayerInventory::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    suspend fun deserialize(player: Player): Boolean {
        val playerInventory = mongoStorage.get(StringId(player.uuid.toString()))
        return if (playerInventory != null) {
            playerInventory.inventory.forEachIndexed { index, itemStack ->
                player.inventory.setItemStack(index, itemStack)
            }
            true
        } else {
            false
        }
    }

    suspend fun serialize(player: Player) {
        val playerInventory = TPlayerInventory(
            player.uuid.toString(),
            player.inventory.itemStacks.toList()
        )
        mongoStorage.insertOrUpdate(StringId(player.uuid.toString()), playerInventory)
    }

    fun setupEvents(globalEventHandler: GlobalEventHandler) {

        globalEventHandler.addListener(PlayerSpawnEvent::class.java) { event ->
            val player = event.player
            coroutineScope.launch {
                deserialize(player)
            }
        }

        globalEventHandler.addListener(PlayerDisconnectEvent::class.java) { event ->
            val player = event.player
            coroutineScope.launch {
                serialize(player)
            }
        }

    }
}

class ItemStackSerializer : StdSerializer<ItemStack>(ItemStack::class.java) {
    override fun serialize(value: ItemStack, gen: JsonGenerator, provider: SerializerProvider) {
        val nbtString = TagStringIO.get().asString(value.toItemNBT())
        gen.writeString(nbtString)
    }
}

class ItemStackDeserializer : StdDeserializer<ItemStack>(ItemStack::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ItemStack {
        val nbtString = p.text
        val nbt = TagStringIO.get().asCompound(nbtString)
        return ItemStack.fromItemNBT(nbt)
    }
}

object InventorySerializer : KSerializer<List<ItemStack>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Inventory", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: List<ItemStack>) {
        val serialized = value.map { it.toItemNBT() }
            .fold(CompoundBinaryTag.builder()) { builder, tag ->
                builder.put(Tag.Integer("slot").toString(), tag)
            }
            .build()
        val snbt = TagStringIO.get().asString(serialized)
        encoder.encodeString(snbt)
    }


    override fun deserialize(decoder: Decoder): List<ItemStack> {
        val snbt = decoder.decodeString()
        val compound = TagStringIO.get().asCompound(snbt)
        return (0 until compound.size()).mapNotNull { index ->
            compound.get(index.toString())?.let { tag ->
                if (tag is CompoundBinaryTag) {
                    ItemStack.fromItemNBT(tag)
                } else {
                    null
                }
            }
        }
    }
}


@Serializable
data class TPlayerInventory(
    @BsonId @SerialName("_id")
    val uuid: String,
    @Serializable(with = InventorySerializer::class)
    val inventory: List<ItemStack>
)