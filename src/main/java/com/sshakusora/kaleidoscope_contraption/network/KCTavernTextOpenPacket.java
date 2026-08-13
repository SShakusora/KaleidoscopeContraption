package com.sshakusora.kaleidoscope_contraption.network;

import com.github.ysbbbbbb.kaleidoscopetavern.util.TextAlignment;
import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import com.sshakusora.kaleidoscope_contraption.client.gui.TavernContraptionTextScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record KCTavernTextOpenPacket(int entityId, BlockPos localPos, String text,
                                     int maxTextLength, TextAlignment textAlignment) implements CustomPacketPayload {
    public static final Type<KCTavernTextOpenPacket> TYPE = new Type<>(KaleidoscopeContraption.asResource("tavern_text_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KCTavernTextOpenPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeInt(packet.entityId);
                buffer.writeBlockPos(packet.localPos);
                buffer.writeUtf(packet.text, 2048);
                buffer.writeVarInt(packet.maxTextLength);
                buffer.writeEnum(packet.textAlignment);
            },
            buffer -> new KCTavernTextOpenPacket(buffer.readInt(), buffer.readBlockPos(),
                    buffer.readUtf(2048), buffer.readVarInt(), buffer.readEnum(TextAlignment.class)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(KCTavernTextOpenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> openScreen(packet));
    }

    @OnlyIn(Dist.CLIENT)
    private static void openScreen(KCTavernTextOpenPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new TavernContraptionTextScreen(
                    packet.entityId, packet.localPos, packet.text,
                    packet.maxTextLength, packet.textAlignment));
        }
    }
}
