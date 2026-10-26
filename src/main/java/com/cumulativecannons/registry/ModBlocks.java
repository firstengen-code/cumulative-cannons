package com.cumulativecannons.registry;

import com.cumulativecannons.CumulativeCannons;
import com.cumulativecannons.block.SmokeTestBlock;
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

    public static final DeferredBlock<SmokeTestBlock> SMOKE_TEST_1 = BLOCKS.register(
            "smoke_test_1",
            () -> new SmokeTestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.5F)
                    .sound(SoundType.STONE), SmokeTestBlock.SmokeType.WHITE)
    );

    public static final DeferredBlock<SmokeTestBlock> SMOKE_TEST_2 = BLOCKS.register(
            "smoke_test_2",
            () -> new SmokeTestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(0.5F)
                    .sound(SoundType.STONE), SmokeTestBlock.SmokeType.BLACK)
    );

    public static final DeferredBlock<SmokeTestBlock> SMOKE_TEST_3 = BLOCKS.register(
            "smoke_test_3",
            () -> new SmokeTestBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(0.5F)
                    .sound(SoundType.STONE), SmokeTestBlock.SmokeType.CLOUD)
    );

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private ModBlocks() {
    }
}
