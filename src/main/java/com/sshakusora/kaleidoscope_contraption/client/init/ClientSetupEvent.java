package com.sshakusora.kaleidoscope_contraption.client.init;

import com.mojang.blaze3d.platform.InputConstants;
import com.sshakusora.kaleidoscope_contraption.KaleidoscopeContraption;
import com.sshakusora.kaleidoscope_contraption.client.gui.overlay.ContraptionPotOverlay;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

import static net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR;

@EventBusSubscriber(value = Dist.CLIENT, modid = KaleidoscopeContraption.MOD_ID)
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
    public static void onRegisterGuiOverlays(RegisterGuiLayersEvent event) {
        // 注册ContraptionPotOverlay
        event.registerAbove(CROSSHAIR, KaleidoscopeContraption.asResource("contraption_pot_overlay"), new ContraptionPotOverlay());
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(REMOVE_BLOCK_KEY);
    }
}
