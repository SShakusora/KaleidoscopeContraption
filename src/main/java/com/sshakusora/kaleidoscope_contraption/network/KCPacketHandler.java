package com.sshakusora.kaleidoscope_contraption.network;

import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class KCPacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            KaleidoscopeContraption.asResource("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        // 注册Contraption方块NBT变更包
        registerPacket(KCContraptionChangedPacket.class,
                KCContraptionChangedPacket::encode,
                KCContraptionChangedPacket::decode,
                KCContraptionChangedPacket::handle,
                NetworkDirection.PLAY_TO_CLIENT);

        // 注册移除方块请求包（C2S）
        registerPacket(KCRemoveBlockPacket.class,
                KCRemoveBlockPacket::encode,
                KCRemoveBlockPacket::decode,
                KCRemoveBlockPacket::handle,
                NetworkDirection.PLAY_TO_SERVER);
    }

    private static <T> void registerPacket(Class<T> packetClass,
                                           BiConsumer<T, FriendlyByteBuf> encoder,
                                           Function<FriendlyByteBuf, T> decoder,
                                           BiConsumer<T, Supplier<NetworkEvent.Context>> handler,
                                           NetworkDirection direction) {
        INSTANCE.registerMessage(packetId++, packetClass, encoder, decoder, handler, Optional.of(direction));
    }

    public static void sendToTracking(Object packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), packet);
    }

    public static void sendToTracking(Object packet, Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }
}
