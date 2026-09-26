package real.o0h.config;

import real.o0h.MacroGrid;
import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Profile-aware button storage, ported from the 3.4 (MC 26.2) branch.
 * Each profile is an independent list of buttons; the UI shows one profile
 * at a time. Old single-list configs ("buttons": [...]) are migrated into a
 * "Default" profile on first load.
 */
public class ButtonManager {
    private static final String CONFIG_FILE = "command_gui_buttons.json";
    private static final String DEFAULT_PROFILE = "Default";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, List<ButtonData>> profiles = new LinkedHashMap<>();
    private static String currentProfile = DEFAULT_PROFILE;
    private static Path configPath;

    public static void init() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        load();
    }

    public static List<ButtonData> getButtons() {
        return new ArrayList<>(currentList());
    }

    public static void addButton(ButtonData button) {
        currentList().add(button);
        save();
    }

    public static void removeButton(int index) {
        List<ButtonData> list = currentList();
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            save();
        }
    }

    public static void updateButton(int index, ButtonData button) {
        List<ButtonData> list = currentList();
        if (index >= 0 && index < list.size()) {
            list.set(index, button);
            save();
        }
    }

    private static List<ButtonData> currentList() {
        return profiles.computeIfAbsent(currentProfile, k -> new ArrayList<>());
    }

    // ---- Profile management ----

    public static List<String> getProfileNames() {
        return new ArrayList<>(profiles.keySet());
    }

    public static String getCurrentProfile() {
        return currentProfile;
    }

    public static void setCurrentProfile(String name) {
        if (profiles.containsKey(name)) {
            currentProfile = name;
            save();
        }
    }

    public static String createProfile(String baseName) {
        String base = (baseName == null || baseName.isBlank()) ? "Profile" : baseName.trim();
        String name = uniqueName(base);
        profiles.put(name, new ArrayList<>());
        currentProfile = name;
        save();
        return name;
    }

    public static boolean renameProfile(String oldName, String newName) {
        if (newName == null || newName.isBlank() || !profiles.containsKey(oldName)) return false;
        newName = newName.trim();
        if (newName.equals(oldName)) return true;
        if (profiles.containsKey(newName)) return false;

        Map<String, List<ButtonData>> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, List<ButtonData>> e : profiles.entrySet()) {
            rebuilt.put(e.getKey().equals(oldName) ? newName : e.getKey(), e.getValue());
        }
        profiles.clear();
        profiles.putAll(rebuilt);
        if (currentProfile.equals(oldName)) currentProfile = newName;
        save();
        return true;
    }

    public static boolean deleteProfile(String name) {
        if (profiles.size() <= 1 || !profiles.containsKey(name)) return false;
        profiles.remove(name);
        if (currentProfile.equals(name)) currentProfile = profiles.keySet().iterator().next();
        save();
        return true;
    }

    private static String uniqueName(String base) {
        if (!profiles.containsKey(base)) return base;
        int n = 2;
        while (profiles.containsKey(base + " " + n)) n++;
        return base + " " + n;
    }

    // ---- Persistence ----

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("currentProfile", currentProfile);
            JsonObject profilesJson = new JsonObject();
            for (Map.Entry<String, List<ButtonData>> e : profiles.entrySet()) {
                JsonArray arr = new JsonArray();
                for (ButtonData b : e.getValue()) arr.add(b.toJson());
                profilesJson.add(e.getKey(), arr);
            }
            root.add("profiles", profilesJson);
            Files.createDirectories(configPath.getParent());
            try (FileWriter w = new FileWriter(configPath.toFile())) { GSON.toJson(root, w); }
            MacroGrid.LOGGER.info("Saved {} profile(s) to config", profiles.size());
        } catch (IOException e) {
            MacroGrid.LOGGER.error("Failed to save buttons config", e);
        }
    }

    public static void load() {
        profiles.clear();
        currentProfile = DEFAULT_PROFILE;

        if (!Files.exists(configPath)) {
            MacroGrid.LOGGER.info("Config file not found, creating new one");
            profiles.put(DEFAULT_PROFILE, new ArrayList<>());
            save();
            return;
        }

        try (FileReader r = new FileReader(configPath.toFile())) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) {
                profiles.put(DEFAULT_PROFILE, new ArrayList<>());
                return;
            }

            if (root.has("profiles")) {
                JsonObject profilesJson = root.getAsJsonObject("profiles");
                for (String key : profilesJson.keySet()) {
                    profiles.put(key, readButtons(profilesJson.getAsJsonArray(key)));
                }
                if (profiles.isEmpty()) profiles.put(DEFAULT_PROFILE, new ArrayList<>());

                String saved = root.has("currentProfile") ? root.get("currentProfile").getAsString() : null;
                currentProfile = (saved != null && profiles.containsKey(saved))
                        ? saved
                        : profiles.keySet().iterator().next();

            } else if (root.has("buttons")) {
                profiles.put(DEFAULT_PROFILE, readButtons(root.getAsJsonArray("buttons")));
                currentProfile = DEFAULT_PROFILE;
                MacroGrid.LOGGER.info("Migrated old single-profile config into a '{}' profile", DEFAULT_PROFILE);
                save();

            } else {
                profiles.put(DEFAULT_PROFILE, new ArrayList<>());
            }

            MacroGrid.LOGGER.info("Loaded {} profile(s), current: {}", profiles.size(), currentProfile);
        } catch (IOException | JsonParseException e) {
            MacroGrid.LOGGER.error("Failed to load buttons config", e);
            profiles.put(DEFAULT_PROFILE, new ArrayList<>());
        }
    }

    /** Parse a JSON array of button objects into a mutable list. */
    private static List<ButtonData> readButtons(JsonArray arr) {
        List<ButtonData> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            list.add(ButtonData.fromJson(arr.get(i).getAsJsonObject()));
        }
        return list;
    }
}
