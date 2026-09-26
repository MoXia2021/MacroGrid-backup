package real.o0h.gui;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import real.o0h.config.ButtonData;
import real.o0h.config.ButtonManager;

public class ButtonGuiScreen extends MacroGridScreen {

    private enum Mode {
        NONE,
        EDIT,
        MOVE,
        DELETE,
    }

    private Mode mode = Mode.NONE;
    private ButtonData moveSource = null;
    private ButtonData pendingDelete = null;

    private static final int COLS = 3;

    private FlowLayout buttonGridLayout;
    private TextBoxComponent searchBox;
    private LabelComponent countLabel;

    private FlowLayout editModeBtn, moveModeBtn, deleteModeBtn;
    private LabelComponent editModeLbl, moveModeLbl, deleteModeLbl;

    // Profile tab bar (ported from 3.4)
    private FlowLayout tabBarLayout;
    private boolean renamingProfile = false;
    private boolean pendingDeleteProfile = false;
    private TextBoxComponent renameField;
    private boolean renameFocusPending = false;

    private boolean firstTick = true;

    @Override
    public void tick() {
        super.tick();
        if (firstTick) {
            firstTick = false;
            if (searchBox != null) searchBox.setValue("");
            if (uiAdapter != null) uiAdapter.rootComponent
                .focusHandler()
                .focus(null, UIComponent.FocusSource.MOUSE_CLICK);
        }
        if (renameFocusPending && renameField != null && uiAdapter != null) {
            renameFocusPending = false;
            uiAdapter.rootComponent
                .focusHandler()
                .focus(renameField, UIComponent.FocusSource.KEYBOARD_CYCLE);
        }
    }

    @Override
    protected void init() {
        super.init();
        if (uiAdapter != null) uiAdapter.rootComponent
            .focusHandler()
            .focus(null, UIComponent.FocusSource.MOUSE_CLICK);
        if (searchBox != null) searchBox.setValue("");
    }

    @Override
    protected void build(FlowLayout root) {
        configureRoot(root);

        // Panel height is content-sized; the grid area height is computed
        // from the window height so the action bar below stays pinned near
        // the bottom, and only the middle grid area scrolls.
        FlowLayout panel = createPanel(340, 8);

        FlowLayout titleRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        titleRow
            .verticalAlignment(VerticalAlignment.CENTER)
            .margins(Insets.bottom(6));
        titleRow.child(
            UIComponents.label(
                Component.literal("MacroGrid").withStyle(ChatFormatting.BOLD)
            ).color(Color.ofArgb(UITheme.COL_TEXT))
        );
        countLabel = UIComponents.label(Component.literal("  (0)"));
        countLabel.color(Color.ofArgb(UITheme.COL_TEXT_DIM));
        titleRow.child(countLabel);
        panel.child(titleRow);

        // Profile tab bar (ported from 3.4): switch / rename / delete /
        // create profiles. The active profile's buttons are shown below.
        tabBarLayout = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        tabBarLayout.verticalAlignment(VerticalAlignment.CENTER);
tabBarLayout.gap(2);
tabBarLayout.margins(Insets.bottom(6));
        panel.child(tabBarLayout);
        rebuildTabs();

        // Search row styled like 3.4: dark strip with a top hairline,
        // borderless field with light text.
        FlowLayout searchRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.fixed(18)
        );
        searchRow.surface(UITheme.inputStrip());
        searchRow
            .verticalAlignment(VerticalAlignment.CENTER)
            .margins(Insets.bottom(8));
        searchBox = UIComponents.textBox(Sizing.fill(90));
        searchBox.setBordered(false);
        searchBox.setTextColor(UITheme.COL_TEXT);
        searchBox.setMaxLength(100);
        searchBox.setSuggestion("Search...");
        searchBox.onChanged().subscribe(s -> {
            searchBox.setSuggestion(s.isEmpty() ? "Search..." : "");
            rebuildGrid();
        });
        FlowLayout clearBtn = UiFactory.clickablePill(
            Component.literal(UITheme.fmt(UITheme.COL_TEXT_DIM) + "✕"),
            UITheme.COL_TEXT_DIM, 0x18FFFFFF, false,
            18, () -> searchBox.setValue("")
        );
        searchRow
            .child(searchBox)
            .child(clearBtn)
            .gap(2);
        panel.child(searchRow);

