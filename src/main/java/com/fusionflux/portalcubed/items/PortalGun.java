package com.fusionflux.portalcubed.items;


import com.fusionflux.portalcubed.accessor.CalledValues;
import com.fusionflux.portalcubed.accessor.EntityPortalsAccess;
import com.fusionflux.portalcubed.blocks.GelFlat;
import com.fusionflux.portalcubed.blocks.PortalCubedBlocks;
import com.fusionflux.portalcubed.entity.ExperimentalPortal;
import com.fusionflux.portalcubed.entity.PortalCubedEntities;
import com.fusionflux.portalcubed.sound.PortalCubedSounds;
import com.fusionflux.portalcubed.util.IPQuaternion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;


public class PortalGun extends Item implements DyeableItem {

    public PortalGun(Settings settings) {
        super(settings);
    }

  // @Environment(EnvType.CLIENT)
  // public static void registerAlternateModels() {
  //     FabricModelPredicateProviderRegistry.register(PortalCubedItems.PORTAL_GUN, PortalCubed.id("variant"), (stack, world, livingEntity, provider) -> {
  //         if (livingEntity == null) {
  //             return 0;
  //         }
  //         // Defaults to 0
  //         return stack.getOrCreateNbt().getInt("variant");
  //     });
  // }

    @Override
    public int getColor(ItemStack stack) {
        NbtCompound compoundTag = stack.getOrCreateNbt();
        boolean complementary = compoundTag.getBoolean("complementary");
        compoundTag = stack.getSubNbt("display");
        return compoundTag != null && compoundTag.contains("color", 99) ? complementary ? compoundTag.getInt("color") * -1 : compoundTag.getInt("color") : (complementary ? 14842149 : -14842149);
    }

