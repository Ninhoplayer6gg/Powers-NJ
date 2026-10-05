package com.powersnj.network;

import com.powersnj.core.net.SkillUnlockRequest;
import com.powersnj.skill.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client &rarr; server: request to unlock a skill. Fully re-validated by {@link SkillService}. */
public record UnlockSkillPacket(SkillUnlockRequest request) {

    public static void encode(UnlockSkillPacket packet, FriendlyByteBuf buf) {
        packet.request.write(buf);
    }

    public static UnlockSkillPacket decode(FriendlyByteBuf buf) {
        return new UnlockSkillPacket(SkillUnlockRequest.read(buf));
    }

    public static void handle(UnlockSkillPacket packet, Supplier<NetworkEvent.Context> context) {
        ServerPlayer sender = context.get().getSender();
        if (sender != null) {
            SkillService.handleUnlockRequest(sender, packet.request.suitId(), packet.request.skillId());
        }
    }
}
