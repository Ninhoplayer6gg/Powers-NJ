package com.powersnj.movement;

import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.player.PowersPlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side movement behaviour attached to the active suit (flight, speedster engine, symbiote
 * crawling...). Created by {@link MovementControllers} from the suit definition's
 * {@code "movement"} block, so any suit can reuse any controller.
 */
public interface MovementController {

    /** Controller id, e.g. {@code powersnj:flight}. */
    String id();

    default void activate(ServerPlayer player, PowersPlayerData data) {
    }

    void tick(ServerPlayer player, PowersPlayerData data, SuitDefinition definition);

    /** Must remove every attribute modifier / state applied to the player. */
    default void deactivate(ServerPlayer player, PowersPlayerData data) {
    }

    MovementSnapshot snapshot(ServerPlayer player);

    default CompoundTag save() {
        return new CompoundTag();
    }

    default void load(CompoundTag tag) {
    }
}
