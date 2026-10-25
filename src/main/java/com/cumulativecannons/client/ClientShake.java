package com.cumulativecannons.client;

import com.cumulativecannons.CumulativeCannons;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = CumulativeCannons.MOD_ID, value = Dist.CLIENT)
public final class ClientShake {
    private static final double MAX_DISTANCE = 32.0D;

    private static int remainingTicks;
    private static int totalTicks;
    private static float baseStrength;
    private static double sourceX;
    private static double sourceY;
    private static double sourceZ;

    public static void trigger(double x, double y, double z, float strength, int duration) {
        if (duration <= 0 || strength <= 0.0F) {
            return;
        }
        if (remainingTicks > 0) {
            baseStrength = Math.max(baseStrength, strength);
            remainingTicks = Math.max(remainingTicks, duration);
            totalTicks = Math.max(totalTicks, duration);
        } else {
            baseStrength = strength;
            remainingTicks = duration;
            totalTicks = duration;
        }
        sourceX = x;
        sourceY = y;
        sourceZ = z;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (remainingTicks <= 0 || totalTicks <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        double distance = minecraft.player.getEyePosition().distanceTo(new Vec3(sourceX, sourceY, sourceZ));
        float distanceFactor = (float) Mth.clamp(1.0D - distance / MAX_DISTANCE, 0.0D, 1.0D);
        float fade = remainingTicks / (float) totalTicks;
        float amplitude = baseStrength * distanceFactor * fade;
        if (amplitude <= 0.0F) {
            return;
        }

        long gameTime = minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        float time = gameTime + (float) event.getPartialTick();
        event.setYaw(event.getYaw() + Mth.sin(time * 2.3F) * amplitude * 1.5F);
        event.setPitch(event.getPitch() + Mth.cos(time * 3.1F) * amplitude);
        event.setRoll(event.getRoll() + Mth.sin(time * 1.7F) * amplitude * 0.8F);
    }

    private ClientShake() {
    }
}
