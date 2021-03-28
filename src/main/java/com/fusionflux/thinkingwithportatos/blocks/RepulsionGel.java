package com.fusionflux.thinkingwithportatos.blocks;

import com.fusionflux.thinkingwithportatos.entity.BlockCollisionLimiter;
import com.fusionflux.thinkingwithportatos.entity.CubeEntity;
import com.fusionflux.thinkingwithportatos.entity.ThinkingWithPortatosEntities;
import com.fusionflux.thinkingwithportatos.sound.ThinkingWithPortatosSounds;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;

import dev.lazurite.rayon.core.impl.physics.PhysicsThread;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public class RepulsionGel extends GelFlat {

    private final BlockCollisionLimiter limiter = new BlockCollisionLimiter();

    public RepulsionGel(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false).with(UP, false).with(DOWN, true));
    }


    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        this.addCollisionEffects(world, entity,pos);
    }

    private void addCollisionEffects(World world, Entity entity,BlockPos pos) {
        if (entity.getType().equals(EntityType.BOAT)) {
            entity.damage(DamageSource.MAGIC, 200);
        } else {
            BlockState state = world.getBlockState(pos);
            if (!entity.isSneaking()) {
              //  if (entity.getVelocity().y < 1.65)
                Vec3d direction = new Vec3d(0,0,0);
                if (state.get(UP)) {
                    direction=direction.add(0,-1,0);
                }

                if (state.get(DOWN)) {
                    direction=direction.add(0,1,0);
                    System.out.println(direction);
                }

                if (state.get(NORTH)) {
                    direction=direction.add(0,0,1);
                }

                if (state.get(SOUTH)) {
                    direction=direction.add(0,0,-1);
                }

                if (state.get(EAST)) {
                    direction=direction.add(-1,0,0);
                }

                if (state.get(WEST)) {
                    direction=direction.add(1,0,0);
                }
                    //entity.setVelocity(entity.getVelocity().add(0, 1.65D, 0));

                entity.setVelocity(entity.getVelocity().add(1.65*direction.x,1.65*direction.y,1.65*direction.z));
                if (limiter.check(world, entity)) {
                    world.playSound(null, entity.getPos().getX(), entity.getPos().getY(), entity.getPos().getZ(), ThinkingWithPortatosSounds.GEL_BOUNCE_EVENT, SoundCategory.MASTER, .3F, 1F);
                }

            }
        }

    }
}
