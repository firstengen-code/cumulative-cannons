package com.cumulativecannons.tunnel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import team.creative.littletiles.common.action.LittleActionDestroyBoxes;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.mc.BlockTile;
import team.creative.littletiles.common.math.box.LittleBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes a jet-carved LittleTiles block break as a whole on a normal left click,
 * matching the vanilla "sneak to break the whole block" behavior.
 */
public final class TunnelBreakHandler {
    private TunnelBreakHandler() {
    }

    public static void breakTracked(ServerLevel level, BlockPos pos, ServerPlayer player) {
        TunnelTracker tracker = TunnelTracker.get(level);
        if (!tracker.contains(pos)) {
            return;
        }
        if (tryBreakWhole(level, pos, player)) {
            tracker.remove(pos);
        }
    }

    private static boolean tryBreakWhole(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockTile)) {
            return false;
        }
        BETiles be = BlockTile.loadBE(level, pos);
        return be != null && !be.isEmpty() && breakWholeBlock(level, pos, player, be);
    }

    private static boolean breakWholeBlock(ServerLevel level, BlockPos pos, ServerPlayer player, BETiles be) {
        try {
            List<LittleBox> boxes = new ArrayList<>();
            for (var pair : be.allTiles()) {
                for (LittleBox box : pair.value) {
                    boxes.add(box.copy());
                }
            }
            if (!boxes.isEmpty()) {
                LittleActionDestroyBoxes.removeBoxes(be, be.getGrid(), boxes);
            }
            if (be.isEmpty()) {
                be.convertBlockToVanilla();
            }
            return be.isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
