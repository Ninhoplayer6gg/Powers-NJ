package com.powersnj.progression;

import com.powersnj.core.config.Settings;
import com.powersnj.core.net.ProgressSnapshot;
import com.powersnj.core.progression.LevelCurve;
import com.powersnj.core.progression.SuitProgress;
import com.powersnj.core.progression.XpGain;
import com.powersnj.core.progression.XpRules;
import com.powersnj.core.skill.SkillTree;
import com.powersnj.core.skill.SkillUnlockResult;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.core.suit.SuitDefinitions;
import com.powersnj.network.PowersNetwork;
import com.powersnj.network.ProgressPacket;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

/**
 * Public, server-authoritative progression API (one progression per player per suit, levels 1-20,
 * persisted in the player capability).
 * <ul>
 *     <li>{@link #addSuitXp(ServerPlayer, String, double)}</li>
 *     <li>{@link #getSuitLevel(Player, String)}</li>
 *     <li>{@link #unlockSkill(ServerPlayer, String, String)}</li>
 *     <li>{@link #isSkillUnlocked(Player, String, String)}</li>
 * </ul>
 * Mutating methods only accept {@link ServerPlayer}: the client can never change progression.
 */
public final class ProgressionAPI {

    private ProgressionAPI() {
    }

    /**
     * Adds raw XP (scaled by the server {@code xpMultiplier}) to a suit.
     */
    public static XpGain addSuitXp(ServerPlayer player, String suitId, double rawXp) {
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        SuitDefinition definition = SuitDefinitions.SERVER.get(suitId).orElse(null);
        if (data == null || definition == null) {
            return XpGain.none(1);
        }
        long amount = XpRules.scaled(rawXp, Settings.get().xpMultiplier());
        XpGain gain = data.ledger().addSuitXp(suitId, amount, definition.curve());
        if (gain.leveledUp()) {
            onLevelUp(player, data, definition, gain);
        }
        return gain;
    }

    public static int getSuitLevel(Player player, String suitId) {
        return PowersPlayerData.get(player).map(data -> data.ledger().getSuitLevel(suitId)).orElse(1);
    }

    public static SkillUnlockResult unlockSkill(ServerPlayer player, String suitId, String skillId) {
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        SuitDefinition definition = SuitDefinitions.SERVER.get(suitId).orElse(null);
        if (data == null || definition == null) {
            return SkillUnlockResult.UNKNOWN_SKILL;
        }
        SkillTree tree = definition.skillTree();
        SkillUnlockResult result = data.ledger().unlockSkill(suitId, skillId, tree);
        if (result.isSuccess()) {
            player.level().playSound(null, player.blockPosition(), ModSounds.SKILL_UNLOCK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            syncProgress(player, data, suitId, false);
            MinecraftForge.EVENT_BUS.post(new SuitProgressEvent.SkillUnlocked(player, suitId, skillId));
        }
        return result;
    }

    public static boolean isSkillUnlocked(Player player, String suitId, String skillId) {
        return PowersPlayerData.get(player).map(data -> data.ledger().isSkillUnlocked(suitId, skillId)).orElse(false);
    }

    /**
     * Admin helper used by {@code /powersnj level}.
     */
    public static void setSuitLevel(ServerPlayer player, String suitId, int level) {
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        SuitDefinition definition = SuitDefinitions.SERVER.get(suitId).orElse(null);
        if (data == null || definition == null) {
            return;
        }
        SuitProgress progress = data.ledger().progress(suitId);
        int old = progress.level();
        progress.setLevel(level, definition.curve());
        data.energy().configure(definition.energy(), progress.level());
        syncProgress(player, data, suitId, progress.level() > old);
    }

    public static void incrementStat(ServerPlayer player, String suitId, String stat, long amount) {
        PowersPlayerData.get(player).ifPresent(data -> data.ledger().progress(suitId).incrementStat(stat, amount));
    }

    private static void onLevelUp(ServerPlayer player, PowersPlayerData data, SuitDefinition definition, XpGain gain) {
        data.energy().configure(definition.energy(), gain.newLevel());
        player.level().playSound(null, player.blockPosition(), ModSounds.LEVEL_UP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.powersnj.level_up",
                Component.translatable("suit." + definition.id().replace(':', '.')), gain.newLevel()).withStyle(ChatFormatting.GOLD), true);
        syncProgress(player, data, definition.id(), true);
        MinecraftForge.EVENT_BUS.post(new SuitProgressEvent.LevelUp(player, definition.id(), gain.oldLevel(), gain.newLevel()));
    }

    /**
     * Sends the progression of {@code suitId} if it is the active suit.
     */
    public static void syncProgress(ServerPlayer player, PowersPlayerData data, String suitId, boolean levelUp) {
        if (!suitId.equals(data.activeSuit())) {
            return;
        }
        LevelCurve curve = SuitDefinitions.SERVER.curveOf(suitId);
        SuitProgress progress = data.ledger().progress(suitId);
        PowersNetwork.sendTo(player, new ProgressPacket(ProgressSnapshot.of(progress, curve), levelUp));
        progress.markClean();
    }
}
