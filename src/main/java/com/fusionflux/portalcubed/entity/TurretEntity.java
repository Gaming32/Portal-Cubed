package com.fusionflux.portalcubed.entity;

import com.fusionflux.portalcubed.blocks.PortalCubedBlocks;
import com.fusionflux.portalcubed.compat.rayon.RayonIntegration;
import com.fusionflux.portalcubed.particle.DecalParticleEffect;
import com.fusionflux.portalcubed.sound.PortalCubedSounds;
import com.fusionflux.portalcubed.util.GeneralUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TurretEntity extends CorePhysicsEntity {
    private static final TrackedData<Float> PITCH_SPEED = DataTracker.registerData(TurretEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public static final float MODEL_SCALE = MathHelper.lerp(0.875f, 1 / 1.62f, 1f);
    private static final Box BASE_BOX = createFootBox(0.5f * MODEL_SCALE, 1.5f * MODEL_SCALE, MODEL_SCALE);
    private static final float FALL_SPEED = 0.3f;

    public TurretEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        getDataTracker().startTracking(PITCH_SPEED, 0f);
    }

    @Override
    protected Box calculateBoundingBox() {
        final Box fallenBox = GeneralUtil.rotate(BASE_BOX, getPitch(), Direction.Axis.X);
        final float yaw = MathHelper.wrapDegrees(headYaw);
        Box result = GeneralUtil.rotate(fallenBox, yaw, Direction.Axis.Y);
        if (yaw >= 45 || yaw < -135) {
            result = new Box(-result.minX, result.minY, -result.minZ, -result.maxX, result.maxY, -result.maxZ);
        }
        return result.offset(getPos());
    }

    @Override
    public void tick() {
        float pitchSpeed = getPitchSpeed();
        float pitch = getPitch();
        if (!RayonIntegration.INSTANCE.isPresent()) {
            if (pitchSpeed > 90) {
                pitchSpeed = 90;
            } else if (pitchSpeed < -90) {
                pitchSpeed = -90;
            }
        }
        super.tick();
        if (!RayonIntegration.INSTANCE.isPresent()) {
            setPitch(MathHelper.wrapDegrees(pitch + pitchSpeed));
            if (getPitch() > 90) {
                setPitch(90);
                pitchSpeed = 0;
            } else if (getPitch() < -90) {
                setPitch(-90);
                pitchSpeed = 0;
            }
            pitchSpeed += FALL_SPEED * MathHelper.sign(pitchSpeed) * Math.sqrt(Math.abs(pitch));
            setPitchSpeed(pitchSpeed);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putFloat("PitchSpeed", getPitchSpeed());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setPitchSpeed(nbt.getFloat("PitchSpeed"));
    }

    public float getPitchSpeed() {
        return getDataTracker().get(PITCH_SPEED);
    }

    public void setPitchSpeed(float pitchSpeed) {
        getDataTracker().set(PITCH_SPEED, pitchSpeed);
    }

    public static void makeBulletHole(ServerWorld world, BlockHitResult hit, SoundCategory soundCategory) {
        final BlockState block = world.getBlockState(hit.getBlockPos());
        final SoundEvent soundEffect;
        final Identifier particleTexture;
        if (block.isIn(PortalCubedBlocks.BULLET_HOLE_CONCRETE)) {
            soundEffect = PortalCubedSounds.BULLET_CONCRETE_EVENT;
            particleTexture = DecalParticleEffect.BULLET_HOLE_CONCRETE;
        } else if (block.isIn(PortalCubedBlocks.BULLET_HOLE_GLASS)) {
            soundEffect = PortalCubedSounds.BULLET_GLASS_EVENT;
            particleTexture = null;
        } else if (block.isIn(PortalCubedBlocks.BULLET_HOLE_METAL)) {
            soundEffect = PortalCubedSounds.BULLET_METAL_EVENT;
            particleTexture = null;
        } else {
            soundEffect = null;
            particleTexture = null;
        }
        final Vec3d pos = hit.getPos().add(Vec3d.of(hit.getSide().getVector()).multiply(0.01));
        if (soundEffect != null) {
            world.playSound(null, pos.x, pos.y, pos.z, soundEffect, soundCategory, 0.3f, 1f);
        }
        if (particleTexture != null) {
            world.spawnParticles(
                new DecalParticleEffect(particleTexture, hit.getSide()),
                pos.x, pos.y, pos.z, 0, 0, 0, 0, 0
            );
        }
    }
}