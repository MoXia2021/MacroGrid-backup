package real.o0h.macrogrid.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import real.o0h.macrogrid.client.config.ButtonData;
import real.o0h.macrogrid.client.config.ButtonManager;

public class ButtonGuiScreen extends Screen {

    private static final int COL_DIM = 0x99000000;
    private static final int COL_PANEL = 0xF0181A22;
    private static final int COL_PANEL_TOP = 0x1AFFFFFF;
    private static final int COL_PANEL_BORDER = 0x33FFFFFF;
    private static final int COL_ACCENT = 0xFF5B8CFF;
    private static final int COL_ACCENT_DIM = 0x405B8CFF;
    private static final int COL_CARD = 0x1EFFFFFF;
    private static final int COL_CARD_HOVER = 0x38FFFFFF;
    private static final int COL_CARD_BORDER = 0x22FFFFFF;
    private static final int COL_TEXT = 0xFFEDEDF2;
    private static final int COL_TEXT_DIM = 0xFF8B93A6;
    private static final int COL_DANGER = 0xFFE5605B;
    private static final int COL_DANGER_DIM = 0x40E5605B;
    private static final int COL_WARN = 0xFFE2B93B;
    private static final int COL_WARN_DIM = 0x40E2B93B;
    private static final int COL_MOVE = 0xFF5B8CFF;
    private static final int COL_MOVE_DIM = 0x405B8CFF;

    private enum Mode { NONE, EDIT, MOVE, DELETE }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private static final int COLS = 3;
    private static final int CARD_H = 34;
    private static final int CARD_GAP = 4;
    private static final int VISIBLE_ROWS = 4;
    private static final int TAB_H = 18;

    private Mode mode = Mode.NONE;
    private ButtonData moveSource;
    private ButtonData pendingDelete;

    private EditBox searchBox;
    private int scroll = 0;
    private int maxScroll = 0;

    // Click-and-drag scrolling state for the grid area.
    private boolean dragging = false;
    private double dragLastY = 0;

    private static boolean OPEN = false;
    public static boolean isOpen() { return OPEN; }
    static void setOpen(boolean open) { OPEN = open; }

    private List<ButtonData> filtered = new ArrayList<>();


    private int panelX, panelY, panelW, panelH, searchH;
    private Rect titleRect, closeRect, clearRect, gridRect, hintRect;
    private Rect addRect, editRect, moveRect, deleteRect;
    private int cardW;


    private Rect tabBarRect;
    private final List<Rect> tabRects = new ArrayList<>();
    private List<String> tabNames = new ArrayList<>();
    private Rect tabRenameRect, tabDeleteRect, tabAddRect;
    private int tabContentW;
    private int tabScroll = 0;
    private boolean pendingDeleteProfile = false;
    private boolean renamingProfile = false;
    private EditBox renameBox;

    public ButtonGuiScreen() {
        super(Component.translatable("macrogrid.title"));
    }

    @Override
    public void onClose() {
        ButtonGuiScreen.setOpen(false);
        super.onClose();
    }

    @Override
    public void removed() {
        ButtonGuiScreen.setOpen(false);
        super.removed();
    }