    public void useLeft(World world, PlayerEntity user, Hand hand) {
        if(!user.isSpectator()) {
            ItemStack stack = user.getStackInHand(hand);
            stack.getOrCreateNbt().putBoolean("complementary", false);
            useImpl(world, user, stack, true);
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        stack.getOrCreateNbt().putBoolean("complementary", true);
        return useImpl(world, user, stack, false);
    }

    public TypedActionResult<ItemStack> useImpl(World world, PlayerEntity user, ItemStack stack, boolean leftClick) {
        if (!world.isClient) {
            NbtCompound tag = stack.getOrCreateNbt();

            //PortalPlaceholderEntity portalOutline;
            ExperimentalPortal portalholder;
            NbtCompound portalsTag = tag.getCompound(world.getRegistryKey().toString());

            //boolean outlineExists = false;
            //if (portalsTag.contains((leftClick ? "Left" : "Right") + "Background")) {
            //    portalOutline = (PortalPlaceholderEntity) ((ServerWorld) world).getEntity(portalsTag.getUuid((leftClick ? "Left" : "Right") + "Background"));
//
            //    if (portalOutline == null) {
            //        portalOutline = PortalCubedEntities.PORTAL_PLACEHOLDER.create(world);
            //    } else {
            //        outlineExists = true;
            //    }
            //} else {
            //    portalOutline = PortalCubedEntities.PORTAL_PLACEHOLDER.create(world);
            //}

            boolean portalExists = false;
            if (portalsTag.contains((leftClick ? "Left" : "Right") + "Portal")) {
                portalholder = (ExperimentalPortal) ((ServerWorld) world).getEntity(portalsTag.getUuid((leftClick ? "Left" : "Right") + "Portal"));
                if (portalholder == null) {
                    portalholder = PortalCubedEntities.EXPERIMENTAL_PORTAL.create(world);
                } else {
                    portalExists = true;
                }
            } else {
                portalholder = PortalCubedEntities.EXPERIMENTAL_PORTAL.create(world);
            }

            ExperimentalPortal otherPortal;
            if (portalsTag.contains((leftClick ? "Right" : "Left") + "Portal")) {
                otherPortal = (ExperimentalPortal) ((ServerWorld) world).getEntity(portalsTag.getUuid((leftClick ? "Right" : "Left") + "Portal"));
            } else {
                otherPortal = null;
            }

            Vec3i up;
            Vec3i normal;
            Vec3i right;
            BlockPos blockPos;

            //= HitResult hitResult = user.raycast(128.0D, 0.0F, false);
            HitResult hitResult = customRaycast(user,128.0D, 0.0F, false);
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                //Block.isFaceFullSquare(world.getBlockState(((BlockHitResult) hitResult).getBlockPos()).getCollisionShape(world,((BlockHitResult) hitResult).getBlockPos()),((BlockHitResult) hitResult).getSide().getOpposite());
                blockPos = ((BlockHitResult) hitResult).getBlockPos();
                normal = ((BlockHitResult) hitResult).getSide().getOpposite().getVector();
                if (normal.getY() == 0) {
                    up = new Vec3i(0, 1, 0);
                } else {
                    up = user.getHorizontalFacing().getVector();
                }
                right = up.crossProduct(normal);



                Vec3d portalPos1 = calcPos(blockPos, up, normal, right, false);
                Vec3d placeholderPos1 = calcPos(blockPos, up, normal, right, true);

                if(!validPos(world,up,right,portalPos1)) {
                    for (int i = 1; i < 9; i++) {
                        Vec3d shiftedPortalPos = portalPos1;
                        Vec3d shiftedPortalPlaceholder = placeholderPos1;
                        switch (i) {
                            case 1 -> {
                                shiftedPortalPos = portalPos1.add(Vec3d.of(up).multiply(-1.0));
                                shiftedPortalPlaceholder = placeholderPos1.add(Vec3d.of(up).multiply(-1.0));
                            }
                            case 2 -> {
                                shiftedPortalPos = portalPos1.add(Vec3d.of(right).multiply(-1.0));
                                shiftedPortalPlaceholder = placeholderPos1.add(Vec3d.of(right).multiply(-1.0));
                            }
                            case 3 -> {
                                shiftedPortalPos = portalPos1.add(Vec3d.of(up));
                                shiftedPortalPlaceholder = placeholderPos1.add(Vec3d.of(up));
                            }
                            case 4 -> {
                                shiftedPortalPos = portalPos1.add(Vec3d.of(right));
                                shiftedPortalPlaceholder = placeholderPos1.add(Vec3d.of(right));
                            }
                            case 5 -> {
                                shiftedPortalPos = portalPos1.add(Vec3d.of(right).multiply(-1.0)).add(Vec3d.of(up).multiply(-1.0));
                                shiftedPortalPlaceholder = placeholderPos1.add(Vec3d.of(right).multiply(-1.0)).add(Vec3d.of(up).multiply(-1.0));
                            }
                            case 6 -> {
                                shiftedPortalPos = portalPos1.add(Vec3d.of(right).multiply(-1.0)).add(Vec3d.of(up));
                                shiftedPortalPlaceholder = placeholderPos1.add(Vec3d.of(right).multiply(-1.0)).add(Vec3d.of(up));
                            }
                            case 7 -> {
                                shiftedPortalPos = portalPos1.add(Vec3d.of(up)).add(Vec3d.of(right));
                                shiftedPortalPlaceholder = placeholderPos1.add(Vec3d.of(up)).add(Vec3d.of(right));
                            }
                            case 8 -> {
                                shiftedPortalPos = portalPos1.add(Vec3d.of(right)).add(Vec3d.of(up).multiply(-1.0));
                                shiftedPortalPlaceholder = placeholderPos1.add(Vec3d.of(right)).add(Vec3d.of(up).multiply(-1.0));
                            }
                        }

                        if (validPos(world, up, right, shiftedPortalPos)) {
                            portalPos1 = shiftedPortalPos;
                            placeholderPos1 = shiftedPortalPlaceholder;
                            break;
                        }

                        if (i == 8) {
                            world.playSound(null, user.getPos().getX(), user.getPos().getY(), user.getPos().getZ(), PortalCubedSounds.INVALID_PORTAL_EVENT, SoundCategory.NEUTRAL, .3F, 1F);
                            return TypedActionResult.pass(stack);
                        }
                    }
                }

                world.playSound(null, user.getPos().getX(), user.getPos().getY(), user.getPos().getZ(), leftClick ? PortalCubedSounds.FIRE_EVENT_PRIMARY : PortalCubedSounds.FIRE_EVENT_SECONDARY, SoundCategory.NEUTRAL, .3F, 1F);
                if (portalholder != null && portalholder.isAlive()) {
                    world.playSound(null, portalholder.getPos().getX(), portalholder.getPos().getY(), portalholder.getPos().getZ(), PortalCubedSounds.ENTITY_PORTAL_CLOSE, SoundCategory.NEUTRAL, .1F, 1F);
                }


                //Pair<Double, Double> rotAngles = IPQuaternion.getPitchYawFromRotation(getPortalOrientationQuaternion(Vec3d.of(right), Vec3d.of(up)));
                //assert portalOutline != null;
                //portalOutline.setPos(placeholderPos1.x, placeholderPos1.y, placeholderPos1.z);
                //portalOutline.setYaw(rotAngles.getLeft().floatValue() + (90 * up.getX()));
                //portalOutline.setPitch(rotAngles.getRight().floatValue());
                //portalOutline.setRoll((rotAngles.getRight().floatValue() + (90)) * up.getX());
                //portalOutline.setColor(this.getColor(stack));
                //portalOutline.noClip = true;
                //if (!outlineExists) {
                //    world.spawnEntity(portalOutline);
                //}


                assert portalholder != null;
                portalholder.setOriginPos(portalPos1);
                CalledValues.setDestination(portalholder,portalPos1);

                Pair<Double, Double> rotAngles = IPQuaternion.getPitchYawFromRotation(getPortalOrientationQuaternion(Vec3d.of(right), Vec3d.of(up)));
                //portalholder.setPos(placeholderPos1.x, placeholderPos1.y, placeholderPos1.z);
                portalholder.setYaw(rotAngles.getLeft().floatValue() + (90 * up.getX()));
                portalholder.setPitch(rotAngles.getRight().floatValue());
                portalholder.setRoll((rotAngles.getRight().floatValue() + (90)) * up.getX());
                portalholder.setColor(this.getColor(stack));

                //portalholder.setDestination(portalPos1);
                CalledValues.setOrientation(portalholder,Vec3d.of(right),Vec3d.of(up).multiply(-1));
                // portalholder.setOrientationAndSize(
                //         Vec3d.of(right), //axisW
                //         Vec3d.of(up).multiply(-1)
                // );
                //PortalCubedComponents.PORTAL_DATA.sync(portalholder);
                //portalholder.setOutline(portalOutline.getUuidAsString());
                //portalOutline.axisH = CalledValues.getAxisH(portalholder);
                //portalOutline.axisW = CalledValues.getAxisW(portalholder);

                if (portalExists && otherPortal == null) {
                    //PortalManipulation.adjustRotationToConnect(PortalAPI.createFlippedPortal(portalholder), portalholder);
                    //portalholder.reloadAndSyncToClient();

                }
                if (!portalExists) {
                    portalholder.setLinkedPortalUuid("null");
                    world.spawnEntity(portalholder);
                    ((EntityPortalsAccess) user).addPortalToList(portalholder);

                    CalledValues.setPlayer(portalholder,user.getUuid());

                    CalledValues.addPortals(user,portalholder.getUuid());
                }
                if (otherPortal == null) {
                    otherPortal = getPotentialOpposite(world, portalPos1, portalholder, portalholder.getColor(), false)
                        .orElse(null);
                }
                if (otherPortal != null) {
                    linkPortals(portalholder, otherPortal, 0.1f);

                    CalledValues.setPlayer(portalholder,user.getUuid());
                    CalledValues.setPlayer(otherPortal,user.getUuid());

                    CalledValues.addPortals(user,portalholder.getUuid());
                    CalledValues.addPortals(user,otherPortal.getUuid());

                    //PortalManipulation.adjustRotationToConnect(portalholder, otherPortal);

                    //otherPortal.reloadAndSyncToClient();
                    //portalholder.reloadAndSyncToClient();
                }
            } else {
                world.playSound(null, user.getPos().getX(), user.getPos().getY(), user.getPos().getZ(), PortalCubedSounds.INVALID_PORTAL_EVENT, SoundCategory.NEUTRAL, .3F, 1F);
            }

            assert portalholder != null;
            portalsTag.putUuid((leftClick ? "Left" : "Right") + "Portal", portalholder.getUuid());
            //assert portalOutline != null;
            //portalsTag.putUuid((leftClick ? "Left" : "Right") + "Background", portalOutline.getUuid());

            tag.put(world.getRegistryKey().toString(), portalsTag);


        }
        return TypedActionResult.pass(stack);
    }

