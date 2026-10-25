package com.cumulativecannons.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;

public class ShockwaveParticle extends TextureSheetParticle {
    private final float startSize;
    private final float endSize;

    public ShockwaveParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, int lifetime) {
        super(level, x, y, z);
        this.setSprite(sprites.get(this.random));
        this.startSize = 0.25F;
        this.endSize = 2.1F;
        this.quadSize = this.startSize;
        this.lifetime = lifetime;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.setColor(1.0F, 0.78F, 0.35F);
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
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        this.quadSize = this.startSize + (this.endSize - this.startSize) * eased;
        this.alpha = (1.0F - progress) * 0.75F;
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
