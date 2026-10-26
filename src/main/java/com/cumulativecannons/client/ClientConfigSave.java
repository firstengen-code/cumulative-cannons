package com.cumulativecannons.client;

import com.cumulativecannons.Config;
import com.cumulativecannons.CumulativeCannons;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

@EventBusSubscriber(modid = CumulativeCannons.MOD_ID, value = Dist.CLIENT)
public final class ClientConfigSave {
    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof ConfigurationScreen) {
            Config.SPEC.save();
        }
    }

    private ClientConfigSave() {
    }
}
