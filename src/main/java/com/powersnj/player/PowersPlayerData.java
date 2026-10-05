package com.powersnj.player;

import com.powersnj.PowersNJ;
import com.powersnj.core.ability.CooldownTracker;
import com.powersnj.core.energy.EnergyBank;
import com.powersnj.core.energy.EnergyPool;
import com.powersnj.core.net.MovementSnapshot;
import com.powersnj.core.progression.ProgressionLedger;
import com.powersnj.core.progression.SuitProgress;
import com.powersnj.core.suit.SuitDefinition;
import com.powersnj.core.suit.SuitDefinitions;
import com.powersnj.movement.MovementController;
import com.powersnj.network.NbtDataNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Everything Powers NJ stores on a player: suit progressions, energy pools, cooldowns, movement
 * controller state and visual form. Server authoritative; the client keeps a mirror in
 * {@code ClientPowerState}. Saved with the player (survives logout, death and restarts).
 */
public final class PowersPlayerData {

    public static final Capability<PowersPlayerData> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    private final ProgressionLedger ledger = new ProgressionLedger();
    private final EnergyBank energy = new EnergyBank();
    private final CooldownTracker cooldowns = new CooldownTracker();
    private CompoundTag movementStates = new CompoundTag();
    private String form = "";

    // Runtime (not saved)
    private String activeSuit = "";
    @Nullable
    private MovementController movement;
    private MovementSnapshot lastSentMovement;
    private boolean fullSyncRequested = true;
    private boolean formDirty;
    private boolean shieldActive;
    private double travelAccumulator;
    private double pendingXp;
    private final Set<String> traits = new HashSet<>();
    private final Set<String> activeAbilities = new HashSet<>();
    private String lastAbility = "";
    private int lastPhaseTick = -1000;
    private int activeTendril = -1;

    public static Optional<PowersPlayerData> get(Player player) {
        return player.getCapability(CAPABILITY).resolve();
    }

    public ProgressionLedger ledger() {
        return this.ledger;
    }

    public EnergyBank energy() {
        return this.energy;
    }

    public CooldownTracker cooldowns() {
        return this.cooldowns;
    }

    public String activeSuit() {
        return this.activeSuit;
    }

    public boolean hasActiveSuit() {
        return !this.activeSuit.isEmpty();
    }

    public void setActiveSuit(String suitId) {
        this.activeSuit = suitId == null ? "" : suitId;
    }

    public Optional<SuitDefinition> activeDefinition(boolean clientSide) {
        return this.hasActiveSuit() ? SuitDefinitions.side(clientSide).get(this.activeSuit) : Optional.empty();
    }

    /**
     * Progress of the active suit, or empty when no suit is worn.
     */
    public Optional<SuitProgress> activeProgress() {
        return this.hasActiveSuit() ? Optional.of(this.ledger.progress(this.activeSuit)) : Optional.empty();
    }

    /**
     * Primary energy pool of the active suit.
     */
    public Optional<EnergyPool> activeEnergy(boolean clientSide) {
        return this.activeDefinition(clientSide).flatMap(def -> this.energy.get(def.energy().type()));
    }

    public int activeLevel() {
        return this.hasActiveSuit() ? this.ledger.getSuitLevel(this.activeSuit) : 0;
    }

    public @Nullable MovementController movement() {
        return this.movement;
    }

    public void setMovement(@Nullable MovementController movement) {
        this.movement = movement;
    }

    public CompoundTag movementState(String controllerId) {
        return this.movementStates.getCompound(controllerId);
    }

    public void storeMovementState(String controllerId, CompoundTag tag) {
        this.movementStates.put(controllerId, tag);
    }

    public String form() {
        return this.form;
    }

    public void setForm(String form) {
        String value = form == null ? "" : form;
        if (!value.equals(this.form)) {
            this.form = value;
            this.formDirty = true;
        }
    }

    public boolean consumeFormDirty() {
        boolean dirty = this.formDirty;
        this.formDirty = false;
        return dirty;
    }

    public boolean isShieldActive() {
        return this.shieldActive;
    }