    @Override
    protected void init() {
        ButtonGuiScreen.setOpen(true);
        Component addLabel = Component.translatable("macrogrid.toolbar.add");
        Component editLabel = Component.translatable("macrogrid.toolbar.edit");
        Component moveLabel = Component.translatable("macrogrid.toolbar.move");
        Component delLabel = Component.translatable("macrogrid.toolbar.delete");
        int tPad = 14;
        int addW0 = this.font.width(addLabel) + tPad;
        int editW0 = this.font.width(editLabel) + tPad;
        int moveW0 = this.font.width(moveLabel) + tPad;
        int delW0 = this.font.width(delLabel) + tPad;
        int tGap = 4;
        int toolbarNeeded = addW0 + editW0 + moveW0 + delW0 + tGap * 3;

        panelW = Math.max(360, Math.min(400, toolbarNeeded + 20));

        // Grid area height adapts to the window: the panel fills the window
        // height (minus a small margin) so the grid is as large as possible,
        // while the action bar below stays pinned and only the grid scrolls.
        int titleH = 26;
        int tabGap = 4;
        searchH = 16;
        int hintGap = 5;
        int hintH = 12;
        int barGap = 8;
        int barH = 20;
        int bottomPad = 10;
        int searchGridGap = 8;
        int chromeH = titleH + TAB_H + tabGap + searchH + searchGridGap + hintGap + hintH + barGap + barH + bottomPad;
        int gridH = Math.max(80, this.height - 24 - chromeH);
        panelH = chromeH + gridH;

        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int pad = 10;
        int innerX = panelX + pad;
        int innerW = panelW - pad * 2;
        int y = panelY;

        titleRect = new Rect(panelX, y, panelW, titleH);
        closeRect = new Rect(panelX + panelW - 20, y + 6, 14, 14);
        y += titleH;

        tabBarRect = new Rect(innerX, y, innerW, TAB_H);
        y += TAB_H + tabGap;

        int searchY = y;
        searchBox = new EditBox(this.font, innerX + 4, searchY + 4, innerW - 26, 12,
                Component.literal("Search"));
        searchBox.setMaxLength(100);
        searchBox.setBordered(false);
        searchBox.setTextColor(COL_TEXT);
        searchBox.setHint(Component.translatable("macrogrid.search.hint").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setResponder(s -> { scroll = 0; rebuildFiltered(); });
        addWidget(searchBox);

        clearRect = new Rect(innerX + innerW - 16, searchY, 16, searchH);
        y += searchH + searchGridGap;

        gridRect = new Rect(innerX, y, innerW, gridH);
        cardW = (gridRect.w() - (COLS - 1) * CARD_GAP) / COLS;
        y += gridH + hintGap;

        hintRect = new Rect(innerX, y, innerW, hintH);
        y += hintH + barGap;

        int barY = y;
        int naturalSum = addW0 + editW0 + moveW0 + delW0;
        int availableForPills = innerW - tGap * 3;
        double scale = availableForPills / (double) naturalSum;
        int addW = (int) (addW0 * scale);
        int editW = (int) (editW0 * scale);
        int moveW = (int) (moveW0 * scale);
        int delW = innerW - addW - editW - moveW - tGap * 3;

        addRect = new Rect(innerX, barY, addW, barH);
        int ex = innerX + addW + tGap;
        editRect = new Rect(ex, barY, editW, barH);
        ex += editW + tGap;
        moveRect = new Rect(ex, barY, moveW, barH);
        ex += moveW + tGap;
        deleteRect = new Rect(ex, barY, delW, barH);

        rebuildFiltered();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }


    private void rebuildFiltered() {
        String search = searchBox != null ? searchBox.getValue().toLowerCase() : "";
        List<ButtonData> all = ButtonManager.getButtons();
        filtered = search.isEmpty()
                ? all
                : all.stream().filter(b -> b.getName().toLowerCase().contains(search)).toList();

        int rows = (int) Math.ceil(filtered.size() / (double) COLS);
        int contentH = rows == 0 ? 0 : rows * CARD_H + (rows - 1) * CARD_GAP;
        maxScroll = Math.max(0, contentH - gridRect.h());
        scroll = Math.min(scroll, maxScroll);
    }

    private Rect cardRect(int index) {
        int col = index % COLS;
        int row = index / COLS;
        int cx = gridRect.x() + col * (cardW + CARD_GAP);
        int cy = gridRect.y() + row * (CARD_H + CARD_GAP) - scroll;
        return new Rect(cx, cy, cardW, CARD_H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {

        super.extractRenderState(g, mouseX, mouseY, delta);

        g.fill(0, 0, this.width, this.height, COL_DIM);

        drawPanel(g);
        drawTitle(g, mouseX, mouseY);
        drawTabs(g, mouseX, mouseY, delta);
        drawSearch(g, mouseX, mouseY, delta);
        drawGrid(g, mouseX, mouseY);
        drawHint(g);
        drawToolbar(g, mouseX, mouseY);

        if (gridRect.contains(mouseX, mouseY)) {
            for (int i = 0; i < filtered.size(); i++) {
                Rect r = cardRect(i);
                if (r.y() + r.h() <= gridRect.y() || r.y() >= gridRect.y() + gridRect.h()) continue;
                if (r.contains(mouseX, mouseY)) {
                    drawTooltip(g, makeTooltip(filtered.get(i)), mouseX, mouseY);
                    break;
                }
            }
        }
    }

    private void drawTooltip(GuiGraphicsExtractor g, List<Component> lines, int mouseX, int mouseY) {
        if (lines.isEmpty()) return;
        int lineH = 10;
        int pad = 4;
        int w = 0;
        for (Component line : lines) w = Math.max(w, this.font.width(line));
        w += pad * 2;
        int h = lines.size() * lineH + pad * 2 - 2;

        int tx = mouseX + 12;
        int ty = mouseY - 6;
        if (tx + w > this.width) tx = mouseX - w - 8;
        if (ty + h > this.height) ty = this.height - h - 4;
        if (ty < 0) ty = 4;

        g.fill(tx - 1, ty - 1, tx + w + 1, ty + h + 1, COL_PANEL_BORDER);
        g.fill(tx, ty, tx + w, ty + h, 0xF0101218);
        for (int i = 0; i < lines.size(); i++) {
            g.text(this.font, lines.get(i), tx + pad, ty + pad + i * lineH, COL_TEXT, false);
        }
    }

    private void centeredText(GuiGraphicsExtractor g, Object text, int centerX, int y, int color) {
        int w = (text instanceof Component c) ? this.font.width(c) : this.font.width(text.toString());
        if (text instanceof Component c) g.text(this.font, c, centerX - w / 2, y, color, false);
        else g.text(this.font, text.toString(), centerX - w / 2, y, color, false);
    }

    private void drawPanel(GuiGraphicsExtractor g) {
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, COL_PANEL_BORDER);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, COL_PANEL);
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, COL_ACCENT);
        g.fillGradient(panelX, panelY + 2, panelX + panelW, panelY + 14, COL_PANEL_TOP, 0);
    }

