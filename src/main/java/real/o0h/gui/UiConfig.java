package real.o0h.gui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class UiConfig {

    public enum GuiMode { CLASSIC, RADIAL }

    private static final String CONFIG_FILE = "command_gui_buttons_ui.json";
    private static final Gson   GSON        = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER      = LoggerFactory.getLogger("commandguibuttons");

    private static GuiMode guiMode = null;
    private static Path    configPath;

    public static void init() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        load();
    }

    public static GuiMode  getGuiMode()     { return guiMode; }
    public static boolean  isFirstLaunch()  { return guiMode == null; }
    public static void     setGuiMode(GuiMode mode) { guiMode = mode; save(); }

    private static void load() {
        if (!Files.exists(configPath)) return;
        try (FileReader reader = new FileReader(configPath.toFile())) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root != null && root.has("gui_mode")) {
                guiMode = switch (root.get("gui_mode").getAsString()) {
                    case "radial"  -> GuiMode.RADIAL;
                    case "classic" -> GuiMode.CLASSIC;
                    default        -> null;
                };
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load UI config", e);
        }
    }

    private static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("gui_mode", guiMode == GuiMode.RADIAL ? "radial" : "classic");
            Files.createDirectories(configPath.getParent());
            try (FileWriter writer = new FileWriter(configPath.toFile())) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save UI config", e);
        }
    }
}
