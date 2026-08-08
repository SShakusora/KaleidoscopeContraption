package com.sshakusora.kaleidoscope_contraption.network;

import com.github.ysbbbbbb.kaleidoscopetavern.util.TextAlignment;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.TavernTextBoardSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KCTavernTextUpdatePacket(int entityId, BlockPos localPos, String text,
                                       TextAlignment textAlignment) {
    public static void encode(KCTavernTextUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeBlockPos(packet.localPos);
        buffer.writeUtf(packet.text, 2048);
        buffer.writeEnum(packet.textAlignment);
    }

    public static KCTavernTextUpdatePacket decode(FriendlyByteBuf buffer) {
        return new KCTavernTextUpdatePacket(buffer.readInt(), buffer.readBlockPos(),
                buffer.readUtf(2048), buffer.readEnum(TextAlignment.class));
    }

    public static void handle(KCTavernTextUpdatePacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    TavernTextBoardSupport.updateText(player, packet.entityId, packet.localPos,
                            packet.text, packet.textAlignment);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
