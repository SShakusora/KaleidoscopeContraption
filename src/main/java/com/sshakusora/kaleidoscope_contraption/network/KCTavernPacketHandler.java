package com.sshakusora.kaleidoscope_contraption.network;

import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Packets that are only meaningful while Kaleidoscope Tavern is loaded. */
public final class KCTavernPacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            KaleidoscopeContraption.asResource("tavern"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);
    private static int packetId;
    private static boolean registered;

    private KCTavernPacketHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        registerPacket(KCTavernTextOpenPacket.class, KCTavernTextOpenPacket::encode,
                KCTavernTextOpenPacket::decode, KCTavernTextOpenPacket::handle,
                NetworkDirection.PLAY_TO_CLIENT);
        registerPacket(KCTavernTextUpdatePacket.class, KCTavernTextUpdatePacket::encode,
                KCTavernTextUpdatePacket::decode, KCTavernTextUpdatePacket::handle,
                NetworkDirection.PLAY_TO_SERVER);
    }

    private static <T> void registerPacket(Class<T> packetClass, BiConsumer<T, FriendlyByteBuf> encoder,
                                           Function<FriendlyByteBuf, T> decoder,
                                           BiConsumer<T, Supplier<NetworkEvent.Context>> handler,
                                           NetworkDirection direction) {
        CHANNEL.registerMessage(packetId++, packetClass, encoder, decoder, handler, Optional.of(direction));
    }

    public static void sendToClient(Player player, Object packet) {
        if (player instanceof ServerPlayer serverPlayer) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
        }
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