    public static Optional<ExperimentalPortal> getPotentialOpposite(World world, Vec3d portalPos, @Nullable ExperimentalPortal ignore, int color, boolean includePlayerPortals) {
        return world.getEntitiesByType(
            PortalCubedEntities.EXPERIMENTAL_PORTAL,
            Box.of(portalPos, 256, 256, 256),
            p ->
                p != ignore &&
                    p.getColor() == 0xffffff - color + 1 &&
                    (includePlayerPortals || CalledValues.getPlayer(p) == null) &&
                    !p.getActive()
        ).stream().min(Comparator.comparingDouble(p -> p.getOriginPos().squaredDistanceTo(portalPos)));
    }

    public static void linkPortals(ExperimentalPortal portal1, ExperimentalPortal portal2, float volume) {
        CalledValues.setDestination(portal1,portal2.getOriginPos().add(portal2.getFacingDirection().getUnitVector().getX()*.1,portal2.getFacingDirection().getUnitVector().getY()*.1,portal2.getFacingDirection().getUnitVector().getZ()*.1));
        CalledValues.setOtherFacing(portal1,new Vec3d(portal2.getFacingDirection().getUnitVector().getX(),portal2.getFacingDirection().getUnitVector().getY(),portal2.getFacingDirection().getUnitVector().getZ()));
        CalledValues.setOtherAxisH(portal1,CalledValues.getAxisH(portal2));
        CalledValues.setDestination(portal2,portal1.getOriginPos().add(portal1.getFacingDirection().getUnitVector().getX()*.1,portal1.getFacingDirection().getUnitVector().getY()*.1,portal1.getFacingDirection().getUnitVector().getZ()*.1));
        CalledValues.setOtherFacing(portal2,new Vec3d(portal1.getFacingDirection().getUnitVector().getX(),portal1.getFacingDirection().getUnitVector().getY(),portal1.getFacingDirection().getUnitVector().getZ()));
        CalledValues.setOtherAxisH(portal2,CalledValues.getAxisH(portal1));
        //CalledValues.setOtherAxisH();
        //portal1.setDestination(portal2.getOriginPos());
        //portal2.setDestination(portal1.getOriginPos());
        portal1.setLinkedPortalUuid(portal2.getUuidAsString());
        portal2.setLinkedPortalUuid(portal1.getUuidAsString());

        portal1.getWorld().playSound(null, portal1.getPos().getX(), portal1.getPos().getY(), portal1.getPos().getZ(), PortalCubedSounds.ENTITY_PORTAL_OPEN, SoundCategory.NEUTRAL, volume, 1F);
        portal2.getWorld().playSound(null, portal2.getPos().getX(), portal2.getPos().getY(), portal2.getPos().getZ(), PortalCubedSounds.ENTITY_PORTAL_OPEN, SoundCategory.NEUTRAL, volume, 1F);
    }

