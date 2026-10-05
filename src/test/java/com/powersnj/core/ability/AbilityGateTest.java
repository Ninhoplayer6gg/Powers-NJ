package com.powersnj.core.ability;

import com.powersnj.core.config.PowersSettings;
import com.powersnj.core.data.MapDataNode;
import com.powersnj.core.energy.EnergyPool;
import com.powersnj.core.energy.EnergyType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AbilityGateTest {

    private static final class FakeContext implements AbilityContext {
        boolean suit = true;
        int level = 1;
        Set<String> skills = new HashSet<>();
        CooldownTracker cooldowns = new CooldownTracker();
        EnergyPool pool = new EnergyPool(EnergyType.VILTRUMITE_STAMINA, 100, 0, 0);
        boolean weakness;
        boolean disabled;

        @Override public boolean isSuitActive() { return this.suit; }
        @Override public int suitLevel() { return this.level; }
        @Override public boolean isSkillUnlocked(String skillId) { return this.skills.contains(skillId); }
        @Override public boolean isOnCooldown(String key) { return this.cooldowns.isOnCooldown(key); }
        @Override public boolean hasEnergy(float amount) { return this.pool.has(amount); }
        @Override public boolean isBlockedByWeakness(String key) { return this.weakness; }
        @Override public boolean isDisabledByConfig(String key) { return this.disabled; }
    }

    private static final AbilityCost SLAM = new AbilityCost("powersnj:thragg#ground_slam", "ground_slam", 2, 40, 100);

    @Test
    void validatesInOrder() {
        FakeContext ctx = new FakeContext();
        ctx.suit = false;
        assertEquals(ActivationResult.SUIT_INACTIVE, AbilityGate.check(SLAM, ctx));
        ctx.suit = true;
        ctx.disabled = true;
        assertEquals(ActivationResult.DISABLED_BY_CONFIG, AbilityGate.check(SLAM, ctx));
        ctx.disabled = false;
        ctx.weakness = true;
        assertEquals(ActivationResult.BLOCKED_BY_WEAKNESS, AbilityGate.check(SLAM, ctx));
        ctx.weakness = false;
        assertEquals(ActivationResult.SKILL_LOCKED, AbilityGate.check(SLAM, ctx));
        ctx.skills.add("ground_slam");
        assertEquals(ActivationResult.LEVEL_TOO_LOW, AbilityGate.check(SLAM, ctx));
        ctx.level = 2;
        ctx.pool.drain(80);
        assertEquals(ActivationResult.NOT_ENOUGH_ENERGY, AbilityGate.check(SLAM, ctx));
        ctx.pool.set(100);
        assertEquals(ActivationResult.ALLOWED, AbilityGate.check(SLAM, ctx));
    }

    @Test
    void commitConsumesEnergyAndScalesCooldown() {
        FakeContext ctx = new FakeContext();
        PowersSettings half = new PowersSettings(true, 0.35, 1, 0.6, true, 48, 64, true, 1, 1, 1, 0.5, true, 8, true, true, 3, 2);
        int cooldown = AbilityGate.commit(SLAM, ctx.pool, ctx.cooldowns, half);
        assertEquals(50, cooldown);
        assertEquals(60, ctx.pool.current(), 1e-6);
        assertTrue(ctx.cooldowns.isOnCooldown(SLAM.key()));
        assertEquals(ActivationResult.ON_COOLDOWN, AbilityGate.check(new AbilityCost(SLAM.key(), "", 1, 0, 0), ctx));
        for (int i = 0; i < 50; i++) {
            ctx.cooldowns.tick();
        }
        assertFalse(ctx.cooldowns.isOnCooldown(SLAM.key()));
    }

    @Test
    void commitFailsWithoutEnergy() {
        FakeContext ctx = new FakeContext();
        ctx.pool.drain(100);
        assertEquals(-1, AbilityGate.commit(SLAM, ctx.pool, ctx.cooldowns, PowersSettings.DEFAULTS));
        assertFalse(ctx.cooldowns.isOnCooldown(SLAM.key()), "no cooldown when payment failed");
        assertEquals(-1, AbilityGate.commit(SLAM, null, ctx.cooldowns, PowersSettings.DEFAULTS));
    }

    @Test
    void cooldownTrackerReportsChangesAndPersists() {
        CooldownTracker tracker = new CooldownTracker();
        tracker.start("a", 2);
        tracker.start("b", 10);
        assertEquals(Set.of("a", "b"), tracker.drainDirty());
        assertTrue(tracker.drainDirty().isEmpty(), "nothing changed since last drain");
        tracker.tick();
        assertTrue(tracker.drainDirty().isEmpty(), "plain countdown is not re-sent");
        tracker.tick();
        assertEquals(Set.of("a"), tracker.drainDirty(), "expiry is reported");
        assertEquals(0.8F, tracker.progress("b"), 1e-6);

        MapDataNode node = new MapDataNode();
        tracker.write(node);
        CooldownTracker restored = new CooldownTracker();
        restored.read(node);
        assertEquals(8, restored.remaining("b"));
        assertEquals(10, restored.total("b"));
        assertFalse(restored.isOnCooldown("a"));
    }

    @Test
    void zeroCooldownMultiplierDisablesCooldowns() {
        PowersSettings none = new PowersSettings(true, 0.35, 1, 0.6, true, 48, 64, true, 1, 1, 1, 0, true, 8, true, true, 3, 2);
        assertEquals(0, none.scaleCooldown(200));
        assertEquals(200, PowersSettings.DEFAULTS.scaleCooldown(200));
    }
}
