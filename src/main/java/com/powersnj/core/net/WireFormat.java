package com.powersnj.core.net;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Low level wire helpers that are byte-compatible with Minecraft's {@code FriendlyByteBuf}
 * (VarInt length prefixed UTF-8 strings, LEB128 VarInts). Because {@code FriendlyByteBuf}
 * extends Netty's {@link ByteBuf}, every payload codec in {@code com.powersnj.core.net} writes
 * straight into the real packet buffer in game and into an {@code Unpooled} buffer in tests.
 */
public final class WireFormat {

    public static final int MAX_STRING_BYTES = 32767;
    public static final int MAX_LIST_SIZE = 4096;

    private WireFormat() {
    }

    public static void writeVarInt(ByteBuf buf, int value) {
        while ((value & -128) != 0) {
            buf.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    public static int readVarInt(ByteBuf buf) {
        int result = 0;
        int shift = 0;
        byte current;
        do {
            current = buf.readByte();
            result |= (current & 127) << shift;
            shift += 7;
            if (shift > 35) {
                throw new IllegalArgumentException("VarInt too big");
            }
        } while ((current & 128) == 128);
        return result;
    }

    public static void writeString(ByteBuf buf, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) {
            throw new IllegalArgumentException("String too long: " + bytes.length + " bytes");
        }
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public static String readString(ByteBuf buf) {
        int length = readVarInt(buf);
        if (length < 0 || length > MAX_STRING_BYTES) {
            throw new IllegalArgumentException("Invalid string length " + length);
        }
        if (length > buf.readableBytes()) {
            throw new IllegalArgumentException("String length " + length + " exceeds readable bytes " + buf.readableBytes());
        }
        String value = buf.toString(buf.readerIndex(), length, StandardCharsets.UTF_8);
        buf.skipBytes(length);
        return value;
    }

    public static void writeStringList(ByteBuf buf, Collection<String> values) {
        writeVarInt(buf, values.size());
        for (String value : values) {
            writeString(buf, value);
        }
    }

    public static List<String> readStringList(ByteBuf buf) {
        int size = readListSize(buf);
        List<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(readString(buf));
        }
        return result;
    }

    public static int readListSize(ByteBuf buf) {
        int size = readVarInt(buf);
        if (size < 0 || size > MAX_LIST_SIZE) {
            throw new IllegalArgumentException("Invalid list size " + size);
        }
        return size;
    }
}
