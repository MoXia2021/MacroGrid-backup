package real.o0h;

import real.o0h.gui.UiConfig;
import real.o0h.gui.ButtonGuiScreen;
import real.o0h.gui.ModeSelectScreen;
import real.o0h.config.ButtonManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.resources.Identifier;

public class MacroGrid implements ClientModInitializer {
    public static final String MOD_ID = "commandguibuttons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Command GUI Buttons");

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("commandguibuttons", "general"));

        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.commandguibuttons.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                category
        ));

        UiConfig.init();
        ButtonManager.init();
        

        boolean[] wasDown = {false};

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (client.screen != null) {
                // if (RadialMenu.isVisible()) RadialMenu.hide(false); 
                wasDown[0] = false;
                return;
            }

            while (openGuiKey.consumeClick()) {
                if (UiConfig.isFirstLaunch()) {
                    client.setScreen(new ModeSelectScreen(false));
                    return;
                }
                client.setScreen(new ButtonGuiScreen());
            }

            /*
            if (UiConfig.getGuiMode() == UiConfig.GuiMode.RADIAL) {
                boolean isDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_G);

                if (isDown && !wasDown[0]) {
                    RadialMenu.show();
                    client.setScreen(new RadialOverlayScreen());
                }

                if (!isDown && wasDown[0] && RadialMenu.isVisible()) {
                    RadialMenu.hide(true);
                    if (client.screen instanceof RadialOverlayScreen) {
                        client.screen.onClose();
                        client.setScreen(null);
                    }
                }
                wasDown[0] = isDown;
            }
            */
        });
    }

    public static void openModeSelect() {
        Minecraft.getInstance().setScreen(new ModeSelectScreen(true));
    }
}