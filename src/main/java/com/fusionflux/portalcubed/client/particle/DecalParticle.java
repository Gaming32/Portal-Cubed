package com.fusionflux.portalcubed.client.particle;

import com.fusionflux.portalcubed.PortalCubed;
import com.fusionflux.portalcubed.particle.DecalParticleEffect;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteProvider;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.quiltmc.loader.api.minecraft.ClientOnly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ClientOnly
public class DecalParticle extends Particle {
    private final Sprite sprite;
    private final Direction direction;

    public DecalParticle(
        ClientWorld world,
        double x, double y, double z,
        Sprite sprite, Direction direction
    ) {
        super(world, x, y, z);
        this.sprite = sprite;
        this.direction = direction;
        maxAge = 1400;
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        final Vec3d cameraPos = camera.getPos();
        final float x = (float)(MathHelper.lerp(tickDelta, prevPosX, this.x) - cameraPos.getX());
        final float y = (float)(MathHelper.lerp(tickDelta, prevPosY, this.y) - cameraPos.getY());
        final float z = (float)(MathHelper.lerp(tickDelta, prevPosZ, this.z) - cameraPos.getZ());
        final Quaternion rotation = direction.getRotationQuaternion();

        final Vec3f[] vertices = {
            new Vec3f(-1f, 0f, -1f),
            new Vec3f(-1f, 0f, 1f),
            new Vec3f(1f, 0f, 1f),
            new Vec3f(1f, 0f, -1f)
        };

        for (final Vec3f vertex : vertices) {
            vertex.rotate(rotation);
            vertex.add(x, y, z);
        }

        colorAlpha = 1f;
        if (age + 100 >= maxAge) {
            final float past100 = (age + tickDelta) - maxAge + 100;
            colorAlpha = 1f - MathHelper.clamp(past100 / 100f, 0f, 1f);
        }

        final float minU = sprite.getMinU();
        final float maxU = sprite.getMaxU();
        final float minV = sprite.getMinV();
        final float maxV = sprite.getMaxV();
        final int brightness = getBrightness(tickDelta);
        vertexConsumer.vertex(vertices[0].getX(), vertices[0].getY(), vertices[0].getZ())
            .uv(maxU, maxV)
            .color(colorRed, colorGreen, colorBlue, colorAlpha)
            .light(brightness)
            .next();
        vertexConsumer.vertex(vertices[1].getX(), vertices[1].getY(), vertices[1].getZ())
            .uv(maxU, minV)
            .color(colorRed, colorGreen, colorBlue, colorAlpha)
            .light(brightness)
            .next();
        vertexConsumer.vertex(vertices[2].getX(), vertices[2].getY(), vertices[2].getZ())
            .uv(minU, minV)
            .color(colorRed, colorGreen, colorBlue, colorAlpha)
            .light(brightness)
            .next();
        vertexConsumer.vertex(vertices[3].getX(), vertices[3].getY(), vertices[3].getZ())
            .uv(minU, maxV)
            .color(colorRed, colorGreen, colorBlue, colorAlpha)
            .light(brightness)
            .next();
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @ClientOnly
    public static class Factory implements ParticleFactory<DecalParticleEffect> {
        private final FabricSpriteProvider spriteProvider;

        private List<Sprite> cacheKey;
        private final Map<Identifier, Sprite> spriteCache = new HashMap<>();

        public Factory(FabricSpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DecalParticleEffect parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            final Sprite sprite = getSpriteCache().get(parameters.getTexture());
            if (sprite == null) {
                PortalCubed.LOGGER.warn("Unknown decal particle texture {}", parameters.getTexture());
                return null;
            }
            return new DecalParticle(world, x, y, z, sprite, parameters.getDirection());
        }

        private Map<Identifier, Sprite> getSpriteCache() {
            final List<Sprite> sprites = spriteProvider.getSprites();
            if (sprites != cacheKey) {
                cacheKey = sprites;
                spriteCache.clear();
                for (final Sprite sprite : sprites) {
                    spriteCache.put(sprite.getId(), sprite);
                }
            }
            return spriteCache;
        }
    }
}