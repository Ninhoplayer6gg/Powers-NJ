package com.powersnj.network;

import com.powersnj.client.ClientPacketHandler;
import com.powersnj.core.net.WireFormat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server &rarr; tracking clients: visual form of an entity (e.g. symbiote partial transformation). */
public record FormPacket(int entityId, String form) {

    public static void encode(FormPacket packet, FriendlyByteBuf buf) {
        WireFormat.writeVarInt(buf, packet.entityId);
        WireFormat.writeString(buf, packet.form);
    }

    public static FormPacket decode(FriendlyByteBuf buf) {
        return new FormPacket(WireFormat.readVarInt(buf), WireFormat.readString(buf));
    }

    public static void handle(FormPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientDispatch.run(() -> () -> ClientPacketHandler.handleForm(packet.entityId, packet.form));
    }
}
