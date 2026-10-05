package com.powersnj.symbiote;

import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.movement.MotionTracker;
import com.powersnj.movement.MovementController;
import com.powersnj.movement.MovementControllers;
import com.powersnj.player.PowersPlayerData;
import com.powersnj.progression.ProgressionEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Symbiote movement: wall crawling (trait {@code wall_crawling}, granted by a Palladium toggle
 * ability) and tendril swinging. Movement itself is predicted by the client
 * ({@code ClientMovementHandler}); the server owns the state, fall damage and XP.
 */
public final class SymbioteMovementController implements MovementController {

    public static final String TRAIT_WALL_CRAWLING = "wall_crawling";

    private final MotionTracker motion = new MotionTracker();
    private boolean crawling;
    private boolean swinging;

    public SymbioteMovementController(SuitDefinition definition) {
    }

    @Override
    public String id() {
        return MovementControllers.SYMBIOTE;
    }

    @Override
    public void tick(ServerPlayer player, PowersPlayerData data, SuitDefinition definition) {
        double moved = this.motion.update(player.position());
        this.crawling = data.hasTrait(TRAIT_WALL_CRAWLING);
        this.swinging = player.level().getEntity(data.activeTendril()) instanceof TendrilEntity tendril && tendril.isSwingAnchor();
        if ((this.crawling && player.horizontalCollision) || this.swinging) {
            player.fallDistance = 0F;
        }
        if (this.crawling || this.swinging) {
            ProgressionEvents.onTravel(player, data, moved);
        }
    }

    @Override
    public MovementSnapshot snapshot(ServerPlayer player) {
        int flags = 0;
        if (this.crawling) {
            flags |= MovementSnapshot.FLAG_WALL_CRAWLING | MovementSnapshot.FLAG_ACTIVE;
        }
        if (this.swinging) {
            flags |= MovementSnapshot.FLAG_SWINGING | MovementSnapshot.FLAG_ACTIVE;
        }
        return new MovementSnapshot(player.getId(), MovementControllers.SYMBIOTE, this.swinging ? "SWINGING" : this.crawling ? "CRAWLING" : "IDLE", 0F, 0, flags);
    }
}
