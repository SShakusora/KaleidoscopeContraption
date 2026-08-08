package com.sshakusora.kaleidoscope_contraption.client.init;

import com.mojang.blaze3d.platform.InputConstants;
import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = KaleidoscopeContraption.MOD_ID)
public class ClientSetupEvent {
    public static final String KEY_CATEGORY = "key.category.kaleidoscope_contraption";

    // 移除方块的按键绑定（默认绑定到R键）
    public static final KeyMapping REMOVE_BLOCK_KEY = new KeyMapping(
            "key.kaleidoscope_contraption.remove_block",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(REMOVE_BLOCK_KEY);
    }
}
