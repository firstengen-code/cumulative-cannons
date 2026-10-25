package com.cumulativecannons.client;

import com.cumulativecannons.CumulativeCannons;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = CumulativeCannons.MOD_ID, value = Dist.CLIENT)
public final class ClientBeams {
    private static final List<Beam> BEAMS = new ArrayList<>();
    private static final RenderType BEAM_TYPE = RenderType.create(
            "cumulative_cannons_jet_beam",
            com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
            4096,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)
    );

    public static void add(double sx, double sy, double sz, double ex, double ey, double ez, float width, int life) {
        if (BEAMS.size() >= 32) {
            BEAMS.remove(0);
        }
        BEAMS.add(new Beam(new Vec3(sx, sy, sz), new Vec3(ex, ey, ez), width, life));
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Iterator<Beam> iterator = BEAMS.iterator();
        while (iterator.hasNext()) {
            Beam beam = iterator.next();
            beam.age++;
            if (beam.age >= beam.life) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BEAMS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(BEAM_TYPE);
        Matrix4f matrix = pose.last().pose();

        for (Beam beam : BEAMS) {
            float progress = beam.age / (float) beam.life;
            float fade = 1.0F - progress;
            drawBeam(consumer, matrix, beam.start, beam.end, beam.width * 2.7F, 1.0F, 0.52F, 0.12F, 0.3F * fade);
            drawBeam(consumer, matrix, beam.start, beam.end, beam.width, 1.0F, 0.95F, 0.78F, 0.95F * fade);
        }

        buffers.endBatch(BEAM_TYPE);
        pose.popPose();
    }

    private static void drawBeam(
            VertexConsumer consumer,
            Matrix4f matrix,
            Vec3 start,
            Vec3 end,
            float width,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vec3 direction = end.subtract(start);
        if (direction.lengthSqr() < 1.0E-8D) {
            return;
        }
        Vec3 view = start.add(end).scale(0.5D).subtract(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()).normalize();
        Vec3 right = direction.cross(view);
        if (right.lengthSqr() < 1.0E-8D) {
            right = new Vec3(0.0D, 1.0D, 0.0D);
        }
        right = right.normalize().scale(width * 0.5D);
        Vec3 up = direction.cross(right).normalize().scale(width * 0.5D);
        emitQuad(consumer, matrix, start, end, right, red, green, blue, alpha);
        emitQuad(consumer, matrix, start, end, up, red, green, blue, alpha);
    }

    private static void emitQuad(
            VertexConsumer consumer,
            Matrix4f matrix,
            Vec3 start,
            Vec3 end,
            Vec3 offset,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        consumer.addVertex(matrix, (float) (start.x - offset.x), (float) (start.y - offset.y), (float) (start.z - offset.z))
                .setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, (float) (start.x + offset.x), (float) (start.y + offset.y), (float) (start.z + offset.z))
                .setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, (float) (end.x + offset.x), (float) (end.y + offset.y), (float) (end.z + offset.z))
                .setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, (float) (end.x - offset.x), (float) (end.y - offset.y), (float) (end.z - offset.z))
                .setColor(red, green, blue, alpha);
    }

    private ClientBeams() {
    }

    private static final class Beam {
        private final Vec3 start;
        private final Vec3 end;
        private final float width;
        private final int life;
        private int age;

        private Beam(Vec3 start, Vec3 end, float width, int life) {
            this.start = start;
            this.end = end;
            this.width = width;
            this.life = life;
        }
    }
}
