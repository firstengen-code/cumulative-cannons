package com.cumulativecannons.registry;

import com.cumulativecannons.CumulativeCannons;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rbasamoyai.createbigcannons.munitions.big_cannon.BigCannonProjectileBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CumulativeCannons.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BigCannonProjectileBlockEntity>> CUMULATIVE_SHELL =
            BLOCK_ENTITIES.register("cumulative_shell", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new BigCannonProjectileBlockEntity(ModBlockEntities.CUMULATIVE_SHELL.get(), pos, state),
                    ModBlocks.CUMULATIVE_SHELL.get()
            ).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    private ModBlockEntities() {
    }
}
