package com.cumulativecannons.network;

import com.cumulativecannons.CumulativeCannons;
import com.cumulativecannons.tunnel.TunnelBreakHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TunnelBreakPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<TunnelBreakPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CumulativeCannons.MOD_ID, "tunnel_break"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TunnelBreakPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBlockPos(payload.pos),
            buf -> new TunnelBreakPayload(buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                var server = player.server;
                server.tell(new TickTask(server.getTickCount() + 1,
                        () -> TunnelBreakHandler.breakTracked(player.serverLevel(), pos, player)));
            }
        });
    }
}