    public void setShieldActive(boolean shieldActive) {
        this.shieldActive = shieldActive;
    }

    /**
     * Traits granted this tick by enabled {@code powersnj:trait} Palladium abilities
     * (e.g. {@code wall_running}, {@code water_running}, {@code wall_crawling}). Cleared at the
     * start of every player tick and refilled while Palladium ticks abilities.
     */
    public Set<String> traits() {
        return this.traits;
    }

    public boolean hasTrait(String trait) {
        return this.traits.contains(trait);
    }

    /**
     * Entity id of the tendril currently controlled by a held ability, -1 when none.
     */
    public int activeTendril() {
        return this.activeTendril;
    }

    public void setActiveTendril(int entityId) {
        this.activeTendril = entityId;
    }

    /**
     * Toggle/held abilities that passed the server gate and are currently running.
     */
    public Set<String> activeAbilities() {
        return this.activeAbilities;
    }

    public String lastAbility() {
        return this.lastAbility;
    }

    public void setLastAbility(String key) {
        this.lastAbility = key == null ? "" : key;
    }

    public int lastPhaseTick() {
        return this.lastPhaseTick;
    }

    public void setLastPhaseTick(int tick) {
        this.lastPhaseTick = tick;
    }

    public MovementSnapshot lastSentMovement() {
        return this.lastSentMovement;
    }

    public void setLastSentMovement(MovementSnapshot snapshot) {
        this.lastSentMovement = snapshot;
    }

    public void requestFullSync() {
        this.fullSyncRequested = true;
    }

    public boolean consumeFullSyncRequest() {
        boolean requested = this.fullSyncRequested;
        this.fullSyncRequested = false;
        return requested;
    }

    /**
     * Accumulates travelled distance; returns whole hundreds of blocks reached (for travel XP).
     */
    public int addTravel(double blocks) {
        this.travelAccumulator += blocks;
        int hundreds = (int) (this.travelAccumulator / 100D);
        this.travelAccumulator -= hundreds * 100D;
        return hundreds;
    }

    /**
     * Accumulates fractional XP (e.g. 0.15 XP per damage point) and returns the whole part to grant.
     */
    public long addPendingXp(double xp) {
        this.pendingXp += Math.max(0D, xp);
        long whole = (long) this.pendingXp;
        this.pendingXp -= whole;
        return whole;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", 1);
        CompoundTag progression = new CompoundTag();
        this.ledger.write(new NbtDataNode(progression));
        tag.put("progression", progression);
        CompoundTag energyTag = new CompoundTag();
        this.energy.write(new NbtDataNode(energyTag));
        tag.put("energy", energyTag);
        CompoundTag cooldownTag = new CompoundTag();
        this.cooldowns.write(new NbtDataNode(cooldownTag));
        tag.put("cooldowns", cooldownTag);
        if (this.movement != null) {
            this.storeMovementState(this.movement.id(), this.movement.save());
        }
        tag.put("movement", this.movementStates.copy());
        tag.putString("form", this.form);
        return tag;
    }

    public void load(CompoundTag tag) {
        try {
            this.ledger.read(new NbtDataNode(tag.getCompound("progression")), SuitDefinitions.SERVER::curveOf);
            this.energy.read(new NbtDataNode(tag.getCompound("energy")));
            this.cooldowns.read(new NbtDataNode(tag.getCompound("cooldowns")));
            this.movementStates = tag.getCompound("movement").copy();
            this.form = tag.getString("form");
        } catch (RuntimeException e) {
            PowersNJ.LOGGER.error("Failed to load Powers NJ player data, keeping defaults", e);
        }
        this.fullSyncRequested = true;
    }

    /**
     * Copies persistent state to a new player instance (respawn / dimension change).
     *
     * @param death true when the player died: energy refills and cooldowns reset, progression is kept
     */
    public void copyFrom(PowersPlayerData other, boolean death) {
        this.load(other.save());
        if (death) {
            this.cooldowns.clearAll();
            this.energy.pools().forEach(pool -> pool.set(pool.max()));
            this.form = "";
        }
    }
}
