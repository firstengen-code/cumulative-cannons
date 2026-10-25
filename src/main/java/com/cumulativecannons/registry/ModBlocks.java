package com.cumulativecannons.registry;

import com.cumulativecannons.CumulativeCannons;
import com.cumulativecannons.munitions.CumulativeShellBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CumulativeCannons.MOD_ID);

    public static final DeferredBlock<CumulativeShellBlock> CUMULATIVE_SHELL = BLOCKS.register(
            "cumulative_shell",
            () -> new CumulativeShellBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion())
    );

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private ModBlocks() {
    }
}
