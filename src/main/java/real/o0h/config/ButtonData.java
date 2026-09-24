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
    private String customColor; // e.g. "FF5B8C"; null = use the enum color

    public ButtonData(String name) {
        this.name = name;
        this.commands = new ArrayList<>();
        this.color = ButtonColor.DEFAULT;
        this.icon = ButtonIcon.NONE;
        this.customColor = null;
    }

    public ButtonData(String name, List<CommandEntry> commands) {
        this.name = name;
        this.commands = new ArrayList<>(commands);
        this.color = ButtonColor.DEFAULT;
        this.icon = ButtonIcon.NONE;
        this.customColor = null;
    }

    public String getName() { return name; }
    public ButtonColor getColor() { return color; }
    public void setColor(ButtonColor color) { this.color = color; }
    public String getCustomColor() { return customColor; }
    public void setCustomColor(String customColor) { this.customColor = customColor; }
    public ButtonIcon getIcon() { return icon; }
    public void setIcon(ButtonIcon icon) { this.icon = icon; }
    public List<CommandEntry> getCommands() { return commands; }
    public void addCommand(CommandEntry command) { commands.add(command); }

    /** Format code for rendering the button label: custom colors use the
     *  MC hex format (§x + §R§R§G§G§B§B), enum colors use their § code. */
    public String getDisplayCode() {
        if (customColor != null && !customColor.isEmpty()) return hexCode(customColor);
        return color.getCode();
    }

    public static String hexCode(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) sb.append('§').append(c);
        return sb.toString();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("color", color.name());
        if (customColor != null) json.addProperty("customColor", customColor);
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
        if (json.has("customColor") && !json.get("customColor").isJsonNull()) {
            try { button.setCustomColor(json.get("customColor").getAsString()); }
            catch (Exception e) { button.setCustomColor(null); }
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
        RED("Red", "§c"),
        GREEN("Green", "§a"),
        BLUE("Blue", "§9"),
        YELLOW("Yellow", "§e"),
        PURPLE("Purple", "§5"),
        AQUA("Aqua", "§b"),
        GOLD("Gold", "§6"),
        GRAY("Gray", "§7"),
        BLACK("Black", "§0");

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