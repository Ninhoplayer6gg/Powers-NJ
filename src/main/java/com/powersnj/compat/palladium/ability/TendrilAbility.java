package com.powersnj.compat.palladium.ability;

import com.powersnj.player.PowersPlayerData;
import com.powersnj.symbiote.TendrilEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

/**
 * {@code powersnj:tendril} - fires a symbiote tendril ({@link TendrilEntity}).
 * <ul>
 *     <li>{@code grab}: restrains the entity hit;</li>
 *     <li>{@code pull}: pulls the entity to you (or you to heavy targets / blocks);</li>
 *     <li>{@code swing}: anchors to a block; use with {@code palladium:held} - releasing the key
 *     retracts the tendril.</li>
 * </ul>
 */
public class TendrilAbility extends PowersAbility {

    public static final PalladiumProperty<String> MODE = new StringProperty("mode").configurable("grab, pull or swing.");
    public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Maximum tendril range.");
    public static final PalladiumProperty<Float> DAMAGE = new FloatProperty("damage").configurable("Hit damage (PvE value).");
    public static final PalladiumProperty<Integer> HOLD_TICKS = new IntegerProperty("hold_ticks").configurable("How long a grab/pull holds.");

    public TendrilAbility() {
        this.withProperty(MODE, "grab");
        this.withProperty(RANGE, 20F);
        this.withProperty(DAMAGE, 4F);
        this.withProperty(HOLD_TICKS, 60);
    }

    @Override
    protected boolean retriesWhileEnabled() {
        return false;
    }

    @Override
    public boolean onActivated(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        Entity existing = player.level().getEntity(data.activeTendril());
        if (existing instanceof TendrilEntity old && old.isAlive()) {
            old.retract();
        }
        TendrilEntity tendril = TendrilEntity.launch(player, TendrilEntity.Mode.byName(instance.getProperty(MODE)),
                instance.getProperty(RANGE), instance.getProperty(DAMAGE), instance.getProperty(HOLD_TICKS));
        data.setActiveTendril(tendril.getId());
        return true;
    }

    @Override
    protected void onDisabled(ServerPlayer player, AbilityInstance instance, PowersPlayerData data) {
        if (TendrilEntity.Mode.byName(instance.getProperty(MODE)) == TendrilEntity.Mode.SWING
                && player.level().getEntity(data.activeTendril()) instanceof TendrilEntity tendril) {
            tendril.retract();
            data.setActiveTendril(-1);
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Powers NJ: symbiote tendril (grab / pull / swing).";
    }
}
