package com.powersnj.core.net;

import io.netty.buffer.ByteBuf;

/**
 * Network view of one ability cooldown. {@code remaining == 0} means the cooldown ended.
 */
public record CooldownSnapshot(String key, int remaining, int total) {

    public void write(ByteBuf buf) {
        WireFormat.writeString(buf, this.key);
        WireFormat.writeVarInt(buf, Math.max(0, this.remaining));
        WireFormat.writeVarInt(buf, Math.max(0, this.total));
    }

    public static CooldownSnapshot read(ByteBuf buf) {
        return new CooldownSnapshot(WireFormat.readString(buf), WireFormat.readVarInt(buf), WireFormat.readVarInt(buf));
    }
}
