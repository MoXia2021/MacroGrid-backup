package real.o0h.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class ButtonData {
    private String name;
    private List<CommandEntry> commands;
    private ButtonColor color;
    private ButtonIcon icon;

    public ButtonData(String name) {
        this.name = name;
        this.commands = new ArrayList<>();
        this.color = ButtonColor.DEFAULT;
        this.icon = ButtonIcon.NONE;
    }

    public ButtonData(String name, List<CommandEntry> commands) {
        this.name = name;
        this.commands = new ArrayList<>(commands);
        this.color = ButtonColor.DEFAULT;
        this.icon = ButtonIcon.NONE;
    }

    public String getName() { return name; }
    public ButtonColor getColor() { return color; }
    public void setColor(ButtonColor color) { this.color = color; }
    public ButtonIcon getIcon() { return icon; }
    public void setIcon(ButtonIcon icon) { this.icon = icon; }
    public List<CommandEntry> getCommands() { return commands; }
    public void addCommand(CommandEntry command) { commands.add(command); }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("color", color.name());
        json.addProperty("icon", icon.name());
        JsonArray commandsArray = new JsonArray();
        for (CommandEntry command : commands) {
            commandsArray.add(command.toJson());
        }
        json.add("commands", commandsArray);
        return json;
    }

    public static ButtonData fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        ButtonData button = new ButtonData(name);
        if (json.has("color")) {
            try { button.setColor(ButtonColor.valueOf(json.get("color").getAsString())); }
            catch (Exception e) { button.setColor(ButtonColor.DEFAULT); }
        }
        if (json.has("icon")) {
            try { button.setIcon(ButtonIcon.valueOf(json.get("icon").getAsString())); }
            catch (Exception e) { button.setIcon(ButtonIcon.NONE); }
        }
        if (json.has("commands")) {
            JsonArray commandsArray = json.getAsJsonArray("commands");
            for (int i = 0; i < commandsArray.size(); i++) {
                button.addCommand(CommandEntry.fromJson(commandsArray.get(i).getAsJsonObject()));
            }
        }
        return button;
    }

    public static class CommandEntry {
        private String command;
        private CommandType type;
        public CommandEntry(String command, CommandType type) { this.command = command; this.type = type; }
        public String getCommand() { return command; }
        public CommandType getType() { return type; }
        public void setType(CommandType type) { this.type = type; }
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("command", command);
            json.addProperty("type", type.name());
            return json;
        }
        public static CommandEntry fromJson(JsonObject json) {
            String command = json.get("command").getAsString();
            CommandType type = CommandType.valueOf(json.get("type").getAsString());
            return new CommandEntry(command, type);
        }
    }

    public enum CommandType { COMMAND, MESSAGE }

    public enum ButtonColor {
        DEFAULT("Default", ""),
        // 16 standard Minecraft format-code colors (§0-§f)
        BLACK("Black", "§0"),
        DARK_BLUE("Dark Blue", "§1"),
        DARK_GREEN("Dark Green", "§2"),
        DARK_AQUA("Dark Aqua", "§3"),
        DARK_RED("Dark Red", "§4"),
        PURPLE("Purple", "§5"),
        GOLD("Gold", "§6"),
        GRAY("Gray", "§7"),
        DARK_GRAY("Dark Gray", "§8"),
        BLUE("Blue", "§9"),
        GREEN("Green", "§a"),
        AQUA("Aqua", "§b"),
        RED("Red", "§c"),
        LIGHT_PURPLE("Light Purple", "§d"),
        YELLOW("Yellow", "§e"),
        WHITE("White", "§f"),
        // 8 extended colors using Minecraft custom RGB format codes (§xRRGGBB)
        ORANGE("Orange", "§x§F§F§A§5§0§0"),
        PINK("Pink", "§x§F§F§C§0§C§B"),
        LIME("Lime", "§x§3§2§C§D§3§2"),
        CYAN("Cyan", "§x§0§0§C§E§D§1"),
        MAGENTA("Magenta", "§x§F§F§0§0§F§F"),
        BROWN("Brown", "§x§8§B§4§5§1§3"),
        NAVY("Navy", "§x§0§0§0§0§8§0"),
        MAROON("Maroon", "§x§8§0§0§0§0§0");

        private final String humanName;
        private final String code;

        ButtonColor(String humanName, String code) {
            this.humanName = humanName;
            this.code = code;
        }

        public String getDisplayName() {
            return code + humanName;
        }

        public String getCode() {
            return code;
        }
    }

    public enum ButtonIcon {
        NONE("None", ""),
        SWORD("Sword", "⚔"), PICK("Pick", "⛏"), AXE("Axe", "🪓"), BOW("Bow", "🏹"),
        TRIDENT("Trident", "🔱"), SHIELD("Shield", "🛡"),
        STAR("Star", "★"), POTION("Potion", "🧪"), SKULL("Skull", "☠"),
        HEART("Heart", "❤"), FIRE("Fire", "🔥"), LIGHTNING("Bolt", "⚡"),
        APPLE("Apple", "🍎"), CAKE("Cake", "🎂"), MEAT("Meat", "🍖"),
        DROP("Water", "💧"), SUN("Sun", "☀"), MOON("Moon", "☾"), FLOWER("Flower", "✿"),
        HOME("Home", "🏠"), SETTINGS("Gear", "⚙"), KEY("Key", "🔑"),
        MAP("Map", "🗺"), MAIL("Mail", "✉"), TRASH("Trash", "🗑"),
        CHECK("Check", "✔"), CROSS("Cross", "✖"), WARN("Warn", "⚠"),
        ONE("1", "❶"), TWO("2", "❷"), THREE("3", "❸"), FOUR("4", "❹"), FIVE("5", "❺"),
        RIGHT("Right", "➜"), LEFT("Left", "⬅"), UP("Up", "⬆"), DOWN("Down", "⬇");

        private final String name;
        private final String symbol;
        ButtonIcon(String name, String symbol) { this.name = name; this.symbol = symbol; }
        public String getName() { return name; }
        public String getSymbol() { return symbol; }
    }
}