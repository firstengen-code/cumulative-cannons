package com.cumulativecannons.client;

import com.cumulativecannons.CumulativeCannons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

@EventBusSubscriber(modid = CumulativeCannons.MOD_ID, value = Dist.CLIENT)
public final class ClientConfigEvents {
    private ClientConfigEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ConfigurationScreen screen)) {
            return;
        }
        event.addListener(Button.builder(
                        Component.translatable("cumulative_cannons.materials.button"),
                        button -> Minecraft.getInstance().setScreen(new BlockPenetrationScreen(screen)))
                .bounds(screen.width - 154, 8, 146, 20)
                .build());
    }
}
