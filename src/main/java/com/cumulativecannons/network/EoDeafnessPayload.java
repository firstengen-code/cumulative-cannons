package com.cumulativecannons.network;

import com.cumulativecannons.CumulativeCannons;
import com.cumulativecannons.client.EoDeafnessClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EoDeafnessPayload(float intensity, double silentSeconds, double effectivePercent,
                                String visibility, int intensityPercent) implements CustomPacketPayload {
    public static final Type<EoDeafnessPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CumulativeCannons.MOD_ID, "eo_deafness"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EoDeafnessPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeFloat(payload.intensity);
                buf.writeDouble(payload.silentSeconds);
                buf.writeDouble(payload.effectivePercent);
                buf.writeUtf(payload.visibility);
                buf.writeVarInt(payload.intensityPercent);
            },
            buf -> new EoDeafnessPayload(
                    buf.readFloat(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readUtf(),
                    buf.readVarInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> EoDeafnessClient.apply(intensity, silentSeconds, effectivePercent,
                visibility, intensityPercent));
    }
}
