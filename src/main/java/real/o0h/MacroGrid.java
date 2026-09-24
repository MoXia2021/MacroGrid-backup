package real.o0h;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import real.o0h.config.ButtonManager;
import real.o0h.gui.ButtonGuiScreen;

public class MacroGrid implements ClientModInitializer {

    public static final String MOD_ID = "commandguibuttons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping openGuiKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Command GUI Buttons");

        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("commandguibuttons", "general")
        );

        openGuiKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.commandguibuttons.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                category
            )
        );

        ButtonManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (client.screen != null) return;

            while (openGuiKey.consumeClick()) {
                client.setScreen(new ButtonGuiScreen());
            }
        });
    }
}
