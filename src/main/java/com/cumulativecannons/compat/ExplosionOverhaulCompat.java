package com.cumulativecannons.compat;

import com.cumulativecannons.CumulativeCannons;
import com.cumulativecannons.network.EoDeafnessPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class ExplosionOverhaulCompat {
    private static final String MOD_ID = "explosionoverhaul";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean registered;

    private ExplosionOverhaulCompat() {
    }

    public static boolean isAvailable() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        registerSourceMode();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerSourceMode() {
        registered = false;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> explosionOverhaul = Class.forName("com.vinlanx.explosionoverhaul.ExplosionOverhaul");
            Method getModes = explosionOverhaul.getMethod("getSourceModes");
            Method setModes = explosionOverhaul.getMethod("setSourceModes", Map.class);
            Class<?> modeClass = Class.forName("com.vinlanx.explosionoverhaul.ExplosionOverhaul$ExplosionSourceMode");
            Object noDestruction = Enum.valueOf((Class<? extends Enum>) modeClass.asSubclass(Enum.class), "NO_DESTRUCTION");

            Map<String, Object> modes = new HashMap<>((Map<String, Object>) getModes.invoke(null));
            modes.put(CumulativeCannons.MOD_ID + ":cumulative_shell", noDestruction);
            setModes.invoke(null, modes);
            registered = true;
            LOGGER.info("Explosion Overhaul source mode registered: NO_DESTRUCTION");
        } catch (Throwable t) {
            LOGGER.warn("Could not register Explosion Overhaul source mode", t);
        }
    }

    public static void triggerConcussion(ServerLevel level, Vec3 pos) {
        if (!isAvailable()) {
            return;
        }
        Constructor<?> concussionPacket = null;
        try {
            Class<?> packetClass = Class.forName("com.vinlanx.explosionoverhaul.StartConcussionPacket");
            concussionPacket = packetClass.getConstructor(float.class, double.class, boolean.class, boolean.class);
        } catch (Throwable t) {
            LOGGER.warn("Could not find Explosion Overhaul concussion packet", t);
        }

        for (ServerPlayer player : level.players()) {
            if (player.connection == null) {
                continue;
            }
            double distance = player.position().distanceTo(pos);
            if (distance > 10.0D) {
                continue;
            }
            boolean lineOfSight = level.clip(new ClipContext(player.getEyePosition(), pos,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;

            if (distance <= 5.0D) {
                if (concussionPacket == null) {
                    continue;
                }
                try {
                    Object packet = concussionPacket.newInstance(20.0F, distance, lineOfSight, false);
                    PacketDistributor.sendToPlayer(player, (CustomPacketPayload) packet);
                } catch (Throwable t) {
                    LOGGER.warn("Could not send Explosion Overhaul concussion", t);
                }
                continue;
            }

            double t = (distance - 5.0D) / 5.0D;
            float intensity = (float) Math.max(0.15D, 0.75D * (1.0D - t));
            double silentSeconds = 3.0D + 3.0D * intensity;
            double effectivePercent = Math.max(0.0D, (1.0D - distance / 10.0D) * 100.0D);
            String visibility = lineOfSight ? "clear" : "occluded";
            int intensityPercent = Math.round(intensity * 100.0F);
            PacketDistributor.sendToPlayer(player, new EoDeafnessPayload(intensity, silentSeconds,
                    effectivePercent, visibility, intensityPercent));
        }
    }
}