    private boolean validPos(World world, Vec3i up, Vec3i right, Vec3d portalPos1){
        Vec3d CalculatedAxisW;
        Vec3d CalculatedAxisH;
        Vec3d posNormal;
        CalculatedAxisW = Vec3d.of(right);
        CalculatedAxisH = Vec3d.of(up).multiply(-1);
        posNormal = CalculatedAxisW.crossProduct(CalculatedAxisH).normalize();
        Direction portalFacing = Direction.fromVector((int) posNormal.getX(), (int) posNormal.getY(), (int) posNormal.getZ());

        BlockPos topBehind = new BlockPos(
                portalPos1.getX() - CalculatedAxisW.crossProduct(CalculatedAxisH).getX(),
                portalPos1.getY() - CalculatedAxisW.crossProduct(CalculatedAxisH).getY(),
                portalPos1.getZ() - CalculatedAxisW.crossProduct(CalculatedAxisH).getZ());
        BlockPos bottomBehind = new BlockPos(
                portalPos1.getX() - CalculatedAxisW.crossProduct(CalculatedAxisH).getX() - Math.abs(CalculatedAxisH.getX()),
                portalPos1.getY() - CalculatedAxisW.crossProduct(CalculatedAxisH).getY() + CalculatedAxisH.getY(),
                portalPos1.getZ() - CalculatedAxisW.crossProduct(CalculatedAxisH).getZ() - Math.abs(CalculatedAxisH.getZ()));
        BlockPos bottom = new BlockPos(
                portalPos1.getX() - Math.abs(CalculatedAxisH.getX()),
                portalPos1.getY() + CalculatedAxisH.getY(),
                portalPos1.getZ() - Math.abs(CalculatedAxisH.getZ()));


        boolean topValidBlock=false;
        if(world.getBlockState(new BlockPos(portalPos1)).isIn(PortalCubedBlocks.GELCHECKTAG)&&world.getBlockState(topBehind).isIn(PortalCubedBlocks.CANT_PLACE_PORTAL_ON)){
            BooleanProperty booleanProperty = GelFlat.getFacingProperty(portalFacing.getOpposite());
            topValidBlock = world.getBlockState(new BlockPos(portalPos1)).get(booleanProperty);
        }else if (!world.getBlockState(topBehind).isIn(PortalCubedBlocks.CANT_PLACE_PORTAL_ON)){
            topValidBlock=true;
        }
        boolean bottomValidBlock=false;
        if(world.getBlockState(bottom).isIn(PortalCubedBlocks.GELCHECKTAG)&&world.getBlockState(bottomBehind).isIn(PortalCubedBlocks.CANT_PLACE_PORTAL_ON)){
            BooleanProperty booleanProperty = GelFlat.getFacingProperty(portalFacing.getOpposite());
            bottomValidBlock = world.getBlockState(bottom).get(booleanProperty);
        }else if (!world.getBlockState(bottomBehind).isIn(PortalCubedBlocks.CANT_PLACE_PORTAL_ON)){
            bottomValidBlock=true;
        }

        //System.out.println("portalInvalid");
        return (world.getBlockState(topBehind).isSideSolidFullSquare(world, topBehind, portalFacing)) &&
                (world.getBlockState(bottomBehind).isSideSolidFullSquare(world, bottomBehind, portalFacing) &&
                        topValidBlock &&
                        bottomValidBlock) &&
                ((world.getBlockState(new BlockPos(portalPos1)).isAir()) || world.getBlockState(new BlockPos(portalPos1)).isIn(PortalCubedBlocks.ALLOW_PORTAL_IN)) && (world.getBlockState(bottom).isAir() || world.getBlockState(bottom).isIn(PortalCubedBlocks.ALLOW_PORTAL_IN));
    }

