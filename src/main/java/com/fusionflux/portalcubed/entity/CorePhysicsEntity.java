package com.fusionflux.portalcubed.entity;

import com.fusionflux.gravity_api.api.GravityChangerAPI;
import com.fusionflux.gravity_api.util.Gravity;
import com.fusionflux.gravity_api.util.RotationUtil;
import com.fusionflux.portalcubed.accessor.Accessors;
import com.fusionflux.portalcubed.accessor.CalledValues;
import com.fusionflux.portalcubed.blocks.PortalCubedBlocks;
import com.fusionflux.portalcubed.client.packet.PortalCubedClientPackets;
import com.fusionflux.portalcubed.items.PortalCubedItems;
import com.fusionflux.portalcubed.packet.NetworkingSafetyWrapper;
import com.fusionflux.portalcubed.sound.PortalCubedSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.quiltmc.qsl.networking.api.PacketByteBufs;
import org.quiltmc.qsl.networking.api.ServerPlayNetworking;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class CorePhysicsEntity extends PathAwareEntity  {

    private float fizzleProgress = 0f;
    private boolean fizzling = false;
    private final Set<ServerPlayerEntity> tracking = new HashSet<>();

    public CorePhysicsEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    private boolean canUsePortals = true;
    private boolean hasCollided;
    private int timeSinceLastSound;

    public Vec3d lastPos = this.getPos();

    private float rotation_yaw = this.bodyYaw;

    private final Vec3d offsetHeight = new Vec3d(0,this.getHeight()/2,0);

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        tracking.add(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        tracking.remove(player);
    }

    @Override
    public boolean collides() {
        return !this.isRemoved();
    }

    @Override
    public boolean isCollidable() {
        return canUsePortals;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return  damageSource != DamageSource.OUT_OF_WORLD && !damageSource.isSourceCreativePlayer() ;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!this.world.isClient && !this.isRemoved()) {
            boolean bl = source.getAttacker() instanceof PlayerEntity && ((PlayerEntity) source.getAttacker()).getAbilities().creativeMode;
            if (source.getAttacker() instanceof PlayerEntity || source == DamageSource.OUT_OF_WORLD) {
                if(source.getAttacker() instanceof PlayerEntity && ((PlayerEntity) source.getAttacker()).getAbilities().allowModifyWorld){
                    if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS) && !bl) {
                        this.dropItem(PortalCubedItems.STORAGE_CUBE);
                    }
                    this.discard();
                }
                if(!(source.getAttacker() instanceof PlayerEntity)) {
                    if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS) && !bl) {
                        this.dropItem(PortalCubedItems.STORAGE_CUBE);
                    }
                    this.discard();
                }
            }

        }
        return false;
    }

    @Override
    public boolean shouldRenderName() {
        return false;
    }
    @Override
    public boolean isCustomNameVisible() {
        return false;
    }


    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        double d = packet.getX();
        double e = packet.getY();
        double f = packet.getZ();
        float g = packet.method_11168();
        float h = packet.method_11171();
        this.syncPacketPositionCodec(d, e, f);
        this.bodyYaw = 0;
        this.headYaw = 0;
        this.prevBodyYaw = this.bodyYaw;
        this.prevHeadYaw = this.headYaw;
        this.setNoDrag(true);
        this.setId(packet.getId());
        this.setUuid(packet.getUuid());
        this.updatePositionAndAngles(d, e, f, g, h);
        this.setVelocity((float)packet.getVelocityX(), (float)packet.getVelocityY(), (float)packet.getVelocityZ());
    }


    public static final TrackedData<Optional<UUID>> HOLDER_UUID = DataTracker.registerData(CorePhysicsEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.getDataTracker().startTracking(HOLDER_UUID, Optional.empty());
    }

    public Boolean getUUIDPresent() {
        return getDataTracker().get(HOLDER_UUID).isPresent();
    }

    public UUID getHolderUUID() {
        if (getDataTracker().get(HOLDER_UUID).isPresent()) {
            return getDataTracker().get(HOLDER_UUID).get();
        }
        return null;
    }
    public void setRotYaw(float yaw) {
        rotation_yaw = yaw;
    }

    public float getRotYaw(){
        return  rotation_yaw;
    }

    public void setHolderUUID(UUID uuid) {
        if(uuid != null) {
            if (getDataTracker().get(HOLDER_UUID).isPresent()) {
                PlayerEntity player = (PlayerEntity) ((ServerWorld) world).getEntity(getHolderUUID());
                if(player != null) {
                    CalledValues.setCubeUUID(player,null);
                }
            }
            this.getDataTracker().set(HOLDER_UUID, Optional.of(uuid));
        }else {
            this.getDataTracker().set(HOLDER_UUID, Optional.empty());
        }
    }

    @Override
    public boolean canUsePortals() {
        return canUsePortals;
    }

    public void dropCube(){
        setHolderUUID(null);
        var byteBuf = PacketByteBufs.create();
        byteBuf.writeDouble(this.getPos().x);
        byteBuf.writeDouble(this.getPos().y);
        byteBuf.writeDouble(this.getPos().z);
        byteBuf.writeDouble(this.lastPos.x);
        byteBuf.writeDouble(this.lastPos.y);
        byteBuf.writeDouble(this.lastPos.z);
        byteBuf.writeFloat(this.rotation_yaw);
        byteBuf.writeUuid(this.getUuid());
        NetworkingSafetyWrapper.sendFromClient("cubeposupdate", byteBuf);
    }

    @Override
    public void tick() {
        super.tick();
        timeSinceLastSound++;
        this.bodyYaw = rotation_yaw;
        this.headYaw = rotation_yaw;
        canUsePortals = !getUUIDPresent();
        Vec3d rotatedOffset = RotationUtil.vecPlayerToWorld(offsetHeight, GravityChangerAPI.getGravityDirection(this));
        this.lastPos = this.getPos();
        if(!world.isClient) {
            this.setNoDrag(!this.isOnGround() && !this.world.getBlockState(this.getBlockPos()).getBlock().equals(PortalCubedBlocks.EXCURSION_FUNNEL));
            if (getUUIDPresent()) {
                PlayerEntity player = (PlayerEntity) ((ServerWorld) world).getEntity(getHolderUUID());
                if (player != null && player.isAlive()) {
                    Vec3d vec3d = player.getCameraPosVec(0);
                    double d = 2;
                    canUsePortals = false;
                    Vec3d vec3d2 = this.getPlayerRotationVector(player.getPitch(),player.getYaw());
                    Vec3d vec3d3 = vec3d.add((vec3d2.x * d) - rotatedOffset.x, (vec3d2.y * d) - rotatedOffset.y, (vec3d2.z * d) - rotatedOffset.z);
                    GravityChangerAPI.addGravity( this, new Gravity(GravityChangerAPI.getGravityDirection(player),10,1,"player_interaction"));
                    this.fallDistance = 0;
                    rotation_yaw = player.headYaw;

                    move(
                        MovementType.PLAYER,
                        RotationUtil.vecWorldToPlayer(vec3d3.subtract(getPos()), GravityChangerAPI.getGravityDirection(player))
                    );
                }else{
                    if(player != null ){
                        setHolderUUID(null);
                    }
                    canUsePortals = true;
                }
            }
        }else{
            if (getUUIDPresent()) {
                PlayerEntity player = (PlayerEntity)((Accessors) world).getEntity(getHolderUUID());
                if (player != null && player.isAlive()) {
                    Vec3d vec3d = player.getCameraPosVec(0);
                    double d = 2;
                    Vec3d vec3d2 = player.getRotationVec(1.0F);
                    rotation_yaw = player.headYaw;
                    Vec3d vec3d3 = vec3d.add((vec3d2.x * d) - rotatedOffset.x, (vec3d2.y * d) - rotatedOffset.y, (vec3d2.z * d) - rotatedOffset.z);
                    move(
                        MovementType.PLAYER,
                        RotationUtil.vecWorldToPlayer(vec3d3.subtract(getPos()), GravityChangerAPI.getGravityDirection(player))
                    );
                }
            }
        }
        if(this.getVelocity().y < -3.92){
            this.setVelocity(this.getVelocity().add(0,.81d,0));
        }
        if (fizzling) {
            if (world.isClient) {
                fizzleProgress += MinecraftClient.getInstance().getTickDelta();
            } else {
                fizzleProgress += 0.05f;
                if (fizzleProgress >= 1f) {
                    remove(RemovalReason.KILLED);
                }
            }
        }
    }

    public void startFizzlingProgress() {
        fizzling = true;
    }

    public void fizzle() {
        world.playSound(null, getX(), getY(), getZ(), PortalCubedSounds.MATERIAL_EMANCIPATION_EVENT, SoundCategory.NEUTRAL, 0.1f, 1f);
        setNoGravity(true);
        fizzling = true;
        final PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(getId());
        final Packet<?> packet = ServerPlayNetworking.createS2CPacket(PortalCubedClientPackets.FIZZLE_PACKET, buf);
        for (final ServerPlayerEntity player : tracking) {
            player.networkHandler.sendPacket(packet);
        }
    }

    public float getFizzleProgress() {
        return fizzleProgress;
    }

    protected final Vec3d getPlayerRotationVector(float pitch, float yaw) {
        float f = pitch * (float) (Math.PI / 180.0);
        float g = -yaw * (float) (Math.PI / 180.0);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return RotationUtil.vecPlayerToWorld(new Vec3d(i * j, -k, h * j), GravityChangerAPI.getGravityDirection(this));
    }

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        super.move(movementType, movement);
        if (horizontalCollision) {
            if (!hasCollided) {
                hasCollided = true;
                if (!world.isClient && timeSinceLastSound >= 20) {
                    world.playSoundFromEntity(null, this, getCollisionSound(), SoundCategory.NEUTRAL, 1f, 1f);
                    timeSinceLastSound = 0;
                }
            }
        } else {
            hasCollided = false;
        }
    }

    protected SoundEvent getCollisionSound() {
        return PortalCubedSounds.CUBE_LOW_HIT_EVENT; // TODO: implement for other physics objects (this requires a lot of assets)
    }

    @Override
    public void onRemoved() {
        if(!world.isClient) {
            PlayerEntity player = (PlayerEntity) ((ServerWorld) world).getEntity(getHolderUUID());
            if (player != null) {
                CalledValues.setCubeUUID(player,null);
            }
        }
    }

    @Override
    protected void updatePostDeath() {
        if(!world.isClient) {
            PlayerEntity player = (PlayerEntity) ((ServerWorld) world).getEntity(getHolderUUID());
            if (player != null) {
                CalledValues.setCubeUUID(player,null);
            }
        }
        super.updatePostDeath();
    }

    @Override
    public void onDeath(DamageSource source) {
        if(!world.isClient) {
            PlayerEntity player = (PlayerEntity) ((ServerWorld) world).getEntity(getHolderUUID());
            if (player != null) {
                CalledValues.setCubeUUID(player,null);
            }
        }
        super.onDeath(source);
    }

}
