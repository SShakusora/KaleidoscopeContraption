package com.sshakusora.kaleidoscope_contraption.network;

import com.github.ysbbbbbb.kaleidoscopetavern.util.TextAlignment;
import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.TavernTextBoardSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record KCTavernTextUpdatePacket(int entityId, BlockPos localPos, String text,
                                       TextAlignment textAlignment) implements CustomPacketPayload {
    public static final Type<KCTavernTextUpdatePacket> TYPE = new Type<>(KaleidoscopeContraption.asResource("tavern_text_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KCTavernTextUpdatePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeInt(packet.entityId);
                buffer.writeBlockPos(packet.localPos);
                buffer.writeUtf(packet.text, 2048);
                buffer.writeEnum(packet.textAlignment);
            },
            buffer -> new KCTavernTextUpdatePacket(buffer.readInt(), buffer.readBlockPos(),
                    buffer.readUtf(2048), buffer.readEnum(TextAlignment.class)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KCTavernTextUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                TavernTextBoardSupport.updateText(player, packet.entityId, packet.localPos,
                        packet.text, packet.textAlignment);
            }
        });
    }
}
