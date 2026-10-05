package com.powersnj.network;

import com.powersnj.client.ClientPacketHandler;
import com.powersnj.core.ability.ActivationResult;
import com.powersnj.core.net.WireFormat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server &rarr; client: result of an ability activation (drives the HUD selected/last ability). */
public record AbilityFeedbackPacket(String abilityKey, ActivationResult result) {

    public static void encode(AbilityFeedbackPacket packet, FriendlyByteBuf buf) {
        WireFormat.writeString(buf, packet.abilityKey);
        WireFormat.writeVarInt(buf, packet.result.ordinal());
    }

    public static AbilityFeedbackPacket decode(FriendlyByteBuf buf) {
        String key = WireFormat.readString(buf);
        int ordinal = WireFormat.readVarInt(buf);
        ActivationResult[] values = ActivationResult.values();
        return new AbilityFeedbackPacket(key, ordinal >= 0 && ordinal < values.length ? values[ordinal] : ActivationResult.NO_TARGET);
    }

    public static void handle(AbilityFeedbackPacket packet, Supplier<NetworkEvent.Context> context) {
        ClientDispatch.run(() -> () -> ClientPacketHandler.handleAbilityFeedback(packet.abilityKey, packet.result));
    }
}