    /**
     * @param hit          the position designated by the player's input for a given portal.
     * @param upright      the upright axial vector of the portal based on placement context.
     * @param facing       the facing axial vector of the portal based on placement context.
     * @param cross        the cross product of upright x facing.
     * @param isBackground whether or not this is positioning for a {@link }
     * @return a vector position specifying the portal's final position in the world.
     */
    private Vec3d calcPos(BlockPos hit, Vec3i upright, Vec3i facing, Vec3i cross, boolean isBackground) {
        double upOffset = isBackground ? -1.0 : -0.5;
        double faceOffset = isBackground ? -0.508 : -0.510;
        double crossOffset = 0.0;
        return new Vec3d(
                ((hit.getX() + 0.5) + upOffset * upright.getX() + faceOffset * facing.getX() + crossOffset * cross.getX()), // x component
                ((hit.getY() + 0.5) + upOffset * upright.getY() + faceOffset * facing.getY() + crossOffset * cross.getY()), // y component
                ((hit.getZ() + 0.5) + upOffset * upright.getZ() + faceOffset * facing.getZ() + crossOffset * cross.getZ())  // z component
        );
    }


    public HitResult customRaycast(Entity user, double maxDistance, float tickDelta, boolean includeFluids) {
        Vec3d vec3d = user.getCameraPosVec(tickDelta);
        Vec3d vec3d2 = user.getRotationVec(tickDelta);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);
        return user.world
                .raycast(
                        new RaycastContext(
                                vec3d,
                                vec3d3,
                                RaycastContext.ShapeType.COLLIDER,
                                includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
                                user
                        )
                );
    }

    public static IPQuaternion getPortalOrientationQuaternion(
            Vec3d axisW, Vec3d axisH
    ) {
        Vec3d normal = axisW.crossProduct(axisH);

        return IPQuaternion.matrixToQuaternion(axisW, axisH, normal);
    }

    //public static IPQuaternion getPortalOrientationQuaternion(
    //        Vec3d axisW, Vec3d axisH
    //) {
    //    Vec3f normal = new Vec3f((float)axisW.getX(),(float)axisW.getY(),(float)axisW.getZ());
    //    normal.cross(new Vec3f((float)axisW.getX(),(float)axisW.getY(),(float)axisW.getZ()));
    //    Vec3d aW = new Vec3d(axisW.getX(),axisW.getY(),axisW.getZ());
    //    Vec3d aH = new Vec3d(axisH.getX(),axisH.getY(),axisH.getZ());
    //    Vec3d aN = new Vec3d(normal.getX(),normal.getY(),normal.getZ());
//
    //    return IPQuaternion.matrixToQuaternion(aW, aH, aN);
    //}

}