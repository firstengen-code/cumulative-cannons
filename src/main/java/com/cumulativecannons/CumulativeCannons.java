package com.cumulativecannons;

import com.cumulativecannons.compat.ExplosionOverhaulCompat;
import com.cumulativecannons.jet.JetMaterialReloadListener;
import com.cumulativecannons.network.ModPayloads;
import com.cumulativecannons.registry.ModBlockEntities;
import com.cumulativecannons.registry.ModBlocks;
import com.cumulativecannons.registry.ModEffects;
import com.cumulativecannons.registry.ModEntities;
import com.cumulativecannons.registry.ModItems;
import com.cumulativecannons.registry.ModParticles;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers;
import rbasamoyai.createbigcannons.munitions.config.MunitionPropertiesHandler;

@Mod(CumulativeCannons.MOD_ID)
public class CumulativeCannons {
    public static final String MOD_ID = "cumulative_cannons";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CumulativeCannons(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModParticles.register(modEventBus);
        ModEffects.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(ModPayloads::register);
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        NeoForge.EVENT_BUS.addListener(this::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(ExplosionOverhaulCompat::onServerStarted);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MunitionPropertiesHandler.registerProjectileHandler(
                    ModEntities.CUMULATIVE_SHELL.get(),
                    CBCMunitionPropertiesHandlers.INERT_BIG_CANNON_PROJECTILE
            );
            LOGGER.info("Cumulative Cannons loaded");
            LOGGER.info("Create present: {}", ModList.get().isLoaded("create"));
            LOGGER.info("Create Big Cannons present: {}", ModList.get().isLoaded("createbigcannons"));
            LOGGER.info("LittleTiles present: {}", ModList.get().isLoaded("littletiles"));
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(new ItemStack(ModItems.CUMULATIVE_SHELL.get()));
        }
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new JetMaterialReloadListener());
    }
}
