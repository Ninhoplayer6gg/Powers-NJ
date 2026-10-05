package com.powersnj.symbiote;

import com.powersnj.combat.CombatService;
import com.powersnj.registry.ModDamageTypes;
import com.powersnj.registry.ModEntities;
import com.powersnj.registry.ModParticles;
import com.powersnj.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;

/**
 * Symbiote tendril: a server-simulated tip travelling from its owner, able to grab, pull or anchor
 * for swinging. All decisions (hits, damage, pulling) happen on the server; clients only render
 * the owner-to-tip ribbon (placeholder) or the GeckoLib model once {@code geo/entity/tendril.geo.json}
 * exists. Never saved to disk.
 */
public class TendrilEntity extends Entity implements GeoEntity {

    public enum Mode {
        GRAB, PULL, SWING;

        public static Mode byName(String name) {
            for (Mode mode : values()) {
                if (mode.name().equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return GRAB;
        }
    }

    public enum Phase {
        EXTENDING, ATTACHED, RETRACTING
    }

    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(TendrilEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(TendrilEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(TendrilEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(TendrilEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Optional<BlockPos>> ANCHOR = SynchedEntityData.defineId(TendrilEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Float> ROPE_LENGTH = SynchedEntityData.defineId(TendrilEntity.class, EntityDataSerializers.FLOAT);

    public static final RawAnimation EXTEND_ANIM = RawAnimation.begin().thenPlay("animation.tendril.extend");
    public static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.tendril.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Server-side configuration
    private Vec3 direction = Vec3.ZERO;
    private double range = 24D;
    private double speed = 2.0D;
    private float damage = 4F;
    private int holdTicks = 60;
    private int attachedTicks;
    private int age;

    public TendrilEntity(EntityType<? extends TendrilEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static TendrilEntity launch(LivingEntity owner, Mode mode, double range, float damage, int holdTicks) {
        TendrilEntity tendril = new TendrilEntity(ModEntities.TENDRIL.get(), owner.level());
        Vec3 start = owner.getEyePosition().add(0, -0.3D, 0);
        tendril.setPos(start.x, start.y, start.z);
        tendril.entityData.set(OWNER, owner.getId());
        tendril.entityData.set(MODE, (byte) mode.ordinal());
        tendril.direction = owner.getLookAngle().normalize();
        tendril.range = range;
        tendril.damage = damage;
        tendril.holdTicks = holdTicks;
        owner.level().addFreshEntity(tendril);
        owner.level().playSound(null, owner.blockPosition(), ModSounds.TENDRIL_SHOOT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return tendril;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER, -1);
        this.entityData.define(TARGET, -1);
        this.entityData.define(MODE, (byte) 0);
        this.entityData.define(PHASE, (byte) 0);
        this.entityData.define(ANCHOR, Optional.empty());
        this.entityData.define(ROPE_LENGTH, 0F);
    }

    public @Nullable Entity getOwner() {
        return this.level().getEntity(this.entityData.get(OWNER));
    }

    public int getOwnerId() {
        return this.entityData.get(OWNER);
    }

    public @Nullable Entity getTarget() {
        int id = this.entityData.get(TARGET);
        return id < 0 ? null : this.level().getEntity(id);
    }

    public Mode getMode() {
        return Mode.values()[Math.max(0, Math.min(Mode.values().length - 1, this.entityData.get(MODE)))];
    }

    public Phase getPhase() {
        return Phase.values()[Math.max(0, Math.min(Phase.values().length - 1, this.entityData.get(PHASE)))];
    }

    public Optional<BlockPos> getAnchor() {
        return this.entityData.get(ANCHOR);
    }

    public float getRopeLength() {
        return this.entityData.get(ROPE_LENGTH);
    }

    public boolean isSwingAnchor() {
        return this.getMode() == Mode.SWING && this.getPhase() == Phase.ATTACHED && this.getAnchor().isPresent();
    }

    private void setPhase(Phase phase) {
        this.entityData.set(PHASE, (byte) phase.ordinal());
    }

    /**
     * Starts retracting (ability released, target lost...).
     */
    public void retract() {
        if (this.getPhase() != Phase.RETRACTING) {
            this.setPhase(Phase.RETRACTING);
            this.entityData.set(TARGET, -1);
            this.level().playSound(null, this.blockPosition(), ModSounds.TENDRIL_RETRACT.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        this.age++;
        Entity ownerEntity = this.getOwner();
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive() || owner.level() != this.level() || this.age > 400
                || owner.distanceTo(this) > this.range * 1.6D) {
            this.discard();
            return;
        }
        switch (this.getPhase()) {
            case EXTENDING -> this.tickExtending(owner);
            case ATTACHED -> this.tickAttached(owner);
            case RETRACTING -> this.tickRetracting(owner);
        }
    }

    private void tickExtending(LivingEntity owner) {
        Vec3 from = this.position();
        Vec3 to = from.add(this.direction.scale(this.speed));
        BlockHitResult blockHit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            to = blockHit.getLocation();
        }
        AABB sweep = this.getBoundingBox().expandTowards(to.subtract(from)).inflate(0.5D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(this.level(), this, from, to, sweep,
                e -> e instanceof LivingEntity && e != owner && e.isAlive() && !e.isSpectator());
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
            this.setPos(entityHit.getLocation());
            this.attachToEntity(owner, target);
            return;
        }
        this.setPos(to.x, to.y, to.z);
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            this.attachToBlock(owner, blockHit.getBlockPos());
            return;
        }
        if (owner.distanceToSqr(this) >= this.range * this.range) {
            this.retract();
        }
    }

    private void attachToEntity(LivingEntity owner, LivingEntity target) {
        if (this.getMode() == Mode.SWING || !CombatService.canAffect(owner, target)) {
            this.retract();
            return;
        }
        this.entityData.set(TARGET, target.getId());
        this.setPhase(Phase.ATTACHED);
        this.attachedTicks = 0;
        CombatService.dealAbilityDamage(owner, target, this.damage, ModDamageTypes.TENDRIL, false);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.SYMBIOTE_GOO.get(), target.getX(), target.getY(0.5D), target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        }
    }

    private void attachToBlock(LivingEntity owner, BlockPos pos) {
        if (this.getMode() == Mode.GRAB) {
            this.retract();
            return;
        }
        this.entityData.set(ANCHOR, Optional.of(pos));
        this.entityData.set(ROPE_LENGTH, (float) Math.max(2D, owner.position().distanceTo(Vec3.atCenterOf(pos))));
        this.setPhase(Phase.ATTACHED);
        this.attachedTicks = 0;
    }

    private void tickAttached(LivingEntity owner) {
        this.attachedTicks++;
        Entity targetEntity = this.getTarget();
        Optional<BlockPos> anchor = this.getAnchor();
        if (targetEntity instanceof LivingEntity target) {
            if (!target.isAlive() || this.attachedTicks > this.holdTicks) {
                this.retract();
                return;
            }
            this.setPos(target.getX(), target.getY(0.5D), target.getZ());
            if (this.getMode() == Mode.GRAB) {
                // Restrain: no horizontal movement, heavy slowness, periodic squeeze damage.
                CombatService.launch(target, new Vec3(0, Math.min(0, target.getDeltaMovement().y), 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 4, false, false));
                if (this.attachedTicks % 20 == 0) {
                    CombatService.dealAbilityDamage(owner, target, this.damage * 0.5F, ModDamageTypes.TENDRIL, false);
                }
            } else {
                boolean heavy = target.getType().is(Tags.EntityTypes.BOSSES) || target.getBbWidth() * target.getBbHeight() > 4F;
                LivingEntity moved = heavy ? owner : target;
                Vec3 destination = heavy ? target.position() : owner.position();
                Vec3 delta = destination.subtract(moved.position());
                if (delta.length() < 2.0D) {
                    this.retract();
                    return;
                }
                CombatService.launch(moved, delta.normalize().scale(1.1D).add(0, 0.12D, 0));
                moved.fallDistance = 0F;
            }
        } else if (anchor.isPresent()) {
            Vec3 point = Vec3.atCenterOf(anchor.get());
            this.setPos(point.x, point.y, point.z);
            owner.fallDistance = 0F;
            if (this.getMode() == Mode.PULL) {
                Vec3 delta = point.subtract(owner.position());
                if (delta.length() < 2.0D || this.attachedTicks > 60) {
                    this.retract();
                    return;
                }
                CombatService.launch(owner, delta.normalize().scale(1.2D).add(0, 0.1D, 0));
            } else if (this.attachedTicks > 20 * 15 || (owner.onGround() && this.attachedTicks > 10)) {
                // Swing: ends on landing or after 15 s (the held ability also retracts on release).
                this.retract();
            }
        } else {
            this.retract();
        }
    }

    private void tickRetracting(LivingEntity owner) {
        Vec3 home = owner.getEyePosition().add(0, -0.3D, 0);
        Vec3 delta = home.subtract(this.position());
        if (delta.length() <= this.speed * 1.5D) {
            this.discard();
            return;
        }
        Vec3 next = this.position().add(delta.normalize().scale(this.speed * 1.5D));
        this.setPos(next.x, next.y, next.z);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "tendril", 2, state ->
                state.setAndContinue(this.getPhase() == Phase.EXTENDING ? EXTEND_ANIM : IDLE_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
