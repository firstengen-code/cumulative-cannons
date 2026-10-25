package com.cumulativecannons.client;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public final class EoDeafnessClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Method start;
    private static boolean failed;

    private EoDeafnessClient() {
    }

    public static void apply(float intensity, double silentSeconds, double effectivePercent,
                             String visibility, int intensityPercent) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        try {
            if (start == null) {
                Class<?> effect = Class.forName("com.vinlanx.explosionoverhaul.client.DeafnessConcussionEffect");
                start = effect.getMethod("start", float.class, double.class, double.class, String.class, int.class);
            }
            start.invoke(null, intensity, silentSeconds, effectivePercent, visibility, intensityPercent);
        } catch (Throwable t) {
            if (!failed) {
                failed = true;
                LOGGER.warn("Could not apply Explosion Overhaul deafness", t);
            }
        }
    }
}
