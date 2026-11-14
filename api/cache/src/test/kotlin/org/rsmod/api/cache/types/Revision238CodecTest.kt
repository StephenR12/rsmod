package org.rsmod.api.cache.types

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.rsmod.api.cache.types.inv.InvTypeDecoder
import org.rsmod.api.cache.types.inv.InvTypeEncoder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrs2.buffer.use
import org.openrs2.buffer.writeString
import org.rsmod.api.cache.types.clientscript.ClientScriptTypeDecoder
import org.rsmod.api.cache.types.comp.ComponentTypeDecoder
import org.rsmod.api.cache.types.comp.ComponentTypeEncoder
import org.rsmod.api.cache.types.dbrow.DbRowTypeDecoder
import org.rsmod.api.cache.types.dbrow.DbRowTypeEncoder
import org.rsmod.api.cache.types.loc.LocTypeDecoder
import org.rsmod.api.cache.types.loc.LocTypeEncoder
import org.rsmod.api.cache.types.npc.NpcTypeDecoder
import org.rsmod.api.cache.types.npc.NpcTypeEncoder
import org.rsmod.api.cache.types.obj.ObjTypeDecoder
import org.rsmod.api.cache.types.obj.ObjTypeEncoder
import org.rsmod.api.cache.types.param.ParamTypeDecoder
import org.rsmod.api.cache.types.param.ParamTypeEncoder
import org.rsmod.api.cache.types.spot.SpotanimTypeDecoder
import org.rsmod.api.cache.util.EncoderContext
import org.rsmod.api.cache.util.readRawParams
import org.rsmod.api.cache.util.toInlineBuf
import org.rsmod.api.cache.util.writeRawParams
import org.rsmod.api.cache.util.writeUnsignedSmartInt
import org.rsmod.game.type.comp.ComponentTypeBuilder
import org.rsmod.game.type.loc.LocTypeBuilder
import org.rsmod.game.type.npc.NpcTypeBuilder
import org.rsmod.game.type.obj.ObjTypeBuilder
import org.rsmod.game.type.spot.SpotanimTypeBuilder
import org.rsmod.game.type.util.EntityOption

class Revision238CodecTest {
    private val context = EncoderContext.server(emptySet(), emptySet(), emptySet())

    @Test
    fun `small model ids use the revision 238 client opcodes and four byte codec`() {
        Unpooled.buffer().use { bytes ->
            val model = 40001
            bytes.writeByte(6).writeByte(1).writeInt(model).writeByte(10)
            val loc = LocTypeDecoder.decode(bytes).build(1)
            bytes.clear()
            LocTypeEncoder.encodeJs5(loc, bytes, context)
            assertEquals(6, bytes.readUnsignedByte().toInt())
            assertEquals(1, bytes.readUnsignedByte().toInt())
            assertEquals(model, bytes.readInt())
            assertEquals(10, bytes.readUnsignedByte().toInt())

            bytes.clear().writeByte(61).writeByte(1).writeInt(model)
            val npc = NpcTypeDecoder.decode(bytes).build(1)
            bytes.clear()
            NpcTypeEncoder.encodeJs5(npc, bytes, context)
            assertEquals(61, bytes.readUnsignedByte().toInt())
            assertEquals(1, bytes.readUnsignedByte().toInt())
            assertEquals(model, bytes.readInt())

            bytes.clear().writeByte(44).writeInt(model)
            val obj = ObjTypeDecoder.decode(bytes).build(1)
            bytes.clear()
            ObjTypeEncoder.encodeJs5(obj, bytes, context)
            assertEquals(44, bytes.readUnsignedByte().toInt())
            assertEquals(model, bytes.readInt())
        }
    }

    @Test
    fun `shop restock cycles retain both legacy and wide durations`() {
        for ((code, duration) in listOf(205 to 65535, 206 to 250000000)) {
            Unpooled.buffer().use { bytes ->
                bytes.writeByte(code).writeByte(0).writeByte(4).writeShort(23911)
                if (code == 206) bytes.writeInt(duration) else bytes.writeShort(duration)
                val type = InvTypeDecoder.decode(bytes).build(6006)
                bytes.clear()
                InvTypeEncoder.encodeGame(type, bytes)
                assertEquals(code, bytes.getUnsignedByte(0).toInt())
                assertEquals(duration, InvTypeDecoder.decode(bytes).build(6006).stock!![0]!!.restockCycles)
            }
        }
    }

