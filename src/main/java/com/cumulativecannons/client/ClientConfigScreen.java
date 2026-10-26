package com.cumulativecannons.client;

import com.cumulativecannons.CumulativeCannons;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(modid = CumulativeCannons.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientConfigScreen {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModList.get().getModContainerById(CumulativeCannons.MOD_ID).ifPresent(container ->
                container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new));
    }

    private ClientConfigScreen() {
    }
}
