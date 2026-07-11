package com.sshakusora.kaleidoscope_contraption.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class KCPacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    private KCPacketHandler() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(KCContraptionChangedPacket.TYPE, KCContraptionChangedPacket.STREAM_CODEC,
                KCContraptionChangedPacket::handle);
        registrar.playToServer(KCRemoveBlockPacket.TYPE, KCRemoveBlockPacket.STREAM_CODEC,
                KCRemoveBlockPacket::handle);
    }

    public static void sendToServer(KCRemoveBlockPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToTracking(KCContraptionChangedPacket packet, ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntity(player, packet);
    }

    public static void sendToTracking(KCContraptionChangedPacket packet, Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, packet);
    }
}
