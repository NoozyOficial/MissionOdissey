package com.noozy.missionodyssey.entity;

import com.noozy.missionodyssey.network.WarpSyncPayload;
import com.noozy.missionodyssey.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SpaceshipEntity extends Entity implements GeoEntity {

    private static final EntityDataAccessor<Boolean> ENGINE_ON =
            SynchedEntityData.defineId(SpaceshipEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> VISUAL_ROLL =
            SynchedEntityData.defineId(SpaceshipEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> THROTTLE_AMOUNT =
            SynchedEntityData.defineId(SpaceshipEntity.class, EntityDataSerializers.FLOAT);

    // ── Temporal Jump ────────────────────────────────────────────────────────
    /** After receiving the jump packet the server waits this many ticks before
     *  firing, so the teleport coincides with the client's white-flash phase. */
    public  static final int  JUMP_FIRE_DELAY    = 30;   // ticks (~1.5 s)
    public  static final int  JUMP_COOLDOWN_MAX  = 600;  // 30 s
    public  static final double JUMP_DISTANCE    = 10_000.0;

    /** Server-side countdown until the jump fires (0 = no pending jump). */
    private int jumpFireTicks = 0;
    /** Server-side cooldown remaining (0 = ready). */
    private int jumpCooldownTicks = 0;

    public boolean inputForward;
    public boolean inputBackward;
    public boolean inputLeft;
    public boolean inputRight;
    public boolean inputUp;
    public boolean inputDown;

    private float smoothThrottle;
    private float smoothYawInput;
    private float smoothPitchInput;
    private float currentRoll;

    private static final float MAX_THRUST = 0.12f;
    private static final float REVERSE_THRUST = 0.04f;
    private static final float DRAG = 0.965f;
    private static final float GRAVITY = 0.035f;
    private static final float LIFT_FACTOR = 0.85f;
    private static final float YAW_SPEED = 2.8f;
    private static final float PITCH_SPEED = 2.0f;
    private static final float MAX_ROLL = 35.0f;
    private static final float PITCH_LIMIT = 55.0f;
    private static final float THROTTLE_LERP = 0.045f;
    private static final float YAW_LERP = 0.07f;
    private static final float PITCH_LERP = 0.07f;
    private static final float ROLL_LERP = 0.08f;
    private static final float PITCH_RETURN = 0.97f;
    private static final float HOVER_BOB_AMPLITUDE = 0.002f;

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean wasEngineOn = false;

    private static final RawAnimation ANIM_OFF = RawAnimation.begin().thenLoop("animation.spaceship.off");
    private static final RawAnimation ANIM_TURN_ON = RawAnimation.begin()
            .thenPlay("animation.spaceship.turn_on")
            .thenLoop("animation.spaceship.driving");
    private static final RawAnimation ANIM_DRIVING = RawAnimation.begin().thenLoop("animation.spaceship.driving");
    private static final RawAnimation ANIM_TURN_OFF = RawAnimation.begin()
            .thenPlay("animation.spaceship.turn_off")
            .then("animation.spaceship.off", Animation.LoopType.HOLD_ON_LAST_FRAME);

    public SpaceshipEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public SpaceshipEntity(Level world, double x, double y, double z) {
        this(ModEntities.SPACESHIP.get(), world);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ENGINE_ON, false);
        builder.define(VISUAL_ROLL, 0.0f);
        builder.define(THROTTLE_AMOUNT, 0.0f);
    }

    // ── Temporal Jump API ────────────────────────────────────────────────────

    public boolean isJumpReady() {
        return jumpCooldownTicks <= 0 && jumpFireTicks <= 0;
    }

    /**
     * Called from the network handler when the client requests a jump.
     * Queues the jump; it fires {@link #JUMP_FIRE_DELAY} ticks later so
     * the server-side teleport coincides with the client's white-flash phase.
     */
    public void requestTemporalJump() {
        if (!isJumpReady()) return;
        jumpFireTicks = JUMP_FIRE_DELAY;
    }

    public boolean isEngineOn() {
        return entityData.get(ENGINE_ON);
    }

    public void setEngineOn(boolean on) {
        entityData.set(ENGINE_ON, on);
    }

    public float getVisualRoll() {
        return entityData.get(VISUAL_ROLL);
    }

    public float getThrottleAmount() {
        return entityData.get(THROTTLE_AMOUNT);
    }

    @Override
    public void tick() {
        // Captura a rotação ANTES de super.tick() aplicar qualquer correção de lerp do servidor.
        float savedYRot = getYRot();
        float savedXRot = getXRot();

        super.tick();

        // Restaura os valores pré-tick para que a interpolação de renderização
        // (yRotO → yRot) use a referência correta e não um valor pós-lerp.
        yRotO = savedYRot;
        xRotO = savedXRot;

        // ── Temporal jump countdown (server only) ───────────────────────────────
        if (!level().isClientSide()) {
            if (jumpCooldownTicks > 0) jumpCooldownTicks--;

            if (jumpFireTicks > 0) {
                jumpFireTicks--;
                if (jumpFireTicks == 0) {
                    performTemporalJump();
                }
            }
        }

        Entity passenger = getControllingPassenger();

        if (passenger != null) {
            // No cliente, cancela a correção de lerp do servidor antes de tickFlying().
            // O lerp captura um ângulo antigo e briga com tickFlying() a cada tick.
            // Perto da fronteira ±180°, o wrapDegrees inverte a direcção da correção
            // e a entidade começa a girar sem controle — este restore impede isso.
            if (level().isClientSide()) {
                setYRot(savedYRot);
                setXRot(savedXRot);
            }
            tickFlying();
        } else {
            tickIdle();
        }

        move(MoverType.SELF, getDeltaMovement());
        checkInsideBlocks();
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        if (getControllingPassenger() != null) {
            super.lerpTo(x, y, z, getYRot(), getXRot(), interpolationSteps);
        } else {
            super.lerpTo(x, y, z, yaw, pitch, interpolationSteps);
        }
    }

    /**
     * Teleports the ship (and its passenger) 10 000 blocks in the direction
     * the controlling player is looking, then starts the cooldown.
     */
    private void performTemporalJump() {
        LivingEntity driver = getControllingPassenger();
        if (!(driver instanceof ServerPlayer serverPlayer)) return;

        Vec3 look = driver.getLookAngle().normalize();
        double newX = getX() + look.x * JUMP_DISTANCE;
        double newY = getY() + look.y * JUMP_DISTANCE;
        double newZ = getZ() + look.z * JUMP_DISTANCE;

        newY = Mth.clamp(newY,
                level().getMinBuildHeight() + 5.0,
                level().getMaxBuildHeight() - 5.0);

        serverPlayer.stopRiding();

        this.teleportTo(newX, newY, newZ);

        serverPlayer.teleportTo((ServerLevel) this.level(), newX, newY, newZ, serverPlayer.getYRot(), serverPlayer.getXRot());

        serverPlayer.startRiding(this, true);

        setDeltaMovement(Vec3.ZERO);

        jumpCooldownTicks = JUMP_COOLDOWN_MAX;
        PacketDistributor.sendToPlayer(serverPlayer,
                new WarpSyncPayload(JUMP_COOLDOWN_MAX));
    }

    private void tickFlying() {
        float targetThrottle = inputForward ? 1.0f : (inputBackward ? -0.3f : 0.0f);
        float targetYaw = inputRight ? 1.0f : (inputLeft ? -1.0f : 0.0f);
        float targetPitch = inputDown ? 1.0f : (inputUp ? -1.0f : 0.0f);

        smoothThrottle = Mth.lerp(THROTTLE_LERP, smoothThrottle, targetThrottle);
        smoothYawInput = Mth.lerp(YAW_LERP, smoothYawInput, targetYaw);
        smoothPitchInput = Mth.lerp(PITCH_LERP, smoothPitchInput, targetPitch);

        float speedFactor = 0.5f + 0.5f * Math.abs(smoothThrottle);
        float yawDelta = smoothYawInput * YAW_SPEED * speedFactor;
        setYRot(getYRot() + yawDelta);

        float pitchDelta = smoothPitchInput * PITCH_SPEED;
        setXRot(Mth.clamp(getXRot() + pitchDelta, -PITCH_LIMIT, PITCH_LIMIT));

        if (!inputUp && !inputDown) {
            setXRot(getXRot() * PITCH_RETURN);
            if (Math.abs(getXRot()) < 0.1f) setXRot(0);
        }

        float targetRoll = smoothYawInput * MAX_ROLL * speedFactor;
        currentRoll = Mth.lerp(ROLL_LERP, currentRoll, targetRoll);
        entityData.set(VISUAL_ROLL, currentRoll);

        float yawRad = getYRot() * DEG_TO_RAD;
        float pitchRad = getXRot() * DEG_TO_RAD;

        Vec3 forward = new Vec3(
                -Mth.sin(yawRad) * Mth.cos(pitchRad),
                -Mth.sin(pitchRad),
                Mth.cos(yawRad) * Mth.cos(pitchRad)
        );

        float thrustMagnitude = smoothThrottle > 0
                ? smoothThrottle * MAX_THRUST
                : smoothThrottle * REVERSE_THRUST;

        Vec3 thrustVec = forward.scale(thrustMagnitude);

        Vec3 velocity = getDeltaMovement();

        velocity = velocity.add(thrustVec);
        velocity = velocity.scale(DRAG);

        float liftCompensation = Math.max(0, smoothThrottle) * GRAVITY * LIFT_FACTOR;
        float effectiveGravity = GRAVITY - liftCompensation;
        velocity = velocity.add(0, -effectiveGravity, 0);

        if (inputUp) {
            velocity = velocity.add(0, 0.04, 0);
        }
        if (inputDown) {
            velocity = velocity.add(0, -0.02, 0);
        }

        setDeltaMovement(velocity);

        boolean shouldBeOn = Math.abs(smoothThrottle) > 0.01f || inputUp || inputDown
                || inputLeft || inputRight;
        if (shouldBeOn && !isEngineOn()) {
            setEngineOn(true);
        }

        entityData.set(THROTTLE_AMOUNT, Math.abs(smoothThrottle));
    }

    private void tickIdle() {
        if (isEngineOn()) {
            setEngineOn(false);
        }

        smoothThrottle *= 0.9f;
        smoothYawInput *= 0.9f;
        smoothPitchInput *= 0.9f;

        currentRoll = Mth.lerp(0.1f, currentRoll, 0.0f);
        entityData.set(VISUAL_ROLL, currentRoll);
        entityData.set(THROTTLE_AMOUNT, 0.0f);

        setXRot(getXRot() * 0.93f);

        Vec3 vel = getDeltaMovement();
        vel = vel.scale(0.92);
        vel = vel.add(0, -GRAVITY, 0);

        if (onGround()) {
            vel = new Vec3(vel.x * 0.8, 0, vel.z * 0.8);
            float bob = Mth.sin(tickCount * 0.05f) * HOVER_BOB_AMPLITUDE;
            vel = vel.add(0, bob, 0);
        }

        setDeltaMovement(vel);

        inputForward = false;
        inputBackward = false;
        inputLeft = false;
        inputRight = false;
        inputUp = false;
        inputDown = false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide()) {
            if (player.isPassenger()) {
                return InteractionResult.PASS;
            }
            player.startRiding(this);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = getFirstPassenger();
        return passenger instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (hasPassenger(passenger)) {
            double seatY = 0.9;
            moveFunction.accept(passenger, getX(), getY() + seatY, getZ());
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        inputForward = false;
        inputBackward = false;
        inputLeft = false;
        inputRight = false;
        inputUp = false;
        inputDown = false;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        float yawRad = getYRot() * DEG_TO_RAD;
        double offsetX = Mth.cos(yawRad) * 2.5;
        double offsetZ = Mth.sin(yawRad) * 2.5;
        return new Vec3(getX() + offsetX, getY(), getZ() + offsetZ);
    }

    public float getRoll(float partialTick) {
        return getVisualRoll();
    }

    public float getSpeedNormalized() {
        double speed = getDeltaMovement().length();
        return (float) Math.min(speed / (MAX_THRUST / (1.0 - DRAG)), 1.0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "ship_controller", 5, state -> {
            SpaceshipEntity ship = state.getAnimatable();
            boolean engineOn = ship.isEngineOn();

            if (engineOn && !wasEngineOn) {
                wasEngineOn = true;
                return state.setAndContinue(ANIM_TURN_ON);
            } else if (!engineOn && wasEngineOn) {
                wasEngineOn = false;
                return state.setAndContinue(ANIM_TURN_OFF);
            } else if (engineOn) {
                return state.setAndContinue(ANIM_DRIVING);
            } else {
                return state.setAndContinue(ANIM_OFF);
            }
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        jumpCooldownTicks = nbt.getInt("JumpCooldown");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("JumpCooldown", jumpCooldownTicks);
    }
}
