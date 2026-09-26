package real.o0h.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import real.o0h.config.ButtonData;

public class EditButtonScreen extends BaseOwoScreen<FlowLayout> {

    private final ButtonData existingButton;
    private final Consumer<ButtonData> onSave;
    private final boolean isAddMode;

    private ButtonData.ButtonColor currentColor;
    private ButtonData.ButtonIcon currentIcon;

    private TextBoxComponent nameField;
    private LabelComponent previewLabel;
    private FlowLayout commandListLayout;

    private final List<CommandWidget> commandWidgets = new ArrayList<>();

    private static final int PAIR_BTN = 142;
    private static final int SAVE_BTN = 148;
    private static final int FIELD_W = 190;

    public EditButtonScreen(ButtonData button, Consumer<ButtonData> onSave) {
        this(button, onSave, button == null);
    }

    public EditButtonScreen(
        ButtonData button,
        Consumer<ButtonData> onSave,
        boolean isAddMode
    ) {
        this.existingButton = button;
        this.onSave = onSave;
        this.isAddMode = isAddMode;

        if (button != null) {
            currentColor = button.getColor();
            currentIcon = button.getIcon();
            for (ButtonData.CommandEntry cmd : button.getCommands())
                commandWidgets.add(
                    new CommandWidget(cmd.getCommand(), cmd.getType())
                );
        } else {
            currentColor = ButtonData.ButtonColor.DEFAULT;
            currentIcon = ButtonData.ButtonIcon.NONE;
        }
    }

    private boolean firstTick = true;
    private boolean needsRefresh = false;

