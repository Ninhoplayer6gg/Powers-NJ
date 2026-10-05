package com.powersnj.network;

import com.powersnj.client.ClientPacketHandler;
import com.powersnj.core.net.WireFormat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server &rarr; tracking clients: play a one-shot suit animation on an entity. {@code key} is a clip
 * name of the entity's animation set or a named event ({@code #level_up}...); clients that do not
 * know it ignore it.
 */
public record SuitAnimationPacket(int entityId, String key) {

    public static final int MAX_KEY_LENGTH = 128;

    public static void encode(SuitAnimationPacket packet, FriendlyByteBuf buf) {
        WireFormat.writeVarInt(buf, packet.entityId);
        WireFormat.writeString(buf, packet.key);
    }

    public static SuitAnimationPacket decode(FriendlyByteBuf buf) {
        int entityId = WireFormat.readVarInt(buf);
        String key = WireFormat.readString(buf);
        return new SuitAnimationPacket(entityId, key.length() > MAX_KEY_LENGTH ? "" : key);
    }

    public static void handle(SuitAnimationPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientDispatch.run(() -> () -> ClientPacketHandler.handleSuitAnimation(packet.entityId, packet.key));
    }
}
