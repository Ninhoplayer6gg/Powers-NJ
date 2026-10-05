package com.powersnj.network;

import com.powersnj.client.ClientPacketHandler;
import com.powersnj.core.net.MovementSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server &rarr; tracking clients: movement controller state of an entity (flight mode, speedster...). */
public record MovementPacket(MovementSnapshot snapshot) {

    public static void encode(MovementPacket packet, FriendlyByteBuf buf) {
        packet.snapshot.write(buf);
    }

    public static MovementPacket decode(FriendlyByteBuf buf) {
        return new MovementPacket(MovementSnapshot.read(buf));
    }

    public static void handle(MovementPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientDispatch.run(() -> () -> ClientPacketHandler.handleMovement(packet.snapshot));
    }
}