    @Override
    public void tick() {
        super.tick();
        if (firstTick) {
            firstTick = false;
            needsRefresh = true;
            String name =
                existingButton != null ? existingButton.getName() : "";
            if (nameField != null) {
                nameField.setValue(name);
                nameField.setSuggestion(
                    name.isEmpty() ? "Enter button name..." : ""
                );
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
        String name = existingButton != null ? existingButton.getName() : "";
        if (nameField != null) {
            nameField.setValue(name);
            nameField.setSuggestion(
                name.isEmpty() ? "Enter button name..." : ""
            );
        }
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.flat(UITheme.COL_DIM))
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout panel = UIContainers.verticalFlow(
            Sizing.fixed(320),
            Sizing.content()
        );
        panel.surface(UITheme.panel()).padding(Insets.of(8));

        FlowLayout titleWrap = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        titleWrap.horizontalAlignment(HorizontalAlignment.CENTER);
        titleWrap.child(
            UIComponents.label(
                Component.literal(
                    isAddMode ? "Add New Button" : "Edit Button"
                ).withStyle(ChatFormatting.BOLD)
            ).color(Color.ofArgb(UITheme.COL_TEXT))
        );
        titleWrap.margins(Insets.bottom(8));
        panel.child(titleWrap);

        FlowLayout nameCard = card();

        // 3.4-style name field: dark strip + top hairline, borderless text.
        FlowLayout nameStrip = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.fixed(16)
        );
        nameStrip.surface(UITheme.inputStrip());
        nameStrip.verticalAlignment(VerticalAlignment.CENTER);
        nameField = UIComponents.textBox(Sizing.fill(100));
        nameField.setBordered(false);
        nameField.setTextColor(UITheme.COL_TEXT);
        nameField.setMaxLength(100);
        nameField.setSuggestion("Enter button name...");
        nameField.onChanged().subscribe(s -> {
            nameField.setSuggestion(s.isEmpty() ? "Enter button name..." : "");
            updatePreview();
        });
        if (existingButton != null) nameField.setValue(
            existingButton.getName()
        );
        nameStrip.child(nameField);
        nameCard.child(nameStrip);
        panel.child(nameCard);

        FlowLayout appearCard = card();

        FlowLayout colorIconRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        colorIconRow.gap(4).margins(Insets.bottom(6));

        FlowLayout colorButton = clickableCard(
            Component.literal(colorLabel()), UITheme.COL_ACCENT_DIM, PAIR_BTN,
            () -> openColorPicker()
        );
        colorButton.tooltip(Component.literal("Click to pick a color"));

        FlowLayout iconButton = clickableCard(
            Component.literal(iconLabel()), UITheme.COL_ACCENT_DIM, PAIR_BTN,
            () -> openIconPicker()
        );
        iconButton.tooltip(Component.literal("Click to pick an icon"));

        colorIconRow.child(colorButton).child(iconButton);
        appearCard.child(colorIconRow);

        FlowLayout previewBox = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        previewBox
            .surface(UITheme.inputStrip())
            .padding(Insets.of(5))
            .verticalAlignment(VerticalAlignment.CENTER);
        previewBox.child(
            UIComponents.label(
                Component.literal("Preview  ")
            ).color(Color.ofArgb(UITheme.COL_TEXT_DIM))
        );
        previewLabel = UIComponents.label(Component.literal(""));
        previewLabel.color(Color.ofArgb(UITheme.COL_TEXT));
        previewBox.child(previewLabel);
        appearCard.child(previewBox);

        panel.child(appearCard);

        FlowLayout actionsCard = card();
        actionsCard.child(cardHeader("Commands"));

        commandListLayout = UIContainers.verticalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        // Compact command list area: fits ~3 rows on screen, no big scrollbar.
        var scroll = UIContainers.verticalScroll(
            Sizing.fill(100),
            Sizing.fixed(84),
            commandListLayout
        );
        scroll.margins(Insets.bottom(6));
        actionsCard.child(scroll);

        FlowLayout addCmdRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        addCmdRow.gap(4);

        FlowLayout addCmdBtn = clickablePill(
            Component.literal(UITheme.fmt(UITheme.COL_CMD) + "+ Command"),
            UITheme.COL_CMD, UITheme.COL_CMD_DIM, false, PAIR_BTN, () -> {
                commandWidgets.add(
                    new CommandWidget("/", ButtonData.CommandType.COMMAND)
                );
                rebuildCommandList();
                needsRefresh = true;
            }
        );
        addCmdBtn.tooltip(
            Component.literal("Add a slash command  (e.g. /tp, /gamemode)")
        );

        FlowLayout addMsgBtn = clickablePill(
            Component.literal(UITheme.fmt(UITheme.COL_MSG) + "+ Message"),
            UITheme.COL_MSG, UITheme.COL_MSG_DIM, false, PAIR_BTN, () -> {
                commandWidgets.add(
                    new CommandWidget("", ButtonData.CommandType.MESSAGE)
                );
                rebuildCommandList();
                needsRefresh = true;
            }
        );
        addMsgBtn.tooltip(
            Component.literal("Add a plain chat message to send")
        );

        addCmdRow.child(addCmdBtn).child(addMsgBtn);
        actionsCard.child(addCmdRow);
        panel.child(actionsCard);

        FlowLayout saveRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        saveRow.gap(4);

        FlowLayout saveBtn = clickablePill(
            Component.literal(UITheme.fmt(UITheme.COL_ACCENT) + "✔  Save"),
            UITheme.COL_ACCENT, UITheme.COL_ACCENT_DIM, true, SAVE_BTN,
            this::saveAndClose
        );

        FlowLayout cancelBtn = clickablePill(
            Component.literal(UITheme.fmt(UITheme.COL_DANGER) + "✖  Cancel"),
            UITheme.COL_DANGER, UITheme.COL_DANGER_DIM, false, SAVE_BTN,
            this::onClose
        );

        saveRow.child(saveBtn).child(cancelBtn);
        panel.child(saveRow);

        // ---- Old full-screen scroll (kept commented out for reference) ----
        // var mainScroll = UIContainers.verticalScroll(
        //     Sizing.fill(100),
        //     Sizing.fill(100),
        //     panel
        // );
        // root.child(mainScroll);

        // Scrollable panel: supports mouse-wheel AND click-and-drag scrolling,
        // keeps the panel centered (not stretched over the whole screen).
        // Use a clearly visible vanilla-style scrollbar instead of the default
        // near-invisible dark one, and make it a bit thicker.
        var mainScroll = new DragScrollContainer<>(
            ScrollContainer.ScrollDirection.VERTICAL,
            Sizing.fixed(320),
            Sizing.fill(100),
            panel
        );
        mainScroll
            .scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
            .scrollbarThiccness(5);
        root.child(mainScroll);
        rebuildCommandList();
        updatePreview();
    }

    private static FlowLayout card() {
        FlowLayout card = UIContainers.verticalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        card.surface(UITheme.subCard())
            .padding(Insets.of(6))
            .margins(Insets.bottom(6));
        return card;
    }

    private static LabelComponent cardHeader(String text) {
        LabelComponent label = UIComponents.label(
            Component.literal(text)
        );
        label.color(Color.ofArgb(UITheme.COL_TEXT_DIM)).margins(Insets.bottom(4));
        return label;
    }

