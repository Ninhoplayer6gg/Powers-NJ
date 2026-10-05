package com.powersnj.network;

import com.powersnj.client.ClientPacketHandler;
import com.powersnj.core.net.ProgressSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server &rarr; client: progression of the active suit changed (XP, level, skills). */
public record ProgressPacket(ProgressSnapshot progress, boolean levelUp) {

    public static void encode(ProgressPacket packet, FriendlyByteBuf buf) {
        packet.progress.write(buf);
        buf.writeBoolean(packet.levelUp);
    }

    public static ProgressPacket decode(FriendlyByteBuf buf) {
        return new ProgressPacket(ProgressSnapshot.read(buf), buf.readBoolean());
    }

    public static void handle(ProgressPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientDispatch.run(() -> () -> ClientPacketHandler.handleProgress(packet.progress, packet.levelUp));
    }
}
