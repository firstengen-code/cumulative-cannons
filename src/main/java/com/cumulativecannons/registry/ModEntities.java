package com.cumulativecannons.registry;

import com.cumulativecannons.CumulativeCannons;
import com.cumulativecannons.munitions.CumulativeShellProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CumulativeCannons.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CumulativeShellProjectile>> CUMULATIVE_SHELL =
            ENTITIES.register("cumulative_shell", () -> EntityType.Builder
                    .<CumulativeShellProjectile>of(CumulativeShellProjectile::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .fireImmune()
                    .updateInterval(1)
                    .setTrackingRange(16)
                    .setShouldReceiveVelocityUpdates(false)
                    .build(CumulativeCannons.MOD_ID + ":cumulative_shell"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    private ModEntities() {
    }
}
