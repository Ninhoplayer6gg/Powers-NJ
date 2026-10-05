package com.powersnj.hud;

import com.powersnj.client.ClientPowerState;
import com.powersnj.core.energy.EnergyType;
import com.powersnj.core.net.EnergySnapshot;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.core.net.ProgressSnapshot;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.core.suit.SuitDefinitions;
import net.minecraft.client.Minecraft;

import java.util.Collection;
import java.util.Optional;

/**
 * Client-side, read-only data API for HUD renderers (the built-in {@link PowersHudOverlay} and any
 * future art-driven HUD). All values mirror the server state; nothing here can change gameplay.
 */
public final class HudAPI {

    private HudAPI() {
    }

    /** Immutable view of everything a HUD needs for one frame. */
    public record HudSnapshot(String suit, String character, int level, int maxLevel, long xp, long xpToNext, float xpFraction,
                              int skillPoints, EnergySnapshot energy, EnergySnapshot stamina, String selectedAbility,
                              float selectedCooldown, int selectedCooldownTicks, MovementSnapshot movement, HudProfile profile) {
    }

    public static boolean hasSuit() {
        return !ClientPowerState.activeSuit().isEmpty();
    }

    public static String getSuit() {
        return ClientPowerState.activeSuit();
    }

    public static Optional<SuitDefinition> getSuitDefinition() {
        return SuitDefinitions.CLIENT.get(ClientPowerState.activeSuit());
    }

    public static int getLevel() {
        return ClientPowerState.progress().map(ProgressSnapshot::level).orElse(0);
    }

    public static long getXp() {
        return ClientPowerState.progress().map(ProgressSnapshot::xp).orElse(0L);
    }

    public static float getXpFraction() {
        return ClientPowerState.progress().map(ProgressSnapshot::levelFraction).orElse(0F);
    }

    /** Primary resource of the worn suit. */
    public static Optional<EnergySnapshot> getEnergy() {
        return getSuitDefinition().map(def -> ClientPowerState.energies().get(def.energy().type().id()));
    }

    /** Stamina-type resource of the worn suit (Viltrumite stamina), if any. */
    public static Optional<EnergySnapshot> getStamina() {
        return getEnergy().filter(e -> EnergyType.byId(e.type()).map(EnergyType::isStamina).orElse(false));
    }

    public static Collection<EnergySnapshot> getAllEnergies() {
        return ClientPowerState.energies().values();
    }

    /** Last ability the player activated (Palladium ability reference {@code power#key}). */
    public static String getSelectedAbility() {
        return ClientPowerState.lastAbility();
    }

    /** Cooldown progress (1 = just started, 0 = ready). */
    public static float getCooldown(String abilityKey) {
        return ClientPowerState.cooldowns().progress(abilityKey);
    }

    public static int getCooldownTicks(String abilityKey) {
        return ClientPowerState.cooldowns().remaining(abilityKey);
    }

    public static Optional<MovementSnapshot> getMovement() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? Optional.empty() : ClientPowerState.movement(minecraft.player.getId());
    }

    public static HudProfile getProfile() {
        return getSuitDefinition().map(def -> HudProfile.get(def.hudProfile())).orElse(HudProfile.DEFAULT);
    }

    public static Optional<HudSnapshot> snapshot() {
        if (!hasSuit()) {
            return Optional.empty();
        }
        SuitDefinition definition = getSuitDefinition().orElse(null);
        ProgressSnapshot progress = ClientPowerState.progress().orElse(null);
        String selected = getSelectedAbility();
        return Optional.of(new HudSnapshot(
                getSuit(),
                definition == null ? getSuit() : definition.character(),
                progress == null ? 1 : progress.level(),
                progress == null ? 20 : progress.maxLevel(),
                progress == null ? 0 : progress.xp(),
                progress == null ? 0 : progress.xpToNext(),
                progress == null ? 0F : progress.levelFraction(),
                progress == null ? 0 : progress.availablePoints(),
                getEnergy().orElse(null),
                getStamina().orElse(null),
                selected,
                getCooldown(selected),
                getCooldownTicks(selected),
                getMovement().orElse(MovementSnapshot.NONE),
                getProfile()));
    }
}
