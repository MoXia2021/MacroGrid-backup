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
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import real.o0h.config.ButtonData;
import real.o0h.config.ButtonManager;

public class ButtonGuiScreen extends BaseOwoScreen<FlowLayout> {

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

    private ButtonComponent editModeBtn;
    private ButtonComponent moveModeBtn;
    private ButtonComponent deleteModeBtn;

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
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.VANILLA_TRANSLUCENT)
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER);

        // Panel height is content-sized; the grid area height is computed
        // from the window height so the action bar below stays pinned near
        // the bottom, and only the middle grid area scrolls.
        FlowLayout panel = UIContainers.verticalFlow(
            Sizing.fixed(340),
            Sizing.content()
        );
        panel.surface(Surface.flat(0xC0000000)).padding(Insets.of(6));

        FlowLayout titleRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        titleRow
            .verticalAlignment(VerticalAlignment.CENTER)
            .margins(Insets.bottom(6));
        titleRow.child(
            UIComponents.label(
                Component.literal("MacroGrid").withStyle(
                    ChatFormatting.YELLOW,
                    ChatFormatting.BOLD
                )
            )
        );
        countLabel = UIComponents.label(
            Component.literal("  (0)").withStyle(ChatFormatting.DARK_GRAY)
        );
        titleRow.child(countLabel);
        panel.child(titleRow);

        FlowLayout searchRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        searchBox = UIComponents.textBox(Sizing.fill(90));
        searchBox.setMaxLength(100);
        searchBox.setSuggestion("Search...");
        searchBox.onChanged().subscribe(s -> {
            searchBox.setSuggestion(s.isEmpty() ? "Search..." : "");
            rebuildGrid();
        });
        ButtonComponent clearBtn = UIComponents.button(
            Component.literal("X"),
            b -> searchBox.setValue("")
        );
        clearBtn.horizontalSizing(Sizing.fixed(18));
        searchRow
            .child(searchBox)
            .child(clearBtn)
            .gap(2)
            .margins(Insets.bottom(6));
        panel.child(searchRow);

        buttonGridLayout = UIContainers.verticalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        // Keep the button rows inside the grid area: reserve space on the
        // right so the scrollbar does not sit flush against the buttons.
        buttonGridLayout.margins(Insets.right(10));

        // ---- Old inner fixed-height scroll (kept commented out) ----
        // var scroll = UIContainers.verticalScroll(
        //     Sizing.fill(100),
        //     Sizing.fixed(160),
        //     buttonGridLayout
        // );
        // scroll.margins(Insets.bottom(6));
        // panel.child(scroll);

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

        ButtonComponent addBtn = UIComponents.button(
            Component.literal("§a+ Add"),
            b -> openAddScreen()
        );
        addBtn.horizontalSizing(Sizing.fixed(130));

        editModeBtn = UIComponents.button(Component.literal("§7✎ Edit"), b ->
            setMode(Mode.EDIT)
        );
        editModeBtn.horizontalSizing(Sizing.fixed(62));
        editModeBtn.tooltip(
            Component.literal("Edit mode: click a button to edit it")
        );

        moveModeBtn = UIComponents.button(Component.literal("§7⇄ Move"), b ->
            setMode(Mode.MOVE)
        );
        moveModeBtn.horizontalSizing(Sizing.fixed(62));
        moveModeBtn.tooltip(
            Component.literal(
                "Move mode: pick a button, then click where to swap it"
            )
        );

        deleteModeBtn = UIComponents.button(Component.literal("§7✖ Del"), b ->
            setMode(Mode.DELETE)
        );
        deleteModeBtn.horizontalSizing(Sizing.fixed(62));
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

        // ---- Old whole-panel scroll (kept commented out for reference) ----
        // var mainScroll = new DragScrollContainer<>(
        //     ScrollContainer.ScrollDirection.VERTICAL,
        //     Sizing.fixed(340),
        //     Sizing.fill(100),
        //     panel
        // );
        // mainScroll
        //     .scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
        //     .scrollbarThiccness(5);
        // root.child(mainScroll);

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
        if (editModeBtn != null) editModeBtn.setMessage(
            Component.literal(mode == Mode.EDIT ? "§e✎ Edit" : "§7✎ Edit")
        );
        if (moveModeBtn != null) moveModeBtn.setMessage(
            Component.literal(mode == Mode.MOVE ? "§9⇄ Move" : "§7⇄ Move")
        );
        if (deleteModeBtn != null) deleteModeBtn.setMessage(
            Component.literal(mode == Mode.DELETE ? "§c✖ Del" : "§7✖ Del")
        );
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
                ? "No buttons yet! Press §a+ Add§7 to create one."
                : "No results for \"§f" + search + "§7\"";
            buttonGridLayout.child(
                UIComponents.label(
                    Component.literal(msg).withStyle(ChatFormatting.DARK_GRAY)
                ).margins(Insets.of(8))
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
            ButtonComponent btn = UIComponents.button(
                Component.literal(makeLabel(button)),
                b -> handleClick(button)
            );
            btn.horizontalSizing(Sizing.fill(33));
            btn.tooltip(makeTooltip(button));
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
        return sb.toString();
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
