package com.cumulativecannons.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModPayloads {
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                ImpactShakePayload.TYPE,
                ImpactShakePayload.STREAM_CODEC,
                ImpactShakePayload::handle
        );
        event.registrar("1").playToClient(
                JetBeamPayload.TYPE,
                JetBeamPayload.STREAM_CODEC,
                JetBeamPayload::handle
        );
        event.registrar("1").playToClient(
                EoDeafnessPayload.TYPE,
                EoDeafnessPayload.STREAM_CODEC,
                EoDeafnessPayload::handle
        );
    }

    private ModPayloads() {
    }
}
