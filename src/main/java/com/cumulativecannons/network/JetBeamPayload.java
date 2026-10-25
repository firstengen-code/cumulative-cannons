package com.cumulativecannons.network;

import com.cumulativecannons.CumulativeCannons;
import com.cumulativecannons.client.ClientBeams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record JetBeamPayload(
        double startX,
        double startY,
        double startZ,
        double endX,
        double endY,
        double endZ,
        float width,
        int life
) implements CustomPacketPayload {
    public static final Type<JetBeamPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CumulativeCannons.MOD_ID, "jet_beam"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JetBeamPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeDouble(payload.startX);
                buf.writeDouble(payload.startY);
                buf.writeDouble(payload.startZ);
                buf.writeDouble(payload.endX);
                buf.writeDouble(payload.endY);
                buf.writeDouble(payload.endZ);
                buf.writeFloat(payload.width);
                buf.writeVarInt(payload.life);
            },
            buf -> new JetBeamPayload(
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readVarInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> ClientBeams.add(startX, startY, startZ, endX, endY, endZ, width, life));
    }
}
