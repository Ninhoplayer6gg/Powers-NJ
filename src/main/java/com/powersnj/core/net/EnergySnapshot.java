package com.powersnj.core.net;

import com.powersnj.core.energy.EnergyPool;
import io.netty.buffer.ByteBuf;

/**
 * Network view of one energy pool.
 */
public record EnergySnapshot(String type, float current, float max) {

    public static EnergySnapshot of(EnergyPool pool) {
        return new EnergySnapshot(pool.type().id(), pool.current(), pool.max());
    }

    public float fraction() {
        return this.max <= 0 ? 0 : Math.max(0, Math.min(1, this.current / this.max));
    }

    public void write(ByteBuf buf) {
        WireFormat.writeString(buf, this.type);
        buf.writeFloat(this.current);
        buf.writeFloat(this.max);
    }

    public static EnergySnapshot read(ByteBuf buf) {
        return new EnergySnapshot(WireFormat.readString(buf), buf.readFloat(), buf.readFloat());
    }
}