        buttonGridLayout = UIContainers.verticalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        // Keep the button rows inside the grid area: reserve space on the
        // right so the scrollbar does not sit flush against the buttons.
        buttonGridLayout.margins(Insets.right(10));

        // The middle grid area is the only scrollable part of the screen.
        // Its height is computed from the window height (minus the fixed
        // chrome above and below it), so the action bar always stays visible
        // at the bottom. Supports mouse-wheel AND click-and-drag scrolling,
        // with a clearly visible scrollbar.
        int gridHeight = Math.max(
            140,
            Minecraft.getInstance().getWindow().getGuiScaledHeight() - 96
        );
        var gridScroll = new DragScrollContainer<>(
            ScrollContainer.ScrollDirection.VERTICAL,
            Sizing.fill(100),
            Sizing.fixed(gridHeight),
            buttonGridLayout
        );
        gridScroll
            .scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
            .scrollbarThiccness(5);
        gridScroll.margins(Insets.bottom(6));
        panel.child(gridScroll);

        FlowLayout bar = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        bar.gap(4);

        // Pill-style toolbar (3.4 look): translucent fill, accent text,
        // accent top line + tinted fill while active. Built from FlowLayout
        // containers because owo leaf buttons cannot take custom surfaces.
        FlowLayout addBtn = UiFactory.clickablePill(
            Component.literal(UITheme.fmt(UITheme.COL_ACCENT) + "+ Add"),
            UITheme.COL_ACCENT, UITheme.COL_ACCENT_DIM, false,
            130, this::openAddScreen
        );

        editModeBtn = UiFactory.clickablePill(
            Component.literal(UITheme.fmt(UITheme.COL_WARN) + "✎ Edit"),
            UITheme.COL_WARN, UITheme.COL_WARN_DIM, false,
            62, () -> setMode(Mode.EDIT)
        );
        editModeLbl = (LabelComponent) editModeBtn.children().get(0);
        editModeBtn.tooltip(
            Component.literal("Edit mode: click a button to edit it")
        );

        moveModeBtn = UiFactory.clickablePill(
            Component.literal(UITheme.fmt(UITheme.COL_MOVE) + "⇄ Move"),
            UITheme.COL_MOVE, UITheme.COL_MOVE_DIM, false,
            62, () -> setMode(Mode.MOVE)
        );
        moveModeLbl = (LabelComponent) moveModeBtn.children().get(0);
        moveModeBtn.tooltip(
            Component.literal(
                "Move mode: pick a button, then click where to swap it"
            )
        );

        deleteModeBtn = UiFactory.clickablePill(
            Component.literal(UITheme.fmt(UITheme.COL_DANGER) + "✖ Del"),
            UITheme.COL_DANGER, UITheme.COL_DANGER_DIM, false,
            62, () -> setMode(Mode.DELETE)
        );
        deleteModeLbl = (LabelComponent) deleteModeBtn.children().get(0);
        deleteModeBtn.tooltip(
            Component.literal(
                "Delete mode: click a button to delete it (two clicks to confirm)"
            )
        );

