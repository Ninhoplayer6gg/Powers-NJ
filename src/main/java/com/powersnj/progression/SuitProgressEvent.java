package com.powersnj.progression;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Forge events fired on {@code MinecraftForge.EVENT_BUS} so other systems/addons can react to suit
 * progression (achievements, rewards, quests...).
 */
public abstract class SuitProgressEvent extends Event {

    private final ServerPlayer player;
    private final String suitId;

    protected SuitProgressEvent(ServerPlayer player, String suitId) {
        this.player = player;
        this.suitId = suitId;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public String getSuitId() {
        return this.suitId;
    }

    public static final class LevelUp extends SuitProgressEvent {

        private final int oldLevel;
        private final int newLevel;

        public LevelUp(ServerPlayer player, String suitId, int oldLevel, int newLevel) {
            super(player, suitId);
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
        }

        public int getOldLevel() {
            return this.oldLevel;
        }

        public int getNewLevel() {
            return this.newLevel;
        }
    }

    public static final class SkillUnlocked extends SuitProgressEvent {

        private final String skillId;

        public SkillUnlocked(ServerPlayer player, String suitId, String skillId) {
            super(player, suitId);
            this.skillId = skillId;
        }

        public String getSkillId() {
            return this.skillId;
        }
    }
}
