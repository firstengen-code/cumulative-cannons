package com.cumulativecannons.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;

public class JetSmokeParticle extends TextureSheetParticle {
    private final boolean dark;
    private final float startSize;

    public JetSmokeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, boolean dark) {
        super(level, x, y, z);
        this.dark = dark;
        this.setSprite(sprites.get(this.random));
        this.startSize = dark ? 0.18F + this.random.nextFloat() * 0.12F : 0.1F + this.random.nextFloat() * 0.08F;
        this.quadSize = this.startSize;
        this.lifetime = dark ? 55 + this.random.nextInt(20) : 65 + this.random.nextInt(20);
        this.hasPhysics = false;
        this.friction = 0.96F;
        this.gravity = -0.002F;
        this.yd += 0.004D + this.random.nextDouble() * 0.004D;
        if (dark) {
            this.setColor(0.12F, 0.12F, 0.13F);
        } else {
            this.setColor(0.72F, 0.74F, 0.76F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float progress = this.age / (float) this.lifetime;
        this.alpha = (1.0F - progress) * (this.dark ? 0.62F : 0.46F);
        this.quadSize = this.startSize * (1.0F + progress * 2.4F);
        this.yd += this.dark ? 0.0012D : 0.0007D;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
