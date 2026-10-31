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
 * Same coordinate fix, but for the rendering path without Sodium: NeoForge's smoother also
 * receives the Level directly, and it must also get the wrapper when it is a composition.
 */
@Pseudo
@Mixin(targets = "team.creative.littletiles.client.render.cache.pipeline.LittleRenderPipelineForge", remap = false)
public class ForgePipelineMixin {

    @ModifyArgs(
            method = "buildCache(Lcom/mojang/blaze3d/vertex/PoseStack;Lteam/creative/creativecore/common/util/type/map/ChunkLayerMap;Lteam/creative/littletiles/client/render/cache/build/RenderingBlockContext;Lcom/mojang/blaze3d/vertex/VertexFormat;Lteam/creative/creativecore/common/util/type/list/SingletonList;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/model/lighting/QuadLighter;setup(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
                    remap = false),
            require = 0,
            remap = false)
    private void cc$bridgeQuadLighter(Args args, PoseStack poseStack, ChunkLayerMap<?> buffers,
            RenderingBlockContext context, VertexFormat format, SingletonList<?> quads) {
        Object raw = args.get(0);
        if (raw instanceof Level level
                && context.be != null
                && SableCompat.isSubLevel(level, context.be.getBlockPos())) {
            args.set(0, new SableLightBridge(level));
        }
    }
}