    private void drawTitle(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Component titleComp = Component.translatable("macrogrid.title").withStyle(ChatFormatting.BOLD);
        g.text(this.font, titleComp, panelX + 10, titleRect.y() + 9, COL_TEXT, false);

        Component count = Component.translatable("macrogrid.count", filtered.size(), ButtonManager.getButtons().size());
        g.text(this.font, count,
                panelX + 10 + this.font.width(titleComp), titleRect.y() + 9, COL_TEXT_DIM, false);

        boolean hover = closeRect.contains(mouseX, mouseY);
        g.fill(closeRect.x(), closeRect.y(), closeRect.x() + closeRect.w(), closeRect.y() + closeRect.h(),
                hover ? COL_DANGER_DIM : 0x00000000);
        centeredText(g, "\u2715", closeRect.x() + closeRect.w() / 2,
                closeRect.y() + 3, hover ? COL_DANGER : COL_TEXT_DIM);
    }


    private void drawTabs(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        tabNames = ButtonManager.getProfileNames();
        String current = ButtonManager.getCurrentProfile();

        tabRects.clear();
        int pillPad = 8;
        int iconW = 14;
        int gap = 3;

        int x = tabBarRect.x() - tabScroll;
        int y = tabBarRect.y();

        g.enableScissor(tabBarRect.x(), tabBarRect.y(), tabBarRect.x() + tabBarRect.w(), tabBarRect.y() + tabBarRect.h());

        for (String name : tabNames) {
            boolean active = name.equals(current);
            int w = this.font.width(name) + pillPad * 2;
            Rect r = new Rect(x, y, w, TAB_H);
            tabRects.add(r);

            if (r.x() + r.w() >= tabBarRect.x() && r.x() <= tabBarRect.x() + tabBarRect.w()) {
                boolean hover = r.contains(mouseX, mouseY) && tabBarRect.contains(mouseX, mouseY);
                g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), active ? COL_ACCENT_DIM : (hover ? 0x22FFFFFF : 0x14FFFFFF));
                if (active) g.fill(r.x(), r.y() + r.h() - 2, r.x() + r.w(), r.y() + r.h(), COL_ACCENT);
                if (active && renamingProfile && renameBox != null) {
                    renameBox.extractRenderState(g, mouseX, mouseY, delta);
                } else {
                    centeredText(g, name, r.x() + r.w() / 2, r.y() + (TAB_H - 8) / 2, active ? COL_TEXT : COL_TEXT_DIM);
                }
            }
            x += w + gap;

