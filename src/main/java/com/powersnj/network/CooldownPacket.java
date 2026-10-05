package com.powersnj.network;

import com.powersnj.client.ClientPacketHandler;
import com.powersnj.core.net.CooldownSnapshot;
import com.powersnj.core.net.WireFormat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server &rarr; client: cooldowns that started or ended (clients count down locally). */
public record CooldownPacket(List<CooldownSnapshot> cooldowns) {

    public static void encode(CooldownPacket packet, FriendlyByteBuf buf) {
        WireFormat.writeVarInt(buf, packet.cooldowns.size());
        packet.cooldowns.forEach(c -> c.write(buf));
    }

    public static CooldownPacket decode(FriendlyByteBuf buf) {
        int size = WireFormat.readListSize(buf);
        List<CooldownSnapshot> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(CooldownSnapshot.read(buf));
        }
        return new CooldownPacket(list);
    }

    public static void handle(CooldownPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientDispatch.run(() -> () -> ClientPacketHandler.handleCooldowns(packet.cooldowns));
    }
}
