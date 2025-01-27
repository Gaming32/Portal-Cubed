package com.fusionflux.portalcubed.blocks.blockentities;

import com.fusionflux.gravity_api.api.GravityChangerAPI;
import com.fusionflux.gravity_api.util.RotationUtil;
import com.fusionflux.portalcubed.blocks.PortalCubedBlocks;
import com.fusionflux.portalcubed.entity.CorePhysicsEntity;
import com.fusionflux.portalcubed.entity.EntityAttachments;
import com.fusionflux.portalcubed.util.CustomProperties;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.loader.api.minecraft.ClientOnly;


public class ExcursionFunnelMain extends BlockWithEntity {

    public static final BooleanProperty NORTH;
    public static final BooleanProperty EAST;
    public static final BooleanProperty SOUTH;
    public static final BooleanProperty WEST;
    public static final BooleanProperty UP;
    public static final BooleanProperty DOWN;
    public static final BooleanProperty R_NORTH;
    public static final BooleanProperty R_EAST;
    public static final BooleanProperty R_SOUTH;
    public static final BooleanProperty R_WEST;
    public static final BooleanProperty R_UP;
    public static final BooleanProperty R_DOWN;

    protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);

    public ExcursionFunnelMain(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false).with(UP, false).with(DOWN, false).with(R_NORTH, false).with(R_EAST, false).with(R_SOUTH, false).with(R_WEST, false).with(R_UP, false).with(R_DOWN, false));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }


    static {
        NORTH = Properties.NORTH;
        EAST = Properties.EAST;
        SOUTH = Properties.SOUTH;
        WEST = Properties.WEST;
        UP = Properties.UP;
        DOWN = Properties.DOWN;
        R_NORTH = CustomProperties.R_NORTH;
        R_EAST = CustomProperties.R_EAST;
        R_SOUTH = CustomProperties.R_SOUTH;
        R_WEST = CustomProperties.R_WEST;
        R_UP = CustomProperties.RUP;
        R_DOWN = CustomProperties.R_DOWN;
    }


    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, WEST, SOUTH, UP, DOWN, R_NORTH, R_EAST, R_WEST, R_SOUTH, R_UP, R_DOWN);
    }


    @Override
    @ClientOnly
    @SuppressWarnings("deprecation")
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean isTranslucent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }


    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }



    @Override
    @SuppressWarnings("deprecation")
    public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        return stateFrom.isOf(this) || super.isSideInvisible(state, stateFrom, direction);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        this.addCollisionEffects(world, entity, pos ,state);
    }

    public static Vec3d getPushDirection(BlockState state) {
        Vec3d result = Vec3d.ZERO;
        boolean modifyX = false;
        boolean modifyY = false;
        boolean modifyZ = false;


        if(state.get(Properties.NORTH)){
            result =result.subtract(0, 0, 1);
        }
        if(state.get(Properties.SOUTH)){
            result =result.add(0, 0, 1);
        }
        if(state.get(Properties.EAST)){
            result =result.add(1, 0, 0);
        }
        if(state.get(Properties.WEST)){
            result =result.subtract(1, 0, 0);
        }
        if(state.get(Properties.UP)){
            result =result.add(0, 1, 0);
        }
        if(state.get(Properties.DOWN)){
            result =result.subtract(0, 1, 0);
        }

        if(state.get(Properties.NORTH) && state.get(Properties.SOUTH)){
            modifyZ = true;
        }
        if(state.get(Properties.EAST) && state.get(Properties.WEST)){
            modifyX = true;
        }
        if(state.get(Properties.UP) && state.get(Properties.DOWN)){
            modifyY = true;
        }

        if(state.get(CustomProperties.R_SOUTH)){
            result =result.subtract(0, 0, 1);
        }
        if(state.get(CustomProperties.R_NORTH)){
            result =result.add(0, 0, 1);
        }
        if(state.get(CustomProperties.R_WEST)){
            result =result.add(1, 0, 0);
        }
        if(state.get(CustomProperties.R_EAST)){
            result =result.subtract(1, 0, 0);
        }
        if(state.get(CustomProperties.RUP)){
            result =result.subtract(0, 1, 0);
        }
        if(state.get(CustomProperties.R_DOWN)){
            result = result.add(0, 1, 0);
        }

        if(state.get(CustomProperties.R_NORTH) && state.get(CustomProperties.R_SOUTH)){
            modifyZ = true;
        }
        if(state.get(CustomProperties.R_EAST) && state.get(CustomProperties.R_WEST)){
            modifyX = true;
        }
        if(state.get(CustomProperties.RUP) && state.get(CustomProperties.R_DOWN)){
            modifyY = true;
        }

        if(modifyX){
            result = new Vec3d(.00001,result.getY(), result.getZ() );
        }
        if(modifyY){
            result = new Vec3d(result.getX(),.00001, result.getZ() );
        }
        if(modifyZ){
            result = new Vec3d(result.getX(),result.getY(), .00001 );
        }


        return result;
    }


    private void addCollisionEffects(World world, Entity entity, BlockPos pos,BlockState state) {
        if (entity instanceof PlayerEntity) {
            if (world.isClient()) {
                Vec3d entityCenter = entity.getBoundingBox().getCenter();
                double xOffset = (entityCenter.getX() - pos.getX() - .5);
                double yOffset = (entityCenter.getY() - pos.getY() - .5);
                double zOffset = (entityCenter.getZ() - pos.getZ() - .5);
                Vec3d direction = getPushDirection(state);

                direction = direction.multiply(.125);


                direction = RotationUtil.vecWorldToPlayer(direction, GravityChangerAPI.getGravityDirection(entity));

                entity.setNoGravity(true);

                if (!((EntityAttachments) entity).isInFunnel()) {
                    ((EntityAttachments) entity).setInFunnel(true);
                    entity.setVelocity(0, 0, 0);
                }

                ((EntityAttachments) entity).setFunnelTimer(2);

                Vec3d gotVelocity = entity.getVelocity();

                if (direction.x != 0) {
                    gotVelocity = new Vec3d(direction.getX(), gotVelocity.y, gotVelocity.z);
                } else {
                    gotVelocity = gotVelocity.add(RotationUtil.vecWorldToPlayer(new Vec3d((-(xOffset / Math.abs(xOffset)) * .004), 0, 0), GravityChangerAPI.getGravityDirection(entity)));
                }
                if (direction.y != 0) {
                    gotVelocity = new Vec3d(gotVelocity.x, direction.getY(), gotVelocity.z);
                } else {
                    gotVelocity = gotVelocity.add(RotationUtil.vecWorldToPlayer(new Vec3d(0, (-(yOffset / Math.abs(yOffset)) * .004), 0), GravityChangerAPI.getGravityDirection(entity)));
                }
                if (direction.z != 0) {
                    gotVelocity = new Vec3d(gotVelocity.x, gotVelocity.y, direction.getZ());
                } else {
                    gotVelocity = gotVelocity.add(RotationUtil.vecWorldToPlayer(new Vec3d(0, 0, (-(zOffset / Math.abs(zOffset)) * .004)), GravityChangerAPI.getGravityDirection(entity)));
                }
                entity.setVelocity(gotVelocity);
            }
        } else {
            if (!world.isClient()) {
                double xOffset = (entity.getPos().getX() - pos.getX()) - .5;
                double yOffset = ((entity.getPos().getY() + entity.getHeight() / 2) - pos.getY()) - .5;
                double zOffset = (entity.getPos().getZ() - pos.getZ()) - .5;

                if (xOffset == 0) {
                    xOffset = .1;
                }
                if (yOffset == 0) {
                    yOffset = .1;
                }
                if (zOffset == 0) {
                    zOffset = .1;
                }

                Vec3d direction = getPushDirection(state);
                direction = direction.multiply(.125);

                entity.setNoGravity(true);

                if (!((EntityAttachments) entity).isInFunnel()) {
                    ((EntityAttachments) entity).setInFunnel(true);
                    entity.setVelocity(0, 0, 0); //not this code
                }

                entity.fallDistance = 0;

                ((EntityAttachments) entity).setFunnelTimer(2);

                Vec3d gotVelocity = entity.getVelocity();

                if (direction.x != 0) {
                    gotVelocity = new Vec3d(direction.getX(), gotVelocity.y, gotVelocity.z);
                } else {
                    if (entity instanceof CorePhysicsEntity) {
                        gotVelocity = new Vec3d(-xOffset/4,gotVelocity.y,gotVelocity.z);
                    } else {
                        gotVelocity = gotVelocity.add(RotationUtil.vecWorldToPlayer(new Vec3d((-(xOffset / Math.abs(xOffset)) * .004), 0, 0), GravityChangerAPI.getGravityDirection(entity)));
                    }
                }
                if (direction.y != 0) {
                    gotVelocity = new Vec3d(gotVelocity.x, direction.getY(), gotVelocity.z);
                } else {
                    if (entity instanceof CorePhysicsEntity) {
                        gotVelocity = new Vec3d(gotVelocity.x,-yOffset/4,gotVelocity.z);
                    } else {
                        gotVelocity = gotVelocity.add(RotationUtil.vecWorldToPlayer(new Vec3d(0, (-(yOffset / Math.abs(yOffset)) * .004), 0), GravityChangerAPI.getGravityDirection(entity)));
                    }
                }
                if (direction.z != 0) {
                    gotVelocity = new Vec3d(gotVelocity.x, gotVelocity.y, direction.getZ());
                } else {
                    if (entity instanceof CorePhysicsEntity) {
                        gotVelocity = new Vec3d(gotVelocity.x,gotVelocity.y,-zOffset/4);
                    } else {
                        gotVelocity = gotVelocity.add(RotationUtil.vecWorldToPlayer(new Vec3d(0, 0, (-(zOffset / Math.abs(zOffset)) * .004)), GravityChangerAPI.getGravityDirection(entity)));
                    }
                }
                if (!gotVelocity.equals(Vec3d.ZERO))
                    entity.setVelocity(gotVelocity);
            }
        }
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ExcursionFunnelEntityMain(pos,state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, PortalCubedBlocks.EXCURSION_FUNNEL_ENTITY, ExcursionFunnelEntityMain::tick);
    }

}