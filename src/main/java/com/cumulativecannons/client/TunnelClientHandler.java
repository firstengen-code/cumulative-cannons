package com.cumulativecannons.client;

import com.cumulativecannons.CumulativeCannons;
import com.cumulativecannons.network.TunnelBreakPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import team.creative.littletiles.common.block.mc.BlockTile;

@EventBusSubscriber(modid = CumulativeCannons.MOD_ID, value = Dist.CLIENT)
public final class TunnelClientHandler {
    private TunnelClientHandler() {
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onAttack(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.player.isShiftKeyDown()) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit)) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (minecraft.level.getBlockState(pos).getBlock() instanceof BlockTile) {
            PacketDistributor.sendToServer(new TunnelBreakPayload(pos));
        }
    }
}
