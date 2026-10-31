package com.cumulativecannons.client.compat;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import com.cumulativecannons.compat.SableCompat;

/**
 * Stand-in for the Level that LittleTiles receives during the render bake inside a Sable contraption:
 * block queries are passed through to the plot (the composition has its own blocks there), and light
 * queries are directed to the real world position of the plot so that the tiles don't render dark.
 */
public final class SableLightBridge implements BlockAndTintGetter {

    private static final int CACHE_LIMIT = 2048;

    private final Level level;
    private final Map<BlockPos, BlockPos> worldLookup = new HashMap<>();

    public SableLightBridge(Level level) {
        this.level = level;
    }

    private BlockPos toWorld(BlockPos pos) {
        BlockPos imm = pos.immutable();
        BlockPos cached = worldLookup.get(imm);
        if (cached != null) {
            return cached;
        }
        if (worldLookup.size() > CACHE_LIMIT) {
            worldLookup.clear();
        }
        BlockPos world = BlockPos.containing(SableCompat.toWorld(level, Vec3.atCenterOf(imm)));
        worldLookup.put(imm, world);
        return world;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return SableCompat.getBlockState(level, pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return SableCompat.getBlockState(level, pos).getFluidState();
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return level.getBrightness(layer, toWorld(pos));
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return level.getRawBrightness(toWorld(pos), amount);
    }

    @Override
    public boolean canSeeSky(BlockPos pos) {
        return level.canSeeSky(toWorld(pos));
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return level.getBlockTint(toWorld(pos), resolver);
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        // Sable forces vanilla getShade to 1.0 for sublevel section baking (ModelBlockRendererMixin
        // WrapOperation) and re-applies directional shading in its vertex shader
        // (vertexColor.rgb *= block_brightness(...)). If we bake the vanilla directional shade
        // into LT tile vertex colors via QuadLighter too, shading is applied twice -> tiles go dark.
        // Return 1.0 so the shader-side dynamic shading stays the single source of shade.
        return 1.0f;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return level.getBlockEntity(pos);
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return level.getMinBuildHeight();
    }
}
