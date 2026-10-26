package com.cumulativecannons.jet;

import com.cumulativecannons.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

public final class JetMaterialCosts {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "cumulative_cannons-materials.json";

    private static final Map<String, Double> DEFAULTS = defaults();
    private static volatile Map<String, Double> custom = Collections.emptyMap();
    private static volatile Map<String, Double> blockOverrides = Collections.emptyMap();

    public static Map<String, Double> defaultCosts() {
        return new LinkedHashMap<>(DEFAULTS);
    }

    public static Map<String, Double> blockOverrides() {
        return new LinkedHashMap<>(blockOverrides);
    }

    public static boolean hasBlockOverride(ResourceLocation id) {
        return blockOverrides.containsKey(id.toString());
    }

    public static Double getBlockOverride(ResourceLocation id) {
        return blockOverrides.get(id.toString());
    }

    public static List<ResourceLocation> defaultExactBlocks() {
        List<ResourceLocation> result = new ArrayList<>();
        for (String key : DEFAULTS.keySet()) {
            if (!key.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) {
                    result.add(id);
                }
            }
        }
        result.sort((a, b) -> a.toString().compareTo(b.toString()));
        return result;
    }

    public static void setCustomCosts(Map<String, Double> costs) {
        custom = Collections.unmodifiableMap(new LinkedHashMap<>(costs));
    }

    public static void resetCustomCosts() {
        custom = Collections.emptyMap();
    }

    public static void setBlockOverride(ResourceLocation id, double cost) {
        Map<String, Double> next = new LinkedHashMap<>(blockOverrides);
        next.put(id.toString(), Math.max(0.0D, cost));
        blockOverrides = Collections.unmodifiableMap(next);
    }

    public static void removeBlockOverride(ResourceLocation id) {
        if (!blockOverrides.containsKey(id.toString())) {
            return;
        }
        Map<String, Double> next = new LinkedHashMap<>(blockOverrides);
        next.remove(id.toString());
        blockOverrides = Collections.unmodifiableMap(next);
    }

    /** Base material cost before the global penetration multiplier. */
    public static double getBaseCost(BlockState state) {
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        Double override = blockOverrides.get(blockId);
        if (override != null) {
            return override;
        }
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

    /** Effective cost used by the jet, including the global multiplier. */
    public static double getCost(BlockState state) {
        double multiplier = Config.JET_PENETRATION_MULTIPLIER.get();
        return Math.max(0.0D, getBaseCost(state) * multiplier);
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

    public static void loadBlockOverrides() {
        Path path = configPath();
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(path));
            if (!parsed.isJsonObject()) {
                return;
            }
            JsonObject root = parsed.getAsJsonObject();
            JsonObject blocks = root.has("blocks") && root.get("blocks").isJsonObject()
                    ? root.getAsJsonObject("blocks") : root;
            Map<String, Double> loaded = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : blocks.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    loaded.put(entry.getKey(), Math.max(0.0D, entry.getValue().getAsDouble()));
                }
            }
            blockOverrides = Collections.unmodifiableMap(loaded);
            LOGGER.info("Loaded {} block penetration overrides from {}", loaded.size(), path);
        } catch (Exception e) {
            LOGGER.warn("Could not read block penetration config {}", path, e);
        }
    }

    public static void saveBlockOverrides() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            JsonObject blocks = new JsonObject();
            blockOverrides.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> blocks.addProperty(entry.getKey(), entry.getValue()));
            JsonObject root = new JsonObject();
            root.add("blocks", blocks);
            Files.writeString(path, GSON.toJson(root));
        } catch (IOException e) {
            LOGGER.warn("Could not save block penetration config {}", path, e);
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
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
