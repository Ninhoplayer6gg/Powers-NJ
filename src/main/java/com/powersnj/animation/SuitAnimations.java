package com.powersnj.animation;

import com.powersnj.PowersNJ;
import com.powersnj.client.ClientPowerState;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.movement.MovementControllers;
import com.powersnj.suit.SuitArmorItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

/**
 * Animation names and controllers shared by every suit. Animation files live at
 * {@code assets/powersnj/animations/suits/<suit>.animation.json} and must define the animations
 * named here (see ASSET_REQUIREMENTS.md). When a suit has no animation file yet the bundled empty
 * {@link #EMPTY_ANIMATIONS} is used, so GeckoLib never fails.
 * <p>
 * The controller picks the animation from the server-synced movement state of the entity wearing
 * the suit: flying &rarr; {@link #FLY}, speedster running &rarr; {@link #RUN}, otherwise {@link #IDLE}.
 */
public final class SuitAnimations {

    public static final ResourceLocation EMPTY_ANIMATIONS = PowersNJ.id("animations/empty.animation.json");

    public static final String IDLE = "animation.suit.idle";
    public static final String FLY = "animation.suit.fly";
    public static final String RUN = "animation.suit.run";

    public static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop(IDLE);
    public static final RawAnimation FLY_ANIM = RawAnimation.begin().thenLoop(FLY);
    public static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop(RUN);

    private SuitAnimations() {
    }

    public static AnimationController<SuitArmorItem> suitController(SuitArmorItem item) {
        return new AnimationController<>(item, "suit", 5, state -> {
            Entity wearer = state.getData(DataTickets.ENTITY);
            MovementSnapshot movement = wearer == null ? null : ClientPowerState.movement(wearer.getId()).orElse(null);
            if (movement != null && movement.has(MovementSnapshot.FLAG_ACTIVE)) {
                if (MovementControllers.FLIGHT.equals(movement.controller())) {
                    return state.setAndContinue(FLY_ANIM);
                }
                if (MovementControllers.SPEEDSTER.equals(movement.controller()) && movement.speed() > 2F) {
                    return state.setAndContinue(RUN_ANIM);
                }
            }
            return state.setAndContinue(IDLE_ANIM);
        });
    }
}
