package com.sshakusora.kaleidoscope_contraption.network;

import com.github.ysbbbbbb.kaleidoscopetavern.util.TextAlignment;
import com.sshakusora.kaleidoscope_contraption.client.gui.TavernContraptionTextScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KCTavernTextOpenPacket(int entityId, BlockPos localPos, String text,
                                     int maxTextLength, TextAlignment textAlignment) {
    public static void encode(KCTavernTextOpenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeBlockPos(packet.localPos);
        buffer.writeUtf(packet.text, 2048);
        buffer.writeVarInt(packet.maxTextLength);
        buffer.writeEnum(packet.textAlignment);
    }

    public static KCTavernTextOpenPacket decode(FriendlyByteBuf buffer) {
        return new KCTavernTextOpenPacket(buffer.readInt(), buffer.readBlockPos(),
                buffer.readUtf(2048), buffer.readVarInt(), buffer.readEnum(TextAlignment.class));
    }

    public static void handle(KCTavernTextOpenPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> openScreen(packet));
        }
        context.setPacketHandled(true);
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
