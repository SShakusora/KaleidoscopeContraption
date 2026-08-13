package com.sshakusora.kaleidoscope_contraption.network;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

/** Packets that are only meaningful while Kaleidoscope Tavern is loaded. */
public final class KCTavernPacketHandler {
    private KCTavernPacketHandler() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(KCTavernTextOpenPacket.TYPE, KCTavernTextOpenPacket.STREAM_CODEC,
                KCTavernTextOpenPacket::handle);
        registrar.playToServer(KCTavernTextUpdatePacket.TYPE, KCTavernTextUpdatePacket.STREAM_CODEC,
                KCTavernTextUpdatePacket::handle);
    }

    public static void sendToClient(net.minecraft.world.entity.player.Player player,
                                    CustomPacketPayload packet) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
