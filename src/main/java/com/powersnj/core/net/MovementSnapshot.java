package com.powersnj.core.net;

import io.netty.buffer.ByteBuf;

/**
 * Network view of the active movement controller (flight mode, speedster state...).
 *
 * @param entityId     entity the state belongs to (sent to tracking players too)
 * @param controller   movement controller id, e.g. {@code powersnj:speedster}
 * @param mode         controller specific mode name (CRUISE, BOOST, RUNNING...)
 * @param speed        controller specific speed value (flight attribute / speed multiplier)
 * @param level        selected speed level (speedsters), 0 otherwise
 * @param flags        bit 0 active, bit 1 wall running, bit 2 water running, bit 3 wall crawling, bit 4 swinging
 */
public record MovementSnapshot(int entityId, String controller, String mode, float speed, int level, int flags) {

    public static final int FLAG_ACTIVE = 1;
    public static final int FLAG_WALL_RUNNING = 1 << 1;
    public static final int FLAG_WATER_RUNNING = 1 << 2;
    public static final int FLAG_WALL_CRAWLING = 1 << 3;
    public static final int FLAG_SWINGING = 1 << 4;

    public static final MovementSnapshot NONE = new MovementSnapshot(-1, "powersnj:none", "NONE", 0F, 0, 0);

    public boolean has(int flag) {
        return (this.flags & flag) != 0;
    }

    /**
     * Whether the difference to {@code other} is worth a packet (mode/flags/level changed or speed moved by 5%).
     */
    public boolean differsSignificantly(MovementSnapshot other) {
        if (other == null || this.entityId != other.entityId || !this.controller.equals(other.controller) || !this.mode.equals(other.mode)
                || this.level != other.level || this.flags != other.flags) {
            return true;
        }
        float reference = Math.max(0.05F, Math.abs(other.speed));
        return Math.abs(this.speed - other.speed) / reference >= 0.05F;
    }

    public void write(ByteBuf buf) {
        WireFormat.writeVarInt(buf, this.entityId);
        WireFormat.writeString(buf, this.controller);
        WireFormat.writeString(buf, this.mode);
        buf.writeFloat(this.speed);
        WireFormat.writeVarInt(buf, this.level);
        WireFormat.writeVarInt(buf, this.flags);
    }

    public static MovementSnapshot read(ByteBuf buf) {
        return new MovementSnapshot(WireFormat.readVarInt(buf), WireFormat.readString(buf), WireFormat.readString(buf),
                buf.readFloat(), WireFormat.readVarInt(buf), WireFormat.readVarInt(buf));
    }
}