    /**
     * Card-style clickable element (FlowLayout + label), because owo leaf
     * buttons cannot take custom surfaces. Left-click triggers onClick.
     */
    private FlowLayout clickableCard(
        Component text, int accent, int width, Runnable onClick
    ) {
        FlowLayout card = UIContainers.horizontalFlow(
            Sizing.fixed(width), Sizing.fixed(18)
        );
        card.surface(UITheme.card(accent));
        card.horizontalAlignment(HorizontalAlignment.CENTER);
        card.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent lbl = UIComponents.label(text);
        lbl.shadow(false);
        card.child(lbl);
        card.mouseDown().subscribe((click, doubled) -> {
            if (click.button() == 0) { onClick.run(); return true; }
            return false;
        });
        return card;
    }

    /** Pill-style clickable element (FlowLayout + label) for the save row. */
    private FlowLayout clickablePill(
        Component text, int accent, int accentDim, boolean active,
        int width, Runnable onClick
    ) {
        FlowLayout pill = UIContainers.horizontalFlow(
            Sizing.fixed(width), Sizing.fixed(18)
        );
        pill.surface(UITheme.pill(accent, accentDim, active));
        pill.horizontalAlignment(HorizontalAlignment.CENTER);
        pill.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent lbl = UIComponents.label(text);
        lbl.shadow(false);
        pill.child(lbl);
        pill.mouseDown().subscribe((click, doubled) -> {
            if (click.button() == 0) { onClick.run(); return true; }
            return false;
        });
        return pill;
    }

    private void updatePreview() {
        if (previewLabel == null) return;
        String name = nameField != null ? nameField.getValue() : "";
        if (name.trim().isEmpty()) name = "§8(no name)";
        StringBuilder sb = new StringBuilder();
        if (
            currentIcon != null && currentIcon != ButtonData.ButtonIcon.NONE
        ) sb.append(currentIcon.getSymbol()).append(" ");
        if (
            currentColor != null &&
            currentColor != ButtonData.ButtonColor.DEFAULT
        ) sb.append(currentColor.getCode());
        sb.append(name);
        previewLabel.text(Component.literal(sb.toString()));
    }

    private String colorLabel() {
        if (
            currentColor == ButtonData.ButtonColor.DEFAULT
        ) return "§7Color: Default";
        return "Color: " + currentColor.getDisplayName();
    }

    private String iconLabel() {
        if (
            currentIcon == null || currentIcon == ButtonData.ButtonIcon.NONE
        ) return "§7Icon: None";
        return (
            "Icon: " + currentIcon.getSymbol() + "  " + currentIcon.getName()
        );
    }

