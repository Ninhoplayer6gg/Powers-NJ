package com.powersnj.network;

import com.powersnj.PowersNJ;
import com.powersnj.core.net.DefinitionsPayload;
import com.powersnj.core.suit.SuitDefinitions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Powers NJ network channel. Payload encoding lives in the engine-agnostic
 * {@code com.powersnj.core.net} records (unit tested); packet classes here only route them.
 * <p>
 * Bandwidth rules: full state only on login/respawn/dimension/suit change; energy only when it
 * moved by &ge; 1 point; cooldowns only on start/end; movement only on significant change.
 */
public final class PowersNetwork {

    public static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(PowersNJ.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean registered;

    private PowersNetwork() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.messageBuilder(PowerStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PowerStatePacket::encode).decoder(PowerStatePacket::decode).consumerMainThread(PowerStatePacket::handle).add();
        CHANNEL.messageBuilder(EnergyPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EnergyPacket::encode).decoder(EnergyPacket::decode).consumerMainThread(EnergyPacket::handle).add();
        CHANNEL.messageBuilder(CooldownPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CooldownPacket::encode).decoder(CooldownPacket::decode).consumerMainThread(CooldownPacket::handle).add();
        CHANNEL.messageBuilder(ProgressPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ProgressPacket::encode).decoder(ProgressPacket::decode).consumerMainThread(ProgressPacket::handle).add();
        CHANNEL.messageBuilder(MovementPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MovementPacket::encode).decoder(MovementPacket::decode).consumerMainThread(MovementPacket::handle).add();
        CHANNEL.messageBuilder(FormPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FormPacket::encode).decoder(FormPacket::decode).consumerMainThread(FormPacket::handle).add();
        CHANNEL.messageBuilder(DefinitionsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DefinitionsPacket::encode).decoder(DefinitionsPacket::decode).consumerMainThread(DefinitionsPacket::handle).add();
        CHANNEL.messageBuilder(AbilityFeedbackPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AbilityFeedbackPacket::encode).decoder(AbilityFeedbackPacket::decode).consumerMainThread(AbilityFeedbackPacket::handle).add();
        CHANNEL.messageBuilder(SuitAnimationPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SuitAnimationPacket::encode).decoder(SuitAnimationPacket::decode).consumerMainThread(SuitAnimationPacket::handle).add();
        CHANNEL.messageBuilder(UnlockSkillPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UnlockSkillPacket::encode).decoder(UnlockSkillPacket::decode).consumerMainThread(UnlockSkillPacket::handle).add();
        PowersNJ.LOGGER.debug("Registered {} Powers NJ packets", id);
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToTrackingAndSelf(Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendDefinitions(ServerPlayer player) {
        sendTo(player, new DefinitionsPacket(new DefinitionsPayload(SuitDefinitions.SERVER.rawJson())));
    }

    public static void broadcastDefinitions() {
        sendToAll(new DefinitionsPacket(new DefinitionsPayload(SuitDefinitions.SERVER.rawJson())));
    }
}
