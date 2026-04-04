package real.o0h.config;

import real.o0h.MacroGrid;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ButtonManager {
    private static final String CONFIG_FILE = "command_gui_buttons.json";
    private static final Gson   GSON        = new GsonBuilder().setPrettyPrinting().create();
    private static final List<ButtonData> buttons = new ArrayList<>();
    private static Path configPath;

    public static void init() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        load();
    }

    public static List<ButtonData> getButtons() { return new ArrayList<>(buttons); }

    public static void addButton(ButtonData button)    { buttons.add(button); save(); }

    public static void removeButton(int index) {
        if (index >= 0 && index < buttons.size()) { buttons.remove(index); save(); }
    }

    public static void updateButton(int index, ButtonData button) {
        if (index >= 0 && index < buttons.size()) { buttons.set(index, button); save(); }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            JsonArray  arr  = new JsonArray();
            for (ButtonData b : buttons) arr.add(b.toJson());
            root.add("buttons", arr);
            Files.createDirectories(configPath.getParent());
            try (FileWriter w = new FileWriter(configPath.toFile())) { GSON.toJson(root, w); }
            MacroGrid.LOGGER.info("Saved {} buttons to config", buttons.size());
        } catch (IOException e) {
            MacroGrid.LOGGER.error("Failed to save buttons config", e);
        }
    }

    public static void load() {
        buttons.clear();
        if (!Files.exists(configPath)) {
            MacroGrid.LOGGER.info("Config file not found, creating new one");
            save();
            return;
        }
        try (FileReader r = new FileReader(configPath.toFile())) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root.has("buttons")) {
                JsonArray arr = root.getAsJsonArray("buttons");
                for (int i = 0; i < arr.size(); i++)
                    buttons.add(ButtonData.fromJson(arr.get(i).getAsJsonObject()));
            }
            MacroGrid.LOGGER.info("Loaded {} buttons from config", buttons.size());
        } catch (IOException | JsonParseException e) {
            MacroGrid.LOGGER.error("Failed to load buttons config", e);
        }
    }
}