        bar.child(addBtn)
            .child(editModeBtn)
            .child(moveModeBtn)
            .child(deleteModeBtn);
        panel.child(bar);
        root.child(panel);
        rebuildGrid();
    }

    private void setMode(Mode newMode) {
        mode = (mode == newMode) ? Mode.NONE : newMode;
        if (mode != Mode.MOVE) moveSource = null;
        pendingDelete = null;
        updateModeButtons();
        rebuildGrid();
    }

    private void updateModeButtons() {
        if (editModeBtn != null) {
            boolean act = mode == Mode.EDIT;
            editModeBtn.surface(UITheme.pill(UITheme.COL_WARN, UITheme.COL_WARN_DIM, act));
            editModeLbl.text(Component.literal(
                (act ? UITheme.fmt(UITheme.COL_WARN) : UITheme.fmt(UITheme.COL_TEXT_DIM)) + "✎ Edit"
            ));
        }
        if (moveModeBtn != null) {
            boolean act = mode == Mode.MOVE;
            moveModeBtn.surface(UITheme.pill(UITheme.COL_MOVE, UITheme.COL_MOVE_DIM, act));
            moveModeLbl.text(Component.literal(
                (act ? UITheme.fmt(UITheme.COL_MOVE) : UITheme.fmt(UITheme.COL_TEXT_DIM)) + "⇄ Move"
            ));
        }
        if (deleteModeBtn != null) {
            boolean act = mode == Mode.DELETE;
            deleteModeBtn.surface(UITheme.pill(UITheme.COL_DANGER, UITheme.COL_DANGER_DIM, act));
            deleteModeLbl.text(Component.literal(
                (act ? UITheme.fmt(UITheme.COL_DANGER) : UITheme.fmt(UITheme.COL_TEXT_DIM)) + "✖ Del"
            ));
        }
    }

    /**
     * (Re)build the profile tab bar: tab pills for each profile, rename/delete
     * icons on the active tab, a trailing "+" to create a new profile. While
     * renaming, shows an inline name field with confirm/cancel icons instead.
     */
    private void rebuildTabs() {
        if (tabBarLayout == null) return;
        tabBarLayout.clearChildren();

        List<String> names = ButtonManager.getProfileNames();
        String current = ButtonManager.getCurrentProfile();

        if (renamingProfile) {
            FlowLayout editRow = UIContainers.horizontalFlow(
                Sizing.fill(100),
                Sizing.fixed(18)
            );
            editRow.surface(UITheme.inputStrip());
            editRow.verticalAlignment(VerticalAlignment.CENTER);
            renameField = UIComponents.textBox(Sizing.fill(72));
            renameField.setBordered(false);
            renameField.setTextColor(UITheme.COL_TEXT);
            renameField.setMaxLength(24);
            renameField.setValue(current);
            renameField.setSuggestion("");
            // Select the whole name so typing replaces it instead of appending.
            renameField.setCursorPosition(renameField.getValue().length());
            renameField.setHighlightPos(0);
            renameField.onChanged().subscribe(s ->
                renameField.setSuggestion(s.isEmpty() ? "Profile name" : "")
            );
            editRow.child(renameField);
            FlowLayout okBtn = UiFactory.clickablePill(
                Component.literal(UITheme.fmt(UITheme.COL_ACCENT) + "✓"),
                UITheme.COL_ACCENT, UITheme.COL_ACCENT_DIM, false,
                18, this::commitRename
            );
            okBtn.tooltip(Component.literal("Confirm rename"));
            FlowLayout cancelBtn = UiFactory.clickablePill(
                Component.literal(UITheme.fmt(UITheme.COL_DANGER) + "✕"),
                UITheme.COL_DANGER, UITheme.COL_DANGER_DIM, false,
                18, () -> { renamingProfile = false; rebuildTabs(); }
            );
            cancelBtn.tooltip(Component.literal("Cancel rename"));
            editRow.child(okBtn);
            editRow.child(cancelBtn);
            editRow.gap(2);
            tabBarLayout.child(editRow);
            renameFocusPending = true;
            return;
        }

        for (String name : names) {
            boolean active = name.equals(current);
            FlowLayout tab = UIContainers.horizontalFlow(
                Sizing.content(),
                Sizing.fixed(18)
            );
            tab.surface(UITheme.tab(active));
            tab.horizontalAlignment(HorizontalAlignment.CENTER);
            tab.verticalAlignment(VerticalAlignment.CENTER);
            tab.padding(Insets.horizontal(8));
            LabelComponent lbl = UIComponents.label(Component.literal(name));
            lbl.shadow(false);
            lbl.color(Color.ofArgb(active ? UITheme.COL_TEXT : UITheme.COL_TEXT_DIM));
            tab.child(lbl);
            final String tabName = name;
            tab.mouseDown().subscribe((click, doubled) -> {
                if (click.button() == 0) {
                    if (!tabName.equals(ButtonManager.getCurrentProfile())) {
                        ButtonManager.setCurrentProfile(tabName);
                        reopen();
                    }
                    return true;
                }
                return false;
            });
            tab.tooltip(Component.literal(
                active ? "Current profile" : "Switch to " + tabName
            ));
            tabBarLayout.child(tab);

            if (active) {
                // Rename icon: edit the current profile's name.
                FlowLayout renameIcon = UiFactory.clickablePill(
                    Component.literal("✎"),
                    UITheme.COL_TEXT_DIM, 0x18FFFFFF, false,
                    14, () -> { renamingProfile = true; rebuildTabs(); }
                );
                renameIcon.tooltip(Component.literal("Rename profile"));
                tabBarLayout.child(renameIcon);

                // Delete icon, only when more than one profile exists.
                if (names.size() > 1) {
                    FlowLayout delIcon = UiFactory.clickablePill(
                        Component.literal(
                            pendingDeleteProfile
                                ? UITheme.fmt(UITheme.COL_DANGER) + "✕"
                                : "✕"
                        ),
                        UITheme.COL_DANGER, 0x18FFFFFF, pendingDeleteProfile,
                        14, () -> {
                            if (pendingDeleteProfile) {
                                ButtonManager.deleteProfile(
                                    ButtonManager.getCurrentProfile()
                                );
                                pendingDeleteProfile = false;
                                reopen();
                            } else {
                                pendingDeleteProfile = true;
                                rebuildTabs();
                            }
                        }
                    );
                    delIcon.tooltip(Component.literal(
                        pendingDeleteProfile
                            ? "Click again to confirm deletion"
                            : "Delete profile"
                    ));
                    tabBarLayout.child(delIcon);
                }
            }
        }

        // Create-new-profile icon at the end of the bar.
        FlowLayout addIcon = UiFactory.clickablePill(
            Component.literal(UITheme.fmt(UITheme.COL_ACCENT) + "+"),
            UITheme.COL_ACCENT, UITheme.COL_ACCENT_DIM, false,
            14, () -> {
                ButtonManager.createProfile(null);
                reopen();
            }
        );
        addIcon.tooltip(Component.literal("Create new profile"));
        tabBarLayout.child(addIcon);
    }

    /** Confirm the inline profile rename and reopen. */
    private void commitRename() {
        if (renameField == null) return;
        String newName = renameField.getValue().trim();
        if (!newName.isEmpty()) {
            ButtonManager.renameProfile(
                ButtonManager.getCurrentProfile(), newName
            );
        }
        renamingProfile = false;
        reopen();
    }

    /** Reopen the main screen after a profile operation. */
    private void reopen() {
        Minecraft.getInstance().setScreen(new ButtonGuiScreen());
    }

    private void rebuildGrid() {
        if (buttonGridLayout == null) return;
        buttonGridLayout.clearChildren();

        if (countLabel != null) {
            int total = ButtonManager.getButtons().size();
            countLabel.text(
                Component.literal("  (" + total + ")").withStyle(
                    ChatFormatting.DARK_GRAY
                )
            );
        }

        String search =
            searchBox != null ? searchBox.getValue().toLowerCase() : "";
        List<ButtonData> all = ButtonManager.getButtons();
        List<ButtonData> list = search.isEmpty()
            ? all
            : all
                  .stream()
                  .filter(b -> b.getName().toLowerCase().contains(search))
                  .toList();

        if (list.isEmpty()) {
            String msg = search.isEmpty()
                ? "No buttons yet! Press " + UITheme.fmt(UITheme.COL_ACCENT) + "+ Add" + UITheme.fmt(UITheme.COL_TEXT_DIM) + " to create one."
                : "No results for \"" + UITheme.fmt(UITheme.COL_TEXT) + search + UITheme.fmt(UITheme.COL_TEXT_DIM) + "\"";
            buttonGridLayout.child(
                UIComponents.label(
                    Component.literal(msg)
                ).color(Color.ofArgb(UITheme.COL_TEXT_DIM)).margins(Insets.of(8))
            );
            return;
        }

        FlowLayout row = null;
        int col = 0;

        for (ButtonData button : list) {
            if (col == 0) {
                row = UIContainers.horizontalFlow(
                    Sizing.fill(100),
                    Sizing.content()
                );
                row.gap(2).margins(Insets.bottom(2));
            }
            FlowLayout btn = UiFactory.clickableCard(
                Component.literal(makeLabel(button)),
                cardAccent(button),
                33, 22,
                (mode == Mode.DELETE && pendingDelete == button)
                    || (mode == Mode.MOVE && moveSource == button),
                () -> handleClick(button), makeTooltip(button)
            );
            row.child(btn);
            col++;
            if (col >= COLS) {
                buttonGridLayout.child(row);
                col = 0;
                row = null;
            }
        }
        if (row != null) buttonGridLayout.child(row);
    }

    /** Left accent stripe color for a grid card, matching the 3.4 modes. */
    private int cardAccent(ButtonData btn) {
        return switch (mode) {
            case DELETE -> UITheme.COL_DANGER;
            case EDIT -> UITheme.COL_WARN;
            case MOVE -> UITheme.COL_MOVE;
            case NONE -> UITheme.COL_ACCENT_DIM;
        };
    }

    /**
     * Card-style clickable element: FlowLayout container + centered label,
     * because owo leaf buttons cannot take custom surfaces. Left-click
     * triggers onClick; optional tooltip lines.
     */
    private String makeLabel(ButtonData btn) {
        StringBuilder sb = new StringBuilder();
        switch (mode) {
            case DELETE -> {
                if (btn == pendingDelete) sb.append("§cSure? ");
                else sb.append("§c✖ ");
            }
            case EDIT -> sb.append("§e✎ ");
            case MOVE -> {
                if (moveSource == btn) sb.append("§a>> ");
                else if (moveSource != null) sb.append("§6→ ");
                else sb.append("§9:: ");
            }
            case NONE -> {
                if (
                    btn.getIcon() != null &&
                    btn.getIcon() != ButtonData.ButtonIcon.NONE
                ) sb.append(btn.getIcon().getSymbol()).append(" ");
                if (
                    btn.getColor() != null &&
                    btn.getColor() != ButtonData.ButtonColor.DEFAULT
                ) sb.append(btn.getColor().getCode());
            }
        }
        sb.append(btn.getName());
        return clipToWidth(sb.toString(), 88);
    }

    /**
     * Clip text to a max pixel width so long button names do not overflow
     * the button. Works on visible characters while preserving the § format
     * codes attached to each character, and appends an ellipsis. The full
     * name is still shown in the button tooltip.
     */
    private String clipToWidth(String text, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(Component.literal(text)) <= maxWidth) return text;

        // Split into visible chars, each carrying its leading format codes.
        java.util.List<String> pieces = new java.util.ArrayList<>();
        StringBuilder codes = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                codes.append(c).append(text.charAt(++i));
                continue;
            }
            pieces.add(codes.toString() + c);
            codes.setLength(0);
        }

        StringBuilder acc = new StringBuilder();
        StringBuilder kept = new StringBuilder();
        String lastCodes = "";
        for (String piece : pieces) {
            int codeEnd = 0;
            while (codeEnd + 1 < piece.length() && piece.charAt(codeEnd) == '§') codeEnd += 2;
            lastCodes = piece.substring(0, codeEnd);
            acc.append(piece);
            if (font.width(Component.literal(acc.toString())) > maxWidth) break;
            kept.append(piece);
        }
        return kept + lastCodes + "…";
    }

    private List<Component> makeTooltip(ButtonData btn) {
        var lines = new java.util.ArrayList<Component>();
        lines.add(
            Component.literal(btn.getName()).withStyle(ChatFormatting.YELLOW)
        );
        switch (mode) {
            case DELETE -> lines.add(
                Component.literal(
                    btn == pendingDelete
                        ? "Click again to confirm deletion"
                        : "Click to DELETE"
                ).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
            );
            case EDIT -> lines.add(
                Component.literal("Click to EDIT").withStyle(
                    ChatFormatting.YELLOW
                )
            );
            case MOVE -> lines.add(
                Component.literal(
                    moveSource == null
                        ? "Click to pick up"
                        : "Click to swap here"
                ).withStyle(ChatFormatting.GOLD)
            );
            case NONE -> {
                int n = 0;
                for (ButtonData.CommandEntry cmd : btn.getCommands()) {
                    if (n++ >= 4) {
                        lines.add(
                            Component.literal("...").withStyle(
                                ChatFormatting.DARK_GRAY
                            )
                        );
                        break;
                    }
                    lines.add(
                        Component.literal(cmd.getCommand()).withStyle(
                            ChatFormatting.GRAY
                        )
                    );
                }
            }
        }
        return lines;
    }

    private void handleClick(ButtonData button) {
        switch (mode) {
            case NONE -> execute(button);
            case EDIT -> {
                mode = Mode.NONE;
                updateModeButtons();
                openEditScreen(button);
            }
            case DELETE -> {
                if (pendingDelete == button) {
                    deleteButton(button);
                    pendingDelete = null;
                } else {
                    pendingDelete = button;
                    rebuildGrid();
                }
            }
            case MOVE -> {
                if (moveSource == null) {
                    moveSource = button;
                    rebuildGrid();
                } else if (moveSource == button) {
                    moveSource = null;
                    rebuildGrid();
                } else {
                    swapButtons(moveSource, button);
                    moveSource = null;
                    mode = Mode.NONE;
                    updateModeButtons();
                    rebuildGrid();
                }
            }
        }
    }

    private void swapButtons(ButtonData a, ButtonData b) {
        List<ButtonData> list = ButtonManager.getButtons();
        int i = list.indexOf(a),
            j = list.indexOf(b);
        if (i != -1 && j != -1) {
            ButtonData tmp = list.get(i);
            ButtonManager.updateButton(i, list.get(j));
            ButtonManager.updateButton(j, tmp);
        }
    }

    private void execute(ButtonData button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        for (ButtonData.CommandEntry e : button.getCommands()) {
            String cmd = e.getCommand();
            if (e.getType() == ButtonData.CommandType.COMMAND) {
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                mc.player.connection.sendCommand(cmd);
            } else {
                mc.player.connection.sendChat(cmd);
            }
        }
        onClose();
    }

    private void openAddScreen() {
        Minecraft.getInstance().setScreen(
            new EditButtonScreen(null, btn -> {
                ButtonManager.addButton(btn);
                Minecraft.getInstance().setScreen(new ButtonGuiScreen());
            })
        );
    }

    private void openEditScreen(ButtonData button) {
        int index = ButtonManager.getButtons().indexOf(button);
        Minecraft.getInstance().setScreen(
            new EditButtonScreen(button, updated -> {
                ButtonManager.updateButton(index, updated);
                Minecraft.getInstance().setScreen(new ButtonGuiScreen());
            })
        );
    }

    private void deleteButton(ButtonData button) {
        List<ButtonData> list = ButtonManager.getButtons();
        int i = list.indexOf(button);
        if (i != -1) ButtonManager.removeButton(i);
        rebuildGrid();
    }
}
