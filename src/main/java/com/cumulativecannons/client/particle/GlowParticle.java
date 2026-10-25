package com.cumulativecannons.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;

public class GlowParticle extends TextureSheetParticle {
    private final float startSize;

    public GlowParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, float size, int lifetime) {
        super(level, x, y, z);
        this.setSprite(sprites.get(this.random));
        this.startSize = size;
        this.quadSize = size;
        this.lifetime = lifetime;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.setColor(1.0F, 0.92F, 0.72F);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        float progress = this.age / (float) this.lifetime;
        this.alpha = 1.0F - progress * progress;
        this.quadSize = this.startSize * (1.0F + progress * 1.1F);
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
