package com.cumulativecannons.registry;

import com.cumulativecannons.CumulativeCannons;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CumulativeCannons.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> JET_FLASH =
            PARTICLE_TYPES.register("jet_flash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> JET_SHOCKWAVE =
            PARTICLE_TYPES.register("jet_shockwave", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> JET_DARK_SMOKE =
            PARTICLE_TYPES.register("jet_dark_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> JET_SMOKE =
            PARTICLE_TYPES.register("jet_smoke", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> JET_SPALL =
            PARTICLE_TYPES.register("jet_spall", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> JET_FRAGMENT =
            PARTICLE_TYPES.register("jet_fragment", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }

    private ModParticles() {
    }
}