    @Test
    fun `map location incremental smart ids and tile offsets retain their unsigned values`() {
        val values = listOf(0, 127, 128, 16384, 32767, 32768, 65534, 98304)
        Unpooled.buffer().use { data ->
            data.writeInt(7)
            for (value in values) data.writeUnsignedSmartInt(value)
            val input = data.slice(4, data.writerIndex() - 4).toInlineBuf()
            var cursor = input.newCursor()
            for (value in values) {
                cursor = input.readIncrShortSmart(cursor)
                assertEquals(value, cursor.value)
            }
            assertEquals(input.backing.size, cursor.pos)
        }
    }

    @Test
    fun `item native ground options coexist with game extensions after edits and reencoding`() {
        Unpooled.buffer().use { bytes ->
            bytes.writeByte(44).writeInt(70001)
            bytes.writeByte(45).writeInt(70002).writeByte(42)
            bytes.writeByte(15)
            bytes.writeOptions(200)
            bytes.writeByte(220).writeByte(1).writeShort(47)
            bytes.writeByte(221).writeInt(1234)
            bytes.writeByte(222).writeInt(5678)
            val item = ObjTypeDecoder.decode(bytes).build(5)
            val merged =
                ObjTypeBuilder.merge(
                    ObjTypeBuilder("edited").apply { name = "Test" }.build(5),
                    item,
                )
            bytes.clear()
            ObjTypeEncoder.encodeJs5(merged, bytes, context)
            ObjTypeEncoder.encodeGame(merged, bytes)
            val result = ObjTypeDecoder.decode(bytes).build(5)
            assertOptions(result.extraOptions)
            assertFalse(result.tradeable)
            assertEquals(70001, result.model)
            assertEquals(70002, result.manwear)
            assertEquals(42, result.manwearOff)
            assertArrayEquals(intArrayOf(47), result.objvar)
            assertEquals(1234, result.playerCost)
            assertEquals(5678, result.playerCostDerived)
        }
    }

    @Test
    fun `npc head body models and options survive a full edit cycle`() {
        Unpooled.buffer().use { bytes ->
            bytes.writeByte(61).writeByte(2).writeInt(40001).writeInt(100001)
            bytes.writeByte(62).writeByte(2).writeInt(40002).writeInt(100002)
            bytes.writeOptions(251)
            val npc = NpcTypeDecoder.decode(bytes).build(10)
            val merged = NpcTypeBuilder.merge(NpcTypeBuilder("edit").build(10), npc)
            bytes.clear()
            NpcTypeEncoder.encodeJs5(merged, bytes, context)
            val result = NpcTypeDecoder.decode(bytes).build(10)
            assertArrayEquals(intArrayOf(40001, 100001), result.models)
            assertArrayEquals(intArrayOf(40002, 100002), result.head)
            assertOptions(result.extraOptions)
        }
    }

    @Test
    fun `shaped and unshaped location models preserve the full model id`() {
        for (code in 6..7) Unpooled.buffer().use { bytes ->
            bytes.writeByte(code).writeByte(1).writeInt(78888)
            if (code == 6) bytes.writeByte(10)
            bytes.writeOptions(100)
            bytes.writeByte(96).writeByte(17)
            val loc = LocTypeDecoder.decode(bytes).build(1)
            val merged = LocTypeBuilder.merge(LocTypeBuilder("edit").build(1), loc)
            bytes.clear()
            LocTypeEncoder.encodeJs5(merged, bytes, context)
            val result = LocTypeDecoder.decode(bytes).build(1)
            assertArrayEquals(intArrayOf(78888), result.models)
            assertEquals(17, result.raise)
            assertOptions(result.extraOptions)
        }
        Unpooled.buffer().use { bytes ->
            val spot = SpotanimTypeBuilder("effect")
            bytes.writeInt(79999)
            SpotanimTypeDecoder.decode(spot, bytes, 3)
            assertEquals(79999, spot.build(1).model)
        }
    }

