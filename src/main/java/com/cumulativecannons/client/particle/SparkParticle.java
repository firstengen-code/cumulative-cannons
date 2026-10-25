package com.cumulativecannons.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SparkParticle extends TextureSheetParticle {
    private final boolean trail;
    private final float width;
    private final float baseRed;
    private final float baseGreen;
    private final float baseBlue;
    private final Vec3 initialDirection;
    private final List<Vec3> points = new ArrayList<>();
    private final int maxPoints;

    public SparkParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites,
            float width,
            int lifetime,
            boolean trail,
            float red,
            float green,
            float blue
    ) {
        super(level, x, y, z);
        this.setSprite(sprites.get(this.random));
        this.setParticleSpeed(xSpeed, ySpeed, zSpeed);
        this.width = width;
        this.lifetime = lifetime;
        this.trail = trail;
        this.maxPoints = trail ? 5 : 0;
        this.baseRed = red;
        this.baseGreen = green;
        this.baseBlue = blue;
        this.hasPhysics = false;
        this.friction = trail ? 0.8F : 0.55F;
        this.gravity = trail ? 0.03F : 0.02F;
        this.alpha = 1.0F;
        this.setColor(red, green, blue);
        Vec3 direction = new Vec3(xSpeed, ySpeed, zSpeed);
        this.initialDirection = direction.lengthSqr() > 1.0E-6D ? direction.normalize() : new Vec3(0.0D, 1.0D, 0.0D);
        if (trail) {
            this.points.add(new Vec3(x, y, z));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float progress = this.age / (float) this.lifetime;
        this.alpha = 1.0F - progress * progress;
        this.setColor(
                this.baseRed,
                this.baseGreen * (1.0F - progress * 0.55F),
                this.baseBlue * (1.0F - progress * 0.8F)
        );
        if (this.trail) {
            this.points.add(new Vec3(this.x, this.y, this.z));
            while (this.points.size() > this.maxPoints) {
                this.points.remove(0);
            }
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        if (!this.trail || this.points.size() < 2) {
            Vec3 tail = new Vec3(this.xo, this.yo, this.zo);
            Vec3 head = new Vec3(this.x, this.y, this.z);
            if (tail.distanceToSqr(head) < 1.0E-6D) {
                tail = head.subtract(this.initialDirection.scale(0.35D));
            }
            this.emitSegment(buffer, camera, tail, head, this.width, this.alpha);
            return;
        }

        int segments = this.points.size() - 1;
        for (int i = 0; i < segments; i++) {
            float t = (i + 1.0F) / segments;
            float taper = 0.3F + 0.7F * t;
            float segmentAlpha = this.alpha * (0.18F + 0.82F * t);
            this.emitSegment(buffer, camera, this.points.get(i), this.points.get(i + 1), this.width * taper, segmentAlpha);
        }
    }

    private void emitSegment(VertexConsumer buffer, Camera camera, Vec3 start, Vec3 end, float halfWidth, float alphaValue) {
        Vec3 a = start.subtract(camera.getPosition());
        Vec3 b = end.subtract(camera.getPosition());
        Vec3 direction = b.subtract(a);
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }
        Vec3 view = a.add(b).scale(0.5D).normalize();
        Vec3 right = direction.cross(view);
        if (right.lengthSqr() < 1.0E-8D) {
            return;
        }
        right = right.normalize().scale(halfWidth);

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = LightTexture.FULL_BRIGHT;

        buffer.addVertex((float) (a.x - right.x), (float) (a.y - right.y), (float) (a.z - right.z))
                .setUv(u0, v1)
                .setColor(this.rCol, this.gCol, this.bCol, alphaValue)
                .setLight(light);
        buffer.addVertex((float) (a.x + right.x), (float) (a.y + right.y), (float) (a.z + right.z))
                .setUv(u1, v1)
                .setColor(this.rCol, this.gCol, this.bCol, alphaValue)
                .setLight(light);
        buffer.addVertex((float) (b.x + right.x), (float) (b.y + right.y), (float) (b.z + right.z))
                .setUv(u1, v0)
                .setColor(this.rCol, this.gCol, this.bCol, alphaValue)
                .setLight(light);
        buffer.addVertex((float) (b.x - right.x), (float) (b.y - right.y), (float) (b.z - right.z))
                .setUv(u0, v0)
                .setColor(this.rCol, this.gCol, this.bCol, alphaValue)
                .setLight(light);
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        double minX = this.x;
        double minY = this.y;
        double minZ = this.z;
        double maxX = this.x;
        double maxY = this.y;
        double maxZ = this.z;
        for (Vec3 point : this.points) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }
        return new AABB(minX - 1.0D, minY - 1.0D, minZ - 1.0D, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return JetParticleRenderTypes.ADDITIVE;
    }
}
