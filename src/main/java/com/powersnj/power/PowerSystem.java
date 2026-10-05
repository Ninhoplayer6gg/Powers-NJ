package com.powersnj.power;

import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Always-on special system enabled per suit through the definition's {@code "systems"} list
 * (regeneration, weaknesses, reactions...). Systems are stateless singletons; per-player state
 * lives in {@link PowersPlayerData}.
 */
public interface PowerSystem {

    String id();

    void tick(ServerPlayer player, PowersPlayerData data, SuitDefinition definition);
}
