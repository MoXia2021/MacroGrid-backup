package real.o0h.macrogrid.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import real.o0h.macrogrid.client.config.ButtonManager;
import real.o0h.macrogrid.client.gui.ButtonGuiScreen;

public class MacrogridClient implements ClientModInitializer {

    public static final String MOD_ID = "macrogrid";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Command GUI Buttons");

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("macrogrid", "general")
        );

        openGuiKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.macrogrid.open_gui",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_G,
                        category
                )
        );

        ButtonManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (ButtonGuiScreen.isOpen()) return;

            while (openGuiKey.consumeClick()) {
                client.gui.setScreen(new ButtonGuiScreen());
            }
        });
    }
}