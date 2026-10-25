package com.cumulativecannons.network;

import com.cumulativecannons.CumulativeCannons;
import com.cumulativecannons.client.ClientShake;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ImpactShakePayload(double x, double y, double z, float strength, int duration) implements CustomPacketPayload {
    public static final Type<ImpactShakePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CumulativeCannons.MOD_ID, "impact_shake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ImpactShakePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeDouble(payload.x);
                buf.writeDouble(payload.y);
                buf.writeDouble(payload.z);
                buf.writeFloat(payload.strength);
                buf.writeVarInt(payload.duration);
            },
            buf -> new ImpactShakePayload(
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
        context.enqueueWork(() -> ClientShake.trigger(x, y, z, strength, duration));
    }
}
