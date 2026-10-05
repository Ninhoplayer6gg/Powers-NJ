package com.powersnj.network;

import com.powersnj.client.ClientPacketHandler;
import com.powersnj.core.net.DefinitionsPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server &rarr; client: every suit definition (login and /reload). */
public record DefinitionsPacket(DefinitionsPayload payload) {

    public static void encode(DefinitionsPacket packet, FriendlyByteBuf buf) {
        packet.payload.write(buf);
    }

    public static DefinitionsPacket decode(FriendlyByteBuf buf) {
        return new DefinitionsPacket(DefinitionsPayload.read(buf));
    }

    public static void handle(DefinitionsPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientDispatch.run(() -> () -> ClientPacketHandler.handleDefinitions(packet.payload));
    }
}
