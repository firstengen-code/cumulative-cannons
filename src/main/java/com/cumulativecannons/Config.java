package com.cumulativecannons;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue JET_BUDGET;
    public static final ModConfigSpec.IntValue JET_MAX_BLOCKS;
    public static final ModConfigSpec.IntValue JET_LAYERS_PER_TICK;
    public static final ModConfigSpec.BooleanValue JET_CARVE_ENABLED;
    public static final ModConfigSpec.BooleanValue JET_RESPECT_GRIEF;
    public static final ModConfigSpec.BooleanValue JET_RESPECT_PROTECTION;
    public static final ModConfigSpec.DoubleValue JET_DEFAULT_COST;
    public static final ModConfigSpec.BooleanValue JET_EFFECTS_ENABLED;
    public static final ModConfigSpec.DoubleValue JET_EFFECT_MULTIPLIER;
    public static final ModConfigSpec.IntValue JET_EFFECT_LIMIT;
    public static final ModConfigSpec.BooleanValue JET_CAMERA_SHAKE;
    public static final ModConfigSpec.DoubleValue JET_SHAKE_STRENGTH;
    public static final ModConfigSpec.BooleanValue JET_IMPACT_LIGHT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("jet");
        JET_BUDGET = builder
                .comment("Total penetration budget of one cumulative jet.")
                .defineInRange("budget", 160.0, 1.0, 100000.0);
        JET_MAX_BLOCKS = builder
                .comment("Hard limit of blocks the jet can travel through.")
                .defineInRange("max_blocks", 25, 1, 64);
        JET_LAYERS_PER_TICK = builder
                .comment("How many 1/16 layers are carved per tick. Lower values reduce server load.")
                .defineInRange("layers_per_tick", 128, 1, 512);
        JET_CARVE_ENABLED = builder
                .comment("Set to false to disable the jet tunnel entirely.")
                .define("carve_enabled", true);
        JET_RESPECT_GRIEF = builder
                .comment("Respect Create Big Cannons' NO_DAMAGE grief state.")
                .define("respect_grief", true);
        JET_RESPECT_PROTECTION = builder
                .comment("Stop at unbreakable blocks such as bedrock.")
                .define("respect_protection", true);
        JET_DEFAULT_COST = builder
                .comment("Fallback cost per block for blocks not listed in the material JSON.")
                .defineInRange("default_cost", 2.0, 0.0, 1000.0);
        JET_EFFECTS_ENABLED = builder
                .comment("Enable the impact flash, jet sparks, smoke and exit debris.")
                .define("effects_enabled", true);
        JET_EFFECT_MULTIPLIER = builder
                .comment("Multiplier for the number of effect particles.")
                .defineInRange("effect_multiplier", 1.0, 0.0, 5.0);
        JET_EFFECT_LIMIT = builder
                .comment("Absolute cap of effect particles spawned per tick.")
                .defineInRange("effect_limit", 512, 0, 4096);
        JET_CAMERA_SHAKE = builder
                .comment("Send a short camera shake to nearby players on impact.")
                .define("camera_shake", true);
        JET_SHAKE_STRENGTH = builder
                .comment("Camera shake strength multiplier.")
                .defineInRange("camera_shake_strength", 1.0, 0.0, 5.0);
        JET_IMPACT_LIGHT = builder
                .comment("Place a temporary light block at the entry and exit for two ticks.")
                .define("impact_light", true);
        builder.pop();

        SPEC = builder.build();
    }

    private Config() {
    }
}
