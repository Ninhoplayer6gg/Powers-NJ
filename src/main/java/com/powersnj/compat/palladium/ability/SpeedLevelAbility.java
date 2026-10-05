package com.powersnj.compat.palladium.ability;

import com.powersnj.player.PowersPlayerData;
import com.powersnj.speedster.SpeedsterMovementController;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * {@code powersnj:speed_level} - raises/lowers the selected speed level (bind to scroll up/down).
 */
public class SpeedLevelAbility extends PowersActionAbility {

    public static final PalladiumProperty<Integer> DELTA = new IntegerProperty("delta").configurable("Speed level change (+1 / -1).");

    public SpeedLevelAbility() {
        this.withProperty(DELTA, 1);
        this.withProperty(XP_REWARD, 0F);
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        if (!(data.movement() instanceof SpeedsterMovementController speedster)) {
            return false;
        }
        int before = speedster.engine().speedLevel();
        int after = speedster.engine().adjustSpeedLevel(instance.getProperty(DELTA));
        if (before == after) {
            return false;
        }
        player.displayClientMessage(Component.translatable("message.powersnj.speedster.level", after, speedster.engine().profile().speedLevels())
                .withStyle(ChatFormatting.RED), true);
        return true;
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: change speedster speed level.";
    }
}
