package com.cumulativecannons.registry;

import com.cumulativecannons.CumulativeCannons;
import com.cumulativecannons.effect.BleedingEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, CumulativeCannons.MOD_ID);

    public static final DeferredHolder<MobEffect, BleedingEffect> BLEEDING =
            EFFECTS.register("bleeding", BleedingEffect::new);

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private ModEffects() {
    }
}
