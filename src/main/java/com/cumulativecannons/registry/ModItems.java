package com.cumulativecannons.registry;

import com.cumulativecannons.CumulativeCannons;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlockItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CumulativeCannons.MOD_ID);

    public static final DeferredItem<ProjectileBlockItem> CUMULATIVE_SHELL = ITEMS.register(
            "cumulative_shell",
            () -> new ProjectileBlockItem(ModBlocks.CUMULATIVE_SHELL.get(), new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<BlockItem> SMOKE_TEST_1 = ITEMS.register(
            "smoke_test_1",
            () -> new BlockItem(ModBlocks.SMOKE_TEST_1.get(), new Item.Properties())
    );

    public static final DeferredItem<BlockItem> SMOKE_TEST_2 = ITEMS.register(
            "smoke_test_2",
            () -> new BlockItem(ModBlocks.SMOKE_TEST_2.get(), new Item.Properties())
    );

    public static final DeferredItem<BlockItem> SMOKE_TEST_3 = ITEMS.register(
            "smoke_test_3",
            () -> new BlockItem(ModBlocks.SMOKE_TEST_3.get(), new Item.Properties())
    );

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {
    }
}
