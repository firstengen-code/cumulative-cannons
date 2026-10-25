package com.cumulativecannons.registry;

import com.cumulativecannons.CumulativeCannons;
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

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {
    }
}
