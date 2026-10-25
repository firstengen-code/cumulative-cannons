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
    public static final ModConfigSpec.BooleanValue DAMAGE_ENABLED;
    public static final ModConfigSpec.DoubleValue DIRECT_HIT_DAMAGE;
    public static final ModConfigSpec.DoubleValue JET_DAMAGE;
    public static final ModConfigSpec.DoubleValue SHOCKWAVE_RADIUS;
    public static final ModConfigSpec.DoubleValue SHOCKWAVE_ENTRY_DAMAGE;
    public static final ModConfigSpec.DoubleValue SHOCKWAVE_EXIT_DAMAGE;
    public static final ModConfigSpec.DoubleValue SHRAPNEL_DAMAGE;
    public static final ModConfigSpec.DoubleValue SHRAPNEL_MAX_DAMAGE;
    public static final ModConfigSpec.DoubleValue SHRAPNEL_RADIUS;
    public static final ModConfigSpec.DoubleValue SHRAPNEL_ANGLE;
    public static final ModConfigSpec.IntValue SHRAPNEL_BOUNCES;
    public static final ModConfigSpec.DoubleValue SHRAPNEL_RANGE;
    public static final ModConfigSpec.IntValue BLEED_DURATION;
    public static final ModConfigSpec.IntValue BLEED_INTERVAL;
    public static final ModConfigSpec.DoubleValue BLEED_DAMAGE;

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
                .comment("How many 1/16 layers are carved per tick; 16 layers = one block. Lower values reduce server load.")
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

        builder.push("combat");
        DAMAGE_ENABLED = builder
                .comment("Enable damage from the shell, jet, shockwave and fragments.")
                .define("damage_enabled", true);
        DIRECT_HIT_DAMAGE = builder
                .comment("Damage when the shell itself hits an entity. Uses magic damage, so armor does not stop it.")
                .defineInRange("direct_hit_damage", 300.0, 0.0, 10000.0);
        JET_DAMAGE = builder
                .comment("Damage to entities in the path of the cumulative jet. Uses magic damage; 152 mm jet is lethal.")
                .defineInRange("jet_damage", 500.0, 0.0, 10000.0);
        SHOCKWAVE_RADIUS = builder
                .comment("Radius of the blast wave damage around the entry and exit points.")
                .defineInRange("shockwave_radius", 5.0, 0.0, 32.0);
        SHOCKWAVE_ENTRY_DAMAGE = builder
                .comment("Maximum blast wave damage at the entry point.")
                .defineInRange("shockwave_entry_damage", 50.0, 0.0, 1000.0);
        SHOCKWAVE_EXIT_DAMAGE = builder
                .comment("Maximum blast wave damage at the exit point.")
                .defineInRange("shockwave_exit_damage", 30.0, 0.0, 1000.0);
        SHRAPNEL_DAMAGE = builder
                .comment("Damage of one black fragment hit. Magic damage, so armor does not stop it.")
                .defineInRange("shrapnel_damage", 1.0, 0.0, 1000.0);
        SHRAPNEL_MAX_DAMAGE = builder
                .comment("Maximum total fragment damage per entity from one shell.")
                .defineInRange("shrapnel_max_damage", 4.0, 0.0, 1000.0);
        SHRAPNEL_RADIUS = builder
                .comment("Maximum range of the visual fragment cone.")
                .defineInRange("shrapnel_radius", 7.0, 0.0, 32.0);
        SHRAPNEL_ANGLE = builder
                .comment("Fragment cone half-angle in degrees.")
                .defineInRange("shrapnel_angle", 40.0, 1.0, 90.0);
        SHRAPNEL_BOUNCES = builder
                .comment("How many times fragments can ricochet from blocks inside a tank.")
                .defineInRange("shrapnel_bounces", 2, 0, 5);
        SHRAPNEL_RANGE = builder
                .comment("Maximum travel distance of one fragment after leaving the jet.")
                .defineInRange("shrapnel_range", 8.0, 0.5, 32.0);
        BLEED_DURATION = builder
                .comment("Bleeding duration in ticks (20 ticks = 1 second).")
                .defineInRange("bleeding_duration", 600, 20, 2400);
        BLEED_INTERVAL = builder
                .comment("Bleeding damage interval in ticks (100 ticks = 5 seconds).")
                .defineInRange("bleeding_interval", 100, 20, 1200);
        BLEED_DAMAGE = builder
                .comment("Bleeding damage per interval (1.0 = half a heart).")
                .defineInRange("bleeding_damage", 1.0, 0.0, 100.0);
        builder.pop();

        SPEC = builder.build();
    }

    private Config() {
    }
}