            if (active && !renamingProfile) {
                tabRenameRect = new Rect(x, y, iconW, TAB_H);
                boolean rHover = tabRenameRect.contains(mouseX, mouseY) && tabBarRect.contains(mouseX, mouseY);
                centeredText(g, "\u270E", tabRenameRect.x() + iconW / 2, tabRenameRect.y() + (TAB_H - 8) / 2,
                        rHover ? COL_TEXT : COL_TEXT_DIM);
                x += iconW + gap;

                if (tabNames.size() > 1) {
                    tabDeleteRect = new Rect(x, y, iconW, TAB_H);
                    boolean dHover = tabDeleteRect.contains(mouseX, mouseY) && tabBarRect.contains(mouseX, mouseY);
                    centeredText(g, "\u2715", tabDeleteRect.x() + iconW / 2, tabDeleteRect.y() + (TAB_H - 8) / 2,
                            pendingDeleteProfile ? COL_DANGER : (dHover ? COL_TEXT : COL_TEXT_DIM));
                    x += iconW + gap;
                } else {
                    tabDeleteRect = null;
                }
            } else if (active) {
                tabRenameRect = null;
                tabDeleteRect = null;
            }
        }

        tabAddRect = new Rect(x, y, iconW, TAB_H);
        boolean addHover = tabAddRect.contains(mouseX, mouseY) && tabBarRect.contains(mouseX, mouseY);
        centeredText(g, "+", tabAddRect.x() + iconW / 2, tabAddRect.y() + (TAB_H - 8) / 2,
                addHover ? COL_ACCENT : COL_TEXT_DIM);
        x += iconW;

        tabContentW = x - (tabBarRect.x() - tabScroll);

        g.disableScissor();
    }

    private void reopenOnProfile() {
        Minecraft.getInstance().gui.setScreen(new ButtonGuiScreen());
    }

    private void enterRenameMode() {
        commitOrCancelRename(false);
        int idx = tabNames.indexOf(ButtonManager.getCurrentProfile());
        if (idx < 0 || idx >= tabRects.size()) return;
        Rect r = tabRects.get(idx);
        renameBox = new EditBox(this.font, r.x() + 4, r.y() + 5, Math.max(30, r.w() - 8), 10, Component.literal("profile"));
        renameBox.setMaxLength(24);
        renameBox.setBordered(false);
        renameBox.setTextColor(COL_TEXT);
        renameBox.setValue(ButtonManager.getCurrentProfile());
        addWidget(renameBox);
        setFocused(renameBox);
        renamingProfile = true;
    }


    private void commitOrCancelRename(boolean commit) {
        if (!renamingProfile || renameBox == null) return;
        if (commit) {
            String newName = renameBox.getValue();
            ButtonManager.renameProfile(ButtonManager.getCurrentProfile(), newName);
        }
        removeWidget(renameBox);
        renameBox = null;
        renamingProfile = false;
    }


    private void drawSearch(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int sx = gridRect.x(), sy = tabBarRect.y() + tabBarRect.h() + 4;
        int sw = gridRect.w();
        g.fill(sx, sy, sx + sw, sy + searchH, 0x22000000);
        g.fill(sx, sy, sx + sw, sy + 1, COL_PANEL_BORDER);
        g.enableScissor(sx, sy, sx + sw, sy + searchH);
        searchBox.extractRenderState(g, mouseX, mouseY, delta);
        g.disableScissor();

        boolean hover = clearRect.contains(mouseX, mouseY);
        centeredText(g, "\u2715", clearRect.x() + clearRect.w() / 2,
                clearRect.y() + (searchH - 8) / 2, hover ? COL_TEXT : COL_TEXT_DIM);
    }

    private void drawGrid(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.enableScissor(gridRect.x(), gridRect.y(), gridRect.x() + gridRect.w(), gridRect.y() + gridRect.h());

        if (filtered.isEmpty()) {
            Component msg = searchBox.getValue().isEmpty()
                    ? Component.translatable("macrogrid.search.empty")
                    : Component.translatable("macrogrid.search.noresults", searchBox.getValue());
            centeredText(g, msg.copy().withStyle(ChatFormatting.DARK_GRAY),
                    gridRect.x() + gridRect.w() / 2, gridRect.y() + gridRect.h() / 2 - 4, COL_TEXT_DIM);
        } else {
            for (int i = 0; i < filtered.size(); i++) {
                ButtonData btn = filtered.get(i);
                Rect r = cardRect(i);
                if (r.y() + r.h() < gridRect.y() || r.y() > gridRect.y() + gridRect.h()) continue;
                drawCard(g, btn, r, r.contains(mouseX, mouseY) && gridRect.contains(mouseX, mouseY));
            }
        }

        g.disableScissor();

        if (maxScroll > 0) {
            int trackW = 5;
            int trackX = gridRect.x() + gridRect.w() + 3;
            g.fill(trackX, gridRect.y(), trackX + trackW, gridRect.y() + gridRect.h(), 0x3DFFFFFF);
            int thumbH = Math.max(12, gridRect.h() * gridRect.h() / (gridRect.h() + maxScroll));
            int thumbY = gridRect.y() + (int) ((gridRect.h() - thumbH) * (scroll / (float) maxScroll));
            g.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, COL_ACCENT);
        }
    }

    private void drawCard(GuiGraphicsExtractor g, ButtonData btn, Rect r, boolean hover) {
        int bg = hover ? COL_CARD_HOVER : COL_CARD;
        int accent = COL_ACCENT;
        int accentDim = COL_ACCENT_DIM;

        switch (mode) {
            case DELETE -> { accent = COL_DANGER; accentDim = COL_DANGER_DIM; if (btn == pendingDelete) bg = COL_DANGER_DIM; }
            case EDIT -> { accent = COL_WARN; accentDim = COL_WARN_DIM; }
            case MOVE -> {
                accent = COL_MOVE; accentDim = COL_MOVE_DIM;
                if (btn == moveSource) bg = COL_MOVE_DIM;
            }
            case NONE -> { }
        }

        g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), bg);
        g.fill(r.x(), r.y(), r.x() + 2, r.y() + r.h(), mode == Mode.NONE ? accentDim : accent);
        if (hover) {
            g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + 1, COL_CARD_BORDER);
            g.fill(r.x(), r.y() + r.h() - 1, r.x() + r.w(), r.y() + r.h(), COL_CARD_BORDER);
        }

        String label = makeLabel(btn);
        int maxTextW = r.w() - 8;
        String trimmed = this.font.plainSubstrByWidth(label, maxTextW);
        if (!trimmed.equals(label)) {
            String ellipsis = "...";
            int ellW = this.font.width(ellipsis);
            String core = this.font.plainSubstrByWidth(label, Math.max(0, maxTextW - ellW));
            trimmed = core + ellipsis;
        }
        int textColor = mode == Mode.DELETE && btn == pendingDelete ? COL_DANGER : COL_TEXT;
        g.text(this.font, trimmed, r.x() + 6, r.y() + r.h() / 2 - 4, textColor, false);
    }

    private void drawHint(GuiGraphicsExtractor g) {
        Component text = hintText();
        if (text.getString().isEmpty()) return;
        int color = switch (mode) {
            case DELETE -> pendingDelete != null ? COL_DANGER : COL_TEXT_DIM;
            case MOVE -> COL_MOVE;
            case EDIT -> COL_WARN;
            case NONE -> COL_TEXT_DIM;
        };
        centeredText(g, text, hintRect.x() + hintRect.w() / 2, hintRect.y() + 2, color);
    }

    private Component hintText() {
        return switch (mode) {
            case EDIT -> Component.translatable("macrogrid.hint.edit");
            case MOVE -> moveSource == null
                    ? Component.translatable("macrogrid.hint.move.pick")
                    : Component.translatable("macrogrid.hint.move.swap");
            case DELETE -> pendingDelete == null
                    ? Component.translatable("macrogrid.hint.delete.pick")
                    : Component.translatable("macrogrid.hint.delete.confirm");
            case NONE -> Component.empty();
        };
    }

    private void drawToolbar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        drawPill(g, addRect, Component.translatable("macrogrid.toolbar.add"), COL_ACCENT, COL_ACCENT_DIM, false, addRect.contains(mouseX, mouseY));
        drawPill(g, editRect, Component.translatable("macrogrid.toolbar.edit"), COL_WARN, COL_WARN_DIM, mode == Mode.EDIT, editRect.contains(mouseX, mouseY));
        drawPill(g, moveRect, Component.translatable("macrogrid.toolbar.move"), COL_MOVE, COL_MOVE_DIM, mode == Mode.MOVE, moveRect.contains(mouseX, mouseY));
        drawPill(g, deleteRect, Component.translatable("macrogrid.toolbar.delete"), COL_DANGER, COL_DANGER_DIM, mode == Mode.DELETE, deleteRect.contains(mouseX, mouseY));
    }

    private void drawPill(GuiGraphicsExtractor g, Rect r, Object label, int accent, int accentDim, boolean active, boolean hover) {
        int bg = active ? accentDim : (hover ? 0x28FFFFFF : 0x18FFFFFF);
        g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), bg);
        if (active) {
            g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + 1, accent);
        }
        int textColor = active ? accent : COL_TEXT;
        centeredText(g, label, r.x() + r.w() / 2, r.y() + (r.h() - 8) / 2, textColor);
    }

    private String makeLabel(ButtonData btn) {
        StringBuilder sb = new StringBuilder();
        if (mode == Mode.NONE) {
            if (btn.getIcon() != null && btn.getIcon() != ButtonData.ButtonIcon.NONE) {
                sb.append(btn.getIcon().getSymbol()).append(" ");
            }
            if (btn.getColor() != null && btn.getColor() != ButtonData.ButtonColor.DEFAULT) {
                sb.append(btn.getColor().getCode());
            }
        }
        sb.append(btn.getName());
        return sb.toString();
    }

    private List<Component> makeTooltip(ButtonData btn) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(btn.getName()).withStyle(ChatFormatting.YELLOW));
        switch (mode) {
            case DELETE -> lines.add((btn == pendingDelete
                    ? Component.translatable("macrogrid.tooltip.delete.confirm")
                    : Component.translatable("macrogrid.tooltip.delete")
            ).copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            case EDIT -> lines.add(Component.translatable("macrogrid.tooltip.edit").withStyle(ChatFormatting.YELLOW));
            case MOVE -> lines.add((moveSource == null
                    ? Component.translatable("macrogrid.tooltip.move.pick")
                    : Component.translatable("macrogrid.tooltip.move.swap")
            ).copy().withStyle(ChatFormatting.GOLD));
            case NONE -> {
                int n = 0;
                for (ButtonData.CommandEntry cmd : btn.getCommands()) {
                    if (n++ >= 4) {
                        lines.add(Component.literal("...").withStyle(ChatFormatting.DARK_GRAY));
                        break;
                    }
                    lines.add(Component.literal(cmd.getCommand()).withStyle(ChatFormatting.GRAY));
                }
            }
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button == 0) {
            if (renamingProfile) {
                int idx = tabNames.indexOf(ButtonManager.getCurrentProfile());
                Rect activeTabRect = (idx >= 0 && idx < tabRects.size()) ? tabRects.get(idx) : null;
                if (activeTabRect == null || !activeTabRect.contains(mouseX, mouseY)) {
                    commitOrCancelRename(true);
                }
            }

            if (tabBarRect.contains(mouseX, mouseY)) {
                if (tabAddRect != null && tabAddRect.contains(mouseX, mouseY)) {
                    ButtonManager.createProfile(null);
                    reopenOnProfile();
                    return true;
                }
                if (tabRenameRect != null && tabRenameRect.contains(mouseX, mouseY)) {
                    enterRenameMode();
                    return true;
                }
                if (tabDeleteRect != null && tabDeleteRect.contains(mouseX, mouseY)) {
                    if (pendingDeleteProfile) {
                        ButtonManager.deleteProfile(ButtonManager.getCurrentProfile());
                        pendingDeleteProfile = false;
                        reopenOnProfile();
                    } else {
                        pendingDeleteProfile = true;
                    }
                    return true;
                }
                for (int i = 0; i < tabRects.size(); i++) {
                    if (tabRects.get(i).contains(mouseX, mouseY)) {
                        String name = tabNames.get(i);
                        if (!name.equals(ButtonManager.getCurrentProfile())) {
                            ButtonManager.setCurrentProfile(name);
                            reopenOnProfile();
                        }
                        return true;
                    }
                }
            }
            pendingDeleteProfile = false;

            if (closeRect.contains(mouseX, mouseY)) { onClose(); return true; }
            if (clearRect.contains(mouseX, mouseY)) { searchBox.setValue(""); return true; }
            if (addRect.contains(mouseX, mouseY)) { openAddScreen(); return true; }
            if (editRect.contains(mouseX, mouseY)) { setMode(Mode.EDIT); return true; }
            if (moveRect.contains(mouseX, mouseY)) { setMode(Mode.MOVE); return true; }
            if (deleteRect.contains(mouseX, mouseY)) { setMode(Mode.DELETE); return true; }

            if (gridRect.contains(mouseX, mouseY)) {
                for (int i = 0; i < filtered.size(); i++) {
                    Rect r = cardRect(i);
                    if (r.y() + r.h() < gridRect.y() || r.y() > gridRect.y() + gridRect.h()) continue;
                    if (r.contains(mouseX, mouseY)) {
                        handleClick(filtered.get(i));
                        return true;
                    }
                }
                // Click on empty grid space: start click-and-drag scrolling.
                dragging = true;
                dragLastY = mouseY;
                return true;
            }
        }

        if (renamingProfile && renameBox != null && renameBox.mouseClicked(click, doubled)) {
            setFocused(renameBox);
            return true;
        }
        if (searchBox.mouseClicked(click, doubled)) {
            setFocused(searchBox);
            return true;
        }
        setFocused(null);
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tabBarRect.contains(mouseX, mouseY)) {
            int tabMaxScroll = Math.max(0, tabContentW - tabBarRect.w());
            if (tabMaxScroll > 0) {
                tabScroll = Math.max(0, Math.min(tabMaxScroll, tabScroll - (int) (scrollY * 20)));
                return true;
            }
        }
        if (gridRect.contains(mouseX, mouseY) && maxScroll > 0) {
            scroll -= (int) (scrollY * 16);
            scroll = Math.max(0, Math.min(maxScroll, scroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging && maxScroll > 0) {
            scroll -= (int) (event.y() - dragLastY);
            scroll = Math.max(0, Math.min(maxScroll, scroll));
            dragLastY = event.y();
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (renamingProfile && renameBox != null && renameBox.isFocused()) {
            if (renameBox.keyPressed(input)) return true;
        }
        if (searchBox.isFocused() && searchBox.keyPressed(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (renamingProfile && renameBox != null && renameBox.isFocused()) {
            if (renameBox.charTyped(input)) return true;
        }
        if (searchBox.isFocused() && searchBox.charTyped(input)) return true;
        return super.charTyped(input);
    }

    private void setMode(Mode newMode) {
        mode = (mode == newMode) ? Mode.NONE : newMode;
        if (mode != Mode.MOVE) moveSource = null;
        pendingDelete = null;
    }

    private void handleClick(ButtonData button) {
        switch (mode) {
            case NONE -> execute(button);
            case EDIT -> { mode = Mode.NONE; openEditScreen(button); }
            case DELETE -> {
                if (pendingDelete == button) { deleteButton(button); pendingDelete = null; }
                else pendingDelete = button;
            }
            case MOVE -> {
                if (moveSource == null) moveSource = button;
                else if (moveSource == button) moveSource = null;
                else {
                    swapButtons(moveSource, button);
                    moveSource = null;
                    mode = Mode.NONE;
                }
            }
        }
    }

    private void swapButtons(ButtonData a, ButtonData b) {
        List<ButtonData> list = ButtonManager.getButtons();
        int i = list.indexOf(a), j = list.indexOf(b);
        if (i != -1 && j != -1) {
            ButtonData tmp = list.get(i);
            ButtonManager.updateButton(i, list.get(j));
            ButtonManager.updateButton(j, tmp);
            rebuildFiltered();
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
        Minecraft.getInstance().gui.setScreen(new EditButtonScreen(null, btn -> {
            ButtonManager.addButton(btn);
            Minecraft.getInstance().gui.setScreen(new ButtonGuiScreen());
        }));
    }

    private void openEditScreen(ButtonData button) {
        int index = ButtonManager.getButtons().indexOf(button);
        Minecraft.getInstance().gui.setScreen(new EditButtonScreen(button, updated -> {
            ButtonManager.updateButton(index, updated);
            Minecraft.getInstance().gui.setScreen(new ButtonGuiScreen());
        }));
    }

    private void deleteButton(ButtonData button) {
        List<ButtonData> list = ButtonManager.getButtons();
        int i = list.indexOf(button);
        if (i != -1) ButtonManager.removeButton(i);
        rebuildFiltered();
    }
}