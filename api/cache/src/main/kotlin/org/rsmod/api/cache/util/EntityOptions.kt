package org.rsmod.api.cache.util

import io.netty.buffer.ByteBuf
import org.openrs2.buffer.readString
import org.openrs2.buffer.writeString
import org.rsmod.game.type.util.EntityOption

internal fun ByteBuf.readEntityOption(kind: Int): EntityOption {
    val index = readUnsignedByte().toInt()
    val subOption =
        when (kind) {
            0 -> readUnsignedByte().toInt()
            1 -> null
            2 -> readUnsignedShort()
            else -> error("Invalid option kind: $kind")
        }
    val condition =
        if (kind == 0) null
        else EntityOption.Condition(readUnsignedShort(), readUnsignedShort(), readInt(), readInt())
    return EntityOption(index, readString(), subOption, condition)
}

internal fun ByteBuf.writeEntityOptions(baseCode: Int, options: List<EntityOption>) {
    for ((index, text, subOption, condition) in options) {
        when {
            condition == null -> {
                writeByte(baseCode)
                writeByte(index)
                writeByte(requireNotNull(subOption))
            }
            subOption == null -> {
                writeByte(baseCode + 1)
                writeByte(index)
            }
            else -> {
                writeByte(baseCode + 2)
                writeByte(index)
                writeShort(subOption)
            }
        }
        if (condition != null) {
            writeShort(condition.varp)
            writeShort(condition.varbit)
            writeInt(condition.min)
            writeInt(condition.max)
        }
        writeString(text)
    }
}