    @Test
    fun `definition type ids and long defaults and values roundtrip`() {
        Unpooled.buffer().use { bytes ->
            bytes.writeByte(1).writeByte('i'.code).writeByte(8).writeByte(0)
            bytes.writeByte(2).writeInt(4).writeByte(200)
            val native = ParamTypeDecoder.decode(bytes).build(14)
            bytes.clear().writeByte(1).writeByte('i'.code).writeByte(2).writeInt(4).writeByte(200)
            assertEquals(native, ParamTypeDecoder.decode(bytes).build(14))
        }
        Unpooled.buffer().use { bytes ->
            bytes.writeByte(8).writeByte(252)
            bytes.writeByte(7).writeLong(Long.MIN_VALUE)
            val param = ParamTypeDecoder.decode(bytes).build(1)
            bytes.clear()
            ParamTypeEncoder.encodeJs5(param, bytes)
            val result = ParamTypeDecoder.decode(bytes).build(1)
            assertEquals(252, result.typeId)
            assertEquals(Long.MIN_VALUE, result.defaultLong)
            bytes.clear()
            val values = mapOf(12 to Long.MAX_VALUE, 42 to "hi", 51 to 123)
            bytes.writeRawParams(values)
            assertEquals(values, bytes.readRawParams())
        }
    }

    @Test
    fun `client script long operands and counts keep the instruction stream aligned`() {
        Unpooled.buffer().use { bytes ->
            bytes.writeByte(0)
            bytes.writeShort(61).writeLong(Long.MAX_VALUE)
            bytes.writeShort(21).writeByte(0)
            bytes.writeInt(2).writeShort(3).writeShort(4).writeShort(5)
            bytes.writeShort(6).writeShort(7).writeShort(8)
            bytes.writeByte(0).writeShort(1)
            val script = ClientScriptTypeDecoder.decode(bytes).build(1)
            assertArrayEquals(intArrayOf(61, 21), script.commands)
            assertArrayEquals(longArrayOf(Long.MAX_VALUE, 0), script.longOperands)
            assertEquals(5, script.longLocalCount)
            assertEquals(8, script.longArgumentCount)
        }
    }

    @Test
    fun `league bank heist row stores table 218 in little endian base 128`() {
        val encoded = byteArrayOf(4, 0xda.toByte(), 1)
        Unpooled.wrappedBuffer(encoded.copyOf()).use { bytes ->
            val row = DbRowTypeDecoder.decode(bytes).build(16118)
            assertEquals(218, row.table)
            bytes.clear()
            DbRowTypeEncoder.encodeJs5(row, bytes)
            assertArrayEquals(
                encoded,
                ByteArray(bytes.readableBytes()).also { bytes.readBytes(it) },
            )
        }
    }

    @Test
    fun `both widget formats retain wide models and following animation fields`() {
        for (modern in listOf(false, true)) {
            val type =
                ComponentTypeBuilder("model")
                    .apply {
                        v3 = modern
                        type = 6
                        model = 90001
                        secondaryModel = 90002
                        modelAnim = 400
                    }
                    .build(5)
            Unpooled.buffer().use { bytes ->
                ComponentTypeEncoder.encode(type, bytes)
                val decoded = ComponentTypeDecoder.decode(5, bytes).build(5)
                assertEquals(90001, decoded.model)
                if (!modern) assertEquals(90002, decoded.secondaryModel)
                assertEquals(400, decoded.modelAnim)
                assertFalse(bytes.isReadable)
            }
        }
    }

    private fun ByteBuf.writeOptions(base: Int) {
        writeByte(base).writeByte(1).writeByte(5).writeString("Sub")
        writeByte(base + 1).writeByte(2)
        writeShort(65535).writeShort(42).writeInt(-1).writeInt(7).writeString("Conditional")
        writeByte(base + 2).writeByte(3).writeShort(500)
        writeShort(41).writeShort(65535).writeInt(4).writeInt(50).writeString("Conditional sub")
    }

    private fun assertOptions(options: List<EntityOption>) {
        assertEquals(
            listOf(
                EntityOption(1, "Sub", 5, null),
                EntityOption(2, "Conditional", null, EntityOption.Condition(65535, 42, -1, 7)),
                EntityOption(3, "Conditional sub", 500, EntityOption.Condition(41, 65535, 4, 50)),
            ),
            options,
        )
    }
}
