package com.cumulativecannons.client;

import com.cumulativecannons.CumulativeCannons;
import com.cumulativecannons.client.particle.GlowParticle;
import com.cumulativecannons.client.particle.JetSmokeParticle;
import com.cumulativecannons.client.particle.ShockwaveParticle;
import com.cumulativecannons.client.particle.SparkParticle;
import com.cumulativecannons.registry.ModEntities;
import com.cumulativecannons.registry.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import rbasamoyai.createbigcannons.munitions.big_cannon.BigCannonProjectileRenderer;

@EventBusSubscriber(modid = CumulativeCannons.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CUMULATIVE_SHELL.get(), BigCannonProjectileRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.JET_FLASH.get(),
                sprites -> (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        new GlowParticle(level, x, y, z, sprites, 0.75F, 3));
        event.registerSpriteSet(ModParticles.JET_SHOCKWAVE.get(),
                sprites -> (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        new ShockwaveParticle(level, x, y, z, sprites, 6));
        event.registerSpriteSet(ModParticles.JET_DARK_SMOKE.get(),
                sprites -> (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        new JetSmokeParticle(level, x, y, z, sprites, true));
        event.registerSpriteSet(ModParticles.JET_SMOKE.get(),
                sprites -> (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        new JetSmokeParticle(level, x, y, z, sprites, false));
        event.registerSpriteSet(ModParticles.JET_SPALL.get(),
                sprites -> (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        new SparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, 0.045F, 7, false, 1.0F, 0.8F, 0.35F));
        event.registerSpriteSet(ModParticles.JET_FRAGMENT.get(),
                sprites -> (type, level, x, y, z, xSpeed, ySpeed, zSpeed) ->
                        new SparkParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, 0.07F, 12, true, 1.0F, 1.0F, 0.9F));
    }

    private ClientEvents() {
    }
}
