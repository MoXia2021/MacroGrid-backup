package real.o0h.gui;

import real.o0h.config.ButtonData;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class EditButtonScreen extends BaseOwoScreen<FlowLayout> {

    private final ButtonData         existingButton;
    private final Consumer<ButtonData> onSave;
    private final boolean            isAddMode;

    private ButtonData.ButtonColor currentColor;
    private ButtonData.ButtonIcon  currentIcon;

    private TextBoxComponent nameField;
    private ButtonComponent  colorButton;
    private FlowLayout       commandListLayout;

    private final List<CommandWidget> commandWidgets = new ArrayList<>();

    public EditButtonScreen(ButtonData button, Consumer<ButtonData> onSave) {
        this(button, onSave, button == null);
    }

    public EditButtonScreen(ButtonData button, Consumer<ButtonData> onSave, boolean isAddMode) {
        this.existingButton = button;
        this.onSave         = onSave;
        this.isAddMode      = isAddMode;

        if (button != null) {
            currentColor = button.getColor();
            currentIcon  = button.getIcon();
            for (ButtonData.CommandEntry cmd : button.getCommands())
                commandWidgets.add(new CommandWidget(cmd.getCommand(), cmd.getType()));
        } else {
            currentColor = ButtonData.ButtonColor.DEFAULT;
            currentIcon  = ButtonData.ButtonIcon.NONE;
        }
    }

    private boolean firstTick    = true;
    private boolean needsRefresh = false;

    @Override
    public void tick() {
        super.tick();
        if (firstTick) {
            firstTick    = false;
            needsRefresh = true;
            String correctName = existingButton != null ? existingButton.getName() : "";
            if (nameField != null) {
                nameField.setValue(correctName);
                nameField.setSuggestion(correctName.isEmpty() ? "Enter button name..." : "");
            }
        }
        if (needsRefresh) {
            needsRefresh = false;
            for (CommandWidget cw : commandWidgets) {
                if (cw.textField != null) {
                    String t = cw.text;
                    cw.textField.setValue("");
                    cw.textField.setValue(t);
                }
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        String correctName = existingButton != null ? existingButton.getName() : "";
        if (nameField != null) {
            nameField.setValue(correctName);
            nameField.setSuggestion(correctName.isEmpty() ? "Enter button name..." : "");
        }
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fixed(320), Sizing.content());
        panel.surface(Surface.flat(0xB0000000)).padding(Insets.of(6));

        panel.child(UIComponents.label(
                        Component.literal(isAddMode ? "Add New Button" : "Edit Button")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .margins(Insets.bottom(4)));

        panel.child(UIComponents.label(Component.literal("Button Name:")).margins(Insets.bottom(2)));
        nameField = UIComponents.textBox(Sizing.fill(100));
        nameField.setMaxLength(100);
        nameField.setSuggestion("Enter button name...");
        nameField.onChanged().subscribe(s ->
                nameField.setSuggestion(s.isEmpty() ? "Enter button name..." : ""));
        if (existingButton != null) nameField.setValue(existingButton.getName());
        panel.child(nameField.margins(Insets.bottom(4)));

        FlowLayout colorIconRow = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        colorIconRow.gap(2).margins(Insets.bottom(4));

        colorButton = UIComponents.button(
                Component.literal("Color: " + currentColor.getDisplayName()), b -> cycleColor());
        colorButton.horizontalSizing(Sizing.fixed(150));

        ButtonComponent iconButton = UIComponents.button(
                Component.literal("Icon: " + iconLabel()), b -> openIconPicker());
        iconButton.horizontalSizing(Sizing.fixed(150));

        colorIconRow.child(colorButton).child(iconButton);
        panel.child(colorIconRow);

        panel.child(UIComponents.label(Component.literal("Actions:")).margins(Insets.bottom(2)));
        commandListLayout = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        var scroll = UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(100), commandListLayout);
        scroll.margins(Insets.bottom(4));
        panel.child(scroll);

        FlowLayout addCmdRow = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        addCmdRow.gap(2).margins(Insets.bottom(4));

        ButtonComponent addCmdBtn = UIComponents.button(Component.literal("+ Command"), b -> {
            commandWidgets.add(new CommandWidget("/", ButtonData.CommandType.COMMAND));
            rebuildCommandList();
            needsRefresh = true;
        });
        addCmdBtn.horizontalSizing(Sizing.fixed(150));

        ButtonComponent addMsgBtn = UIComponents.button(Component.literal("+ Message"), b -> {
            commandWidgets.add(new CommandWidget("", ButtonData.CommandType.MESSAGE));
            rebuildCommandList();
            needsRefresh = true;
        });
        addMsgBtn.horizontalSizing(Sizing.fixed(150));

        addCmdRow.child(addCmdBtn).child(addMsgBtn);
        panel.child(addCmdRow);

        FlowLayout saveRow = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        saveRow.gap(2);

        ButtonComponent saveBtn = UIComponents.button(Component.literal("Save"),
                b -> saveAndClose());
        saveBtn.horizontalSizing(Sizing.fixed(150));

        ButtonComponent cancelBtn = UIComponents.button(Component.literal("Cancel"),
                b -> onClose());
        cancelBtn.horizontalSizing(Sizing.fixed(150));

        saveRow.child(saveBtn).child(cancelBtn);
        panel.child(saveRow);

        root.child(panel);
        rebuildCommandList();
    }

    private void rebuildCommandList() {
        if (commandListLayout == null) return;
        commandListLayout.clearChildren();

        for (int i = 0; i < commandWidgets.size(); i++) {
            final int idx = i;
            CommandWidget cw = commandWidgets.get(i);

            FlowLayout row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
            row.gap(2).margins(Insets.bottom(2));

            ButtonComponent typeBtn = UIComponents.button(
                    Component.literal(cw.type == ButtonData.CommandType.COMMAND ? "§9CMD" : "§aMSG"),
                    b -> {
                        cw.type = cw.type == ButtonData.CommandType.COMMAND
                                ? ButtonData.CommandType.MESSAGE
                                : ButtonData.CommandType.COMMAND;
                        rebuildCommandList();
                    });
            typeBtn.horizontalSizing(Sizing.fixed(34));
            typeBtn.tooltip(Component.literal("Click to toggle CMD / MSG"));

            ButtonComponent upBtn = UIComponents.button(Component.literal("↑"), b -> {
                if (idx > 0) {
                    CommandWidget tmp = commandWidgets.get(idx);
                    commandWidgets.set(idx, commandWidgets.get(idx - 1));
                    commandWidgets.set(idx - 1, tmp);
                    rebuildCommandList(); needsRefresh = true;
                }
            });
            upBtn.horizontalSizing(Sizing.fixed(16));

            ButtonComponent downBtn = UIComponents.button(Component.literal("↓"), b -> {
                if (idx < commandWidgets.size() - 1) {
                    CommandWidget tmp = commandWidgets.get(idx);
                    commandWidgets.set(idx, commandWidgets.get(idx + 1));
                    commandWidgets.set(idx + 1, tmp);
                    rebuildCommandList(); needsRefresh = true;
                }
            });
            downBtn.horizontalSizing(Sizing.fixed(16));

            ButtonComponent delBtn = UIComponents.button(Component.literal("§cX"), b -> {
                commandWidgets.remove(idx);
                rebuildCommandList(); needsRefresh = true;
            });
            delBtn.horizontalSizing(Sizing.fixed(16));

            TextBoxComponent field = cw.createField();
            row.child(typeBtn).child(field).child(upBtn).child(downBtn).child(delBtn);
            commandListLayout.child(row);
        }
    }

    private void cycleColor() {
        ButtonData.ButtonColor[] colors = ButtonData.ButtonColor.values();
        int cur = Arrays.asList(colors).indexOf(currentColor);
        currentColor = colors[(cur + 1) % colors.length];
        if (colorButton != null)
            colorButton.setMessage(Component.literal("Color: " + currentColor.getDisplayName()));
    }

    private void openIconPicker() {
        ButtonData current = buildButtonData();
        Minecraft.getInstance().setScreen(new IconSelectionScreen(
                icon -> {
                    current.setIcon(icon);
                    Minecraft.getInstance().setScreen(new EditButtonScreen(current, onSave, isAddMode));
                },
                () -> Minecraft.getInstance().setScreen(new EditButtonScreen(current, onSave, isAddMode))
        ));
    }

    private String iconLabel() {
        if (currentIcon == null || currentIcon == ButtonData.ButtonIcon.NONE) return "None";
        return currentIcon.getSymbol() + " " + currentIcon.getName();
    }

    private ButtonData buildButtonData() {
        String name = nameField != null
                ? nameField.getValue()
                : (existingButton != null ? existingButton.getName() : "");
        List<ButtonData.CommandEntry> cmds = new ArrayList<>();
        for (CommandWidget w : commandWidgets)
            cmds.add(new ButtonData.CommandEntry(w.getText(), w.getType()));
        ButtonData data = new ButtonData(name, cmds);
        data.setColor(currentColor);
        data.setIcon(currentIcon);
        return data;
    }

    private void saveAndClose() {
        if (nameField == null || nameField.getValue().trim().isEmpty()) return;
        onSave.accept(buildButtonData());
        onClose();
    }

    private static class CommandWidget {
        String text;
        ButtonData.CommandType type;
        TextBoxComponent textField;

        CommandWidget(String text, ButtonData.CommandType type) {
            this.text = text;
            this.type = type;
        }

        TextBoxComponent createField() {
            textField = UIComponents.textBox(Sizing.fixed(180));
            textField.setValue(text);
            textField.setMaxLength(256);
            textField.onChanged().subscribe(s -> text = s);
            return textField;
        }
        String getText() { return textField != null ? textField.getValue() : text; }
        ButtonData.CommandType getType() { return type; }
    }
}
