package com.powersnj.network;

import com.powersnj.client.ClientPacketHandler;
import com.powersnj.core.net.EnergySnapshot;
import com.powersnj.core.net.WireFormat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server &rarr; client: energy pools that changed noticeably. */
public record EnergyPacket(List<EnergySnapshot> energies) {

    public static void encode(EnergyPacket packet, FriendlyByteBuf buf) {
        WireFormat.writeVarInt(buf, packet.energies.size());
        packet.energies.forEach(e -> e.write(buf));
    }

    public static EnergyPacket decode(FriendlyByteBuf buf) {
        int size = WireFormat.readListSize(buf);
        List<EnergySnapshot> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(EnergySnapshot.read(buf));
        }
        return new EnergyPacket(list);
    }

    public static void handle(EnergyPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientDispatch.run(() -> () -> ClientPacketHandler.handleEnergy(packet.energies));
    }
}
