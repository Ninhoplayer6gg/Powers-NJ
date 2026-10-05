package com.powersnj.skill;

import com.powersnj.core.skill.SkillUnlockResult;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.progression.ProgressionAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server handler of skill unlock requests sent by the skill tree screen. The client request is
 * only a wish: the suit must be worn and every tree rule is re-validated by
 * {@link ProgressionAPI#unlockSkill}.
 */
public final class SkillService {

    private SkillService() {
    }

    public static void handleUnlockRequest(ServerPlayer player, String suitId, String skillId) {
        PowersPlayerData data = PowersPlayerData.get(player).orElse(null);
        if (data == null) {
            return;
        }
        if (!suitId.equals(data.activeSuit())) {
            player.displayClientMessage(Component.translatable("message.powersnj.skill.suit_not_worn").withStyle(ChatFormatting.RED), true);
            return;
        }
        SkillUnlockResult result = ProgressionAPI.unlockSkill(player, suitId, skillId);
        if (result.isSuccess()) {
            player.displayClientMessage(Component.translatable("message.powersnj.skill.unlocked",
                    Component.translatable("skill." + suitId.replace(':', '.') + "." + skillId)).withStyle(ChatFormatting.GREEN), true);
        } else {
            player.displayClientMessage(Component.translatable(result.translationKey()).withStyle(ChatFormatting.RED), true);
        }
    }
}
