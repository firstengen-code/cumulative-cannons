package com.cumulativecannons.jet;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class JetMaterialReloadListener extends SimpleJsonResourceReloadListener {
    public JetMaterialReloadListener() {
        super(new Gson(), "cumulative_jet_materials");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, Double> costs = new LinkedHashMap<>();
        for (JsonElement root : files.values()) {
            if (!root.isJsonObject()) {
                continue;
            }
            JsonObject rootObject = root.getAsJsonObject();
            JsonObject blocks = rootObject.has("blocks") && rootObject.get("blocks").isJsonObject()
                    ? rootObject.getAsJsonObject("blocks")
                    : rootObject;
            for (Map.Entry<String, JsonElement> entry : blocks.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    costs.put(entry.getKey(), value.getAsDouble());
                }
            }
        }
        JetMaterialCosts.setCustomCosts(costs);
        JetMaterialCosts.loadBlockOverrides();
    }
}
