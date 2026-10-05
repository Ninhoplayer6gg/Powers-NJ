package com.powersnj.network;

import com.powersnj.client.ClientPacketHandler;
import com.powersnj.core.net.PowerStateSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server &rarr; client: complete power state of the local player. */
public record PowerStatePacket(PowerStateSnapshot snapshot) {

    public static void encode(PowerStatePacket packet, FriendlyByteBuf buf) {
        packet.snapshot.write(buf);
    }

    public static PowerStatePacket decode(FriendlyByteBuf buf) {
        return new PowerStatePacket(PowerStateSnapshot.read(buf));
    }

    public static void handle(PowerStatePacket packet, Supplier<NetworkEvent.Context> context) {
        ClientDispatch.run(() -> () -> ClientPacketHandler.handlePowerState(packet.snapshot));
    }
}
