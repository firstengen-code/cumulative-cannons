package com.cumulativecannons.jet;

import com.cumulativecannons.Config;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class JetMaterialCosts {
    private static final Map<String, Double> DEFAULTS = defaults();
    private static volatile Map<String, Double> custom = Collections.emptyMap();

    public static Map<String, Double> defaultCosts() {
        return new LinkedHashMap<>(DEFAULTS);
    }

    public static void setCustomCosts(Map<String, Double> costs) {
        custom = Collections.unmodifiableMap(new LinkedHashMap<>(costs));
    }

    public static void resetCustomCosts() {
        custom = Collections.emptyMap();
    }

    public static double getCost(BlockState state) {
        Double customCost = find(custom, state);
        if (customCost != null) {
            return customCost;
        }
        Double defaultCost = find(DEFAULTS, state);
        if (defaultCost != null) {
            return defaultCost;
        }
        return Config.JET_DEFAULT_COST.get();
    }

    private static Double find(Map<String, Double> costs, BlockState state) {
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        Double exact = costs.get(blockId);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, Double> entry : costs.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("#")) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(key.substring(1));
            if (id == null) {
                continue;
            }
            if (state.is(TagKey.create(Registries.BLOCK, id))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Map<String, Double> defaults() {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("#minecraft:logs", 1.0);
        map.put("#minecraft:planks", 1.0);
        map.put("#minecraft:wooden_slabs", 1.0);
        map.put("#minecraft:leaves", 0.5);
        map.put("#minecraft:dirt", 0.5);
        map.put("#minecraft:sand", 0.75);
        map.put("#minecraft:base_stone_overworld", 2.0);
        map.put("#minecraft:base_stone_nether", 2.0);
        map.put("#minecraft:stone_bricks", 2.0);
        map.put("minecraft:iron_block", 6.0);
        map.put("minecraft:copper_block", 4.0);
        map.put("minecraft:netherite_block", 20.0);
        map.put("createbigcannons:cast_iron_block", 6.0);
        map.put("createbigcannons:steel_block", 6.0);
        map.put("createbigcannons:nethersteel_block", 8.0);
        map.put("minecraft:obsidian", 16.0);
        map.put("minecraft:crying_obsidian", 16.0);
        map.put("minecraft:ancient_debris", 16.0);
        map.put("minecraft:deepslate", 3.0);
        return Collections.unmodifiableMap(map);
    }

    private JetMaterialCosts() {
    }
}