    private void rebuildCommandList() {
        if (commandListLayout == null) return;
        commandListLayout.clearChildren();

        if (commandWidgets.isEmpty()) {
            commandListLayout.child(
                UIComponents.label(
                    Component.literal(
                        "No actions yet — add a "
                        + UITheme.fmt(UITheme.COL_CMD) + "Command"
                        + UITheme.fmt(UITheme.COL_TEXT_DIM) + " or "
                        + UITheme.fmt(UITheme.COL_MSG) + "Message"
                        + UITheme.fmt(UITheme.COL_TEXT_DIM) + " below"
                    )
                ).color(Color.ofArgb(UITheme.COL_TEXT_DIM)).margins(Insets.of(6))
            );
            return;
        }

        for (int i = 0; i < commandWidgets.size(); i++) {
            final int idx = i;
            CommandWidget cw = commandWidgets.get(i);
            boolean isCmd = cw.type == ButtonData.CommandType.COMMAND;
            int accent = isCmd ? UITheme.COL_CMD : UITheme.COL_MSG;
            int accentDim = isCmd ? UITheme.COL_CMD_DIM : UITheme.COL_MSG_DIM;

            // 3.4-style row: tinted translucent background (blue for CMD,
            // green for MSG), badge pill, borderless field, mini action pills.
            FlowLayout rowWrap = UIContainers.verticalFlow(
                Sizing.fill(100),
                Sizing.content()
            );
            rowWrap
                .surface(Surface.flat(accentDim))
                .padding(Insets.of(2))
                .margins(Insets.bottom(2));

            FlowLayout row = UIContainers.horizontalFlow(
                Sizing.fill(100),
                Sizing.content()
            );
            row.gap(2).verticalAlignment(VerticalAlignment.CENTER);

            FlowLayout typeBtn = clickablePill(
                Component.literal(UITheme.fmt(accent) + (isCmd ? "CMD" : "MSG")),
                accent, accentDim, false, 34, () -> {
                    cw.type = isCmd
                        ? ButtonData.CommandType.MESSAGE
                        : ButtonData.CommandType.COMMAND;
                    rebuildCommandList();
                    // Force-refresh the recreated text boxes on the next tick;
                    // without this the field text would stay invisible until
                    // the field is clicked again.
                    needsRefresh = true;
                }
            );
            typeBtn.tooltip(
                Component.literal(
                    isCmd
                        ? "§9Command§r: runs /command\nClick to switch to §aMessage"
                        : "§aMessage§r: sends chat text\nClick to switch to §9Command"
                )
            );

            TextBoxComponent field = cw.createField(FIELD_W);

            FlowLayout upBtn = clickablePill(
                Component.literal("↑"),
                UITheme.COL_TEXT_DIM, 0x18FFFFFF, false, 14, () -> {
                    if (idx > 0) {
                        CommandWidget tmp = commandWidgets.get(idx);
                        commandWidgets.set(idx, commandWidgets.get(idx - 1));
                        commandWidgets.set(idx - 1, tmp);
                        rebuildCommandList();
                        needsRefresh = true;
                    }
                }
            );

            FlowLayout downBtn = clickablePill(
                Component.literal("↓"),
                UITheme.COL_TEXT_DIM, 0x18FFFFFF, false, 14, () -> {
                    if (idx < commandWidgets.size() - 1) {
                        CommandWidget tmp = commandWidgets.get(idx);
                        commandWidgets.set(idx, commandWidgets.get(idx + 1));
                        commandWidgets.set(idx + 1, tmp);
                        rebuildCommandList();
                        needsRefresh = true;
                    }
                }
            );

            FlowLayout delBtn = clickablePill(
                Component.literal("✕"),
                UITheme.COL_DANGER, 0x18FFFFFF, false, 16, () -> {
                    commandWidgets.remove(idx);
                    rebuildCommandList();
                    needsRefresh = true;
                }
            );

            row.child(typeBtn)
                .child(field)
                .child(upBtn)
                .child(downBtn)
                .child(delBtn);
            rowWrap.child(row);
            commandListLayout.child(rowWrap);
        }
    }

    // No longer used (color button now opens the picker); kept for reference.
    // private void cycleColor() {
    //     ButtonData.ButtonColor[] colors = ButtonData.ButtonColor.values();
    //     int cur = Arrays.asList(colors).indexOf(currentColor);
    //     currentColor = colors[(cur + 1) % colors.length];
    //     if (colorButton != null) colorButton.setMessage(
    //         Component.literal(colorLabel())
    //     );
    //     updatePreview();
    // }

    private void openColorPicker() {
        ButtonData current = buildButtonData();
        Minecraft.getInstance().setScreen(
            new ColorSelectionScreen(
                color -> {
                    current.setColor(color);
                    Minecraft.getInstance().setScreen(
                        new EditButtonScreen(current, onSave, isAddMode)
                    );
                },
                () ->
                    Minecraft.getInstance().setScreen(
                        new EditButtonScreen(current, onSave, isAddMode)
                    )
            )
        );
    }

    private void openIconPicker() {
        ButtonData current = buildButtonData();
        Minecraft.getInstance().setScreen(
            new IconSelectionScreen(
                icon -> {
                    current.setIcon(icon);
                    Minecraft.getInstance().setScreen(
                        new EditButtonScreen(current, onSave, isAddMode)
                    );
                },
                () ->
                    Minecraft.getInstance().setScreen(
                        new EditButtonScreen(current, onSave, isAddMode)
                    )
            )
        );
    }

    private ButtonData buildButtonData() {
        String name =
            nameField != null
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

        TextBoxComponent createField(int width) {
            textField = UIComponents.textBox(Sizing.fixed(width));
            textField.setBordered(false);
            textField.setTextColor(UITheme.COL_TEXT);
            textField.setValue(text);
            textField.setMaxLength(256);
            textField.onChanged().subscribe(s -> text = s);
            return textField;
        }

        String getText() {
            return textField != null ? textField.getValue() : text;
        }

        ButtonData.CommandType getType() {
            return type;
        }
    }
}
