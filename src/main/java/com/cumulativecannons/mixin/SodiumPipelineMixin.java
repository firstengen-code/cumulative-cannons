package com.cumulativecannons.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import net.minecraft.core.BlockPos;

import com.cumulativecannons.client.compat.SableLightBridge;
import com.cumulativecannons.compat.SableCompat;

import team.creative.littletiles.client.render.cache.build.RenderingBlockContext;
import team.creative.creativecore.common.util.type.map.ChunkLayerMap;
import team.creative.creativecore.common.util.type.list.SingletonList;

/**
 * LittleTiles takes light for the blocks it renders from the Level it receives. Inside a Sable contraption,
 * the block coordinates are local to the composition, and light in the real world is looked up at different
 * coordinates, which is why the carved tiles turned gray. We swap in a wrapper that fixes up the coordinates.
 */
@Pseudo
@Mixin(targets = "team.creative.littletiles.client.mod.sodium.pipeline.LittleRenderPipelineSodium", remap = false)
public class SodiumPipelineMixin {

    @ModifyArgs(
            method = "buildCache(Lcom/mojang/blaze3d/vertex/PoseStack;Lteam/creative/creativecore/common/util/type/map/ChunkLayerMap;Lteam/creative/littletiles/client/render/cache/build/RenderingBlockContext;Lcom/mojang/blaze3d/vertex/VertexFormat;Lteam/creative/creativecore/common/util/type/list/SingletonList;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lteam/creative/littletiles/client/mod/sodium/pipeline/LittleLightDataAccess;prepare(Lnet/minecraft/world/level/BlockAndTintGetter;)V",
                    remap = false),
            require = 0,
            remap = false)
    private void cc$bridgeLightAccess(Args args, PoseStack poseStack, ChunkLayerMap<?> buffers,
            RenderingBlockContext context, VertexFormat format, SingletonList<?> quads) {
        Object raw = args.get(0);
        if (raw instanceof Level level
                && context.be != null
                && SableCompat.isSubLevel(level, context.be.getBlockPos())) {
            args.set(0, new SableLightBridge(level));
        }
    }
}
