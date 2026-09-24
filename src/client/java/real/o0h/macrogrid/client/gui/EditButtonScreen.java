package real.o0h.macrogrid.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import real.o0h.macrogrid.client.config.ButtonData;

public class EditButtonScreen extends Screen {

    private static final int COL_DIM = 0x99000000;
    private static final int COL_PANEL = 0xF0181A22;
    private static final int COL_PANEL_TOP = 0x1AFFFFFF;
    private static final int COL_PANEL_BORDER = 0x33FFFFFF;
    private static final int COL_ACCENT = 0xFF5B8CFF;
    private static final int COL_ACCENT_DIM = 0x405B8CFF;
    private static final int COL_TEXT = 0xFFEDEDF2;
    private static final int COL_TEXT_DIM = 0xFF8B93A6;
    private static final int COL_CMD = 0xFF5B8CFF;
    private static final int COL_CMD_DIM = 0x2A5B8CFF;
    private static final int COL_MSG = 0xFF5FCB7A;
    private static final int COL_MSG_DIM = 0x2A5FCB7A;
    private static final int COL_DANGER = 0xFFE5605B;

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private static final class CmdRow {
        ButtonData.CommandType type;
        String text;
        CmdRow(ButtonData.CommandType type, String text) { this.type = type; this.text = text; }
    }

    private final ButtonData existing;
    private final Consumer<ButtonData> onSave;
    private final boolean isAddMode;

    private ButtonData.ButtonColor currentColor;
    private ButtonData.ButtonIcon currentIcon;

    private EditBox nameBox;
    private final List<CmdRow> rows = new ArrayList<>();
    private int scroll = 0;
    private int maxScroll = 0;


    private EditBox activeField;
    private int activeRow = -1;

    // Click-and-drag scrolling state for the command list.
    private boolean dragging = false;
    private double dragLastY = 0;

    private int panelX, panelY, panelW, panelH;
    private Rect nameFieldRect, colorRect, iconRect, previewRect;
    private Rect cmdListRect, addCmdRect, addMsgRect, saveRect, cancelRect;
    private static final int ROW_H = 20;
    private static final int VISIBLE_ROWS = 3;

    public EditButtonScreen(ButtonData existing, Consumer<ButtonData> onSave) {
        super(existing == null ? Component.translatable("macrogrid.edit.title.add") : Component.translatable("macrogrid.edit.title.edit"));
        this.existing = existing;
        this.onSave = onSave;
        this.isAddMode = existing == null;
        if (existing != null) {
            this.currentColor = existing.getColor();
            this.currentIcon = existing.getIcon();
        } else {
            this.currentColor = ButtonData.ButtonColor.DEFAULT;
            this.currentIcon = ButtonData.ButtonIcon.NONE;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        ButtonGuiScreen.setOpen(false);
        super.removed();
    }


    @Override
    protected void init() {
        ButtonGuiScreen.setOpen(true);
        panelW = 240;
        panelX = (this.width - panelW) / 2;

        int pad = 12;
        int innerX = panelX + pad;
        int innerW = panelW - pad * 2;

        int y = 0;
        int titleH = 24;
        int nameH = 22;
        int gap = 6;
        int appearH = 20;
        int previewH = 16;
        int cmdLabelH = 12;
        int addRowH = 20;
        int saveRowH = 20;
        int bottomPad = 10;

        // Command list height adapts to the window: the panel fills the
        // window height (minus a small margin) so the list is as large as
        // possible, while Save/Cancel stay pinned at the bottom.
        int chromeH = titleH + nameH + gap + appearH + gap + previewH + gap
                + cmdLabelH + gap + addRowH + gap + saveRowH + bottomPad;
        int cmdListH = Math.max(60, this.height - 24 - chromeH);

        panelH = chromeH + cmdListH;
        panelY = (this.height - panelH) / 2;

        y = panelY + titleH;
        nameFieldRect = new Rect(innerX, y, innerW, nameH);
        nameBox = new EditBox(this.font, innerX + 2, y + 4, innerW - 4, 14, Component.translatable("macrogrid.edit.name.hint"));
        nameBox.setMaxLength(24);
        nameBox.setBordered(false);
        nameBox.setTextColor(COL_TEXT);
        nameBox.setHint(Component.translatable("macrogrid.edit.name.hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(existing != null ? existing.getName() : "");
        addWidget(nameBox);
        y += nameH + gap;

        int halfW = (innerW - 4) / 2;
        colorRect = new Rect(innerX, y, halfW, appearH);
        iconRect = new Rect(innerX + halfW + 4, y, halfW, appearH);
        y += appearH + gap;

        previewRect = new Rect(innerX, y, innerW, previewH);
        y += previewH + gap;

        y += cmdLabelH;
        cmdListRect = new Rect(innerX, y, innerW, cmdListH);
        y += cmdListH + gap;

        int addGap = 4;
        int addW = (innerW - addGap) / 2;
        addCmdRect = new Rect(innerX, y, addW, addRowH);
        addMsgRect = new Rect(innerX + addW + addGap, y, addW, addRowH);
        y += addRowH + gap;

        int saveGap = 6;
        int btnW = (innerW - saveGap) / 2;
        saveRect = new Rect(innerX, y, btnW, saveRowH);
        cancelRect = new Rect(innerX + btnW + saveGap, y, btnW, saveRowH);

        rows.clear();
        if (existing != null) {
            for (ButtonData.CommandEntry cmd : existing.getCommands()) {
                addRow(cmd.getType(), cmd.getCommand());
            }
        }
        recalcScroll();
    }

    private void addRow(ButtonData.CommandType type, String text) {
        rows.add(new CmdRow(type, text));
        recalcScroll();
    }

    private void removeRow(int index) {
        if (activeRow == index) commitActiveField();
        else if (activeRow > index) activeRow--;
        rows.remove(index);
        recalcScroll();
    }


    private void focusRow(int index) {
        if (activeRow == index) return;
        commitActiveField();
        RowRects rr = rowRects(index);
        activeField = new EditBox(this.font, rr.field().x(), rr.field().y() + 4, rr.field().w(), 14,
                Component.literal("cmd"));
        activeField.setMaxLength(256);
        activeField.setBordered(false);
        activeField.setTextColor(COL_TEXT);
        activeField.setValue(rows.get(index).text);
        addWidget(activeField);
        setInitialFocus(activeField);
        activeRow = index;
    }

    private void commitActiveField() {
        if (activeField != null) {
            if (activeRow >= 0 && activeRow < rows.size()) rows.get(activeRow).text = activeField.getValue();
            removeWidget(activeField);
        }
        activeField = null;
        activeRow = -1;
    }

    private void positionActiveField(RowRects rr) {
        if (activeField == null) return;
        activeField.setX(rr.field().x());
        activeField.setY(rr.field().y() + 4);
        activeField.setWidth(rr.field().w());
    }

    private void recalcScroll() {
        int contentH = rows.size() * ROW_H;
        maxScroll = Math.max(0, contentH - cmdListRect.h());
        scroll = Math.min(scroll, maxScroll);
    }


    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        g.fill(0, 0, this.width, this.height, COL_DIM);

        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, COL_PANEL_BORDER);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, COL_PANEL);
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, COL_ACCENT);
        g.fillGradient(panelX, panelY + 2, panelX + panelW, panelY + 14, COL_PANEL_TOP, 0);

        centeredText(g, this.title, panelX + panelW / 2, panelY + 8, COL_TEXT);


        g.fill(nameFieldRect.x(), nameFieldRect.y(), nameFieldRect.x() + nameFieldRect.w(),
                nameFieldRect.y() + nameFieldRect.h(), 0x22000000);
        g.fill(nameFieldRect.x(), nameFieldRect.y(), nameFieldRect.x() + nameFieldRect.w(),
                nameFieldRect.y() + 1, COL_PANEL_BORDER);
        g.enableScissor(nameFieldRect.x() + 2, nameFieldRect.y(),
                nameFieldRect.x() + nameFieldRect.w() - 2, nameFieldRect.y() + nameFieldRect.h());
        nameBox.extractRenderState(g, mouseX, mouseY, delta);
        g.disableScissor();

        drawPill(g, colorRect, colorLabel(), COL_TEXT, colorRect.contains(mouseX, mouseY));
        drawPill(g, iconRect, iconLabel(), COL_TEXT, iconRect.contains(mouseX, mouseY));

        g.fill(previewRect.x(), previewRect.y(), previewRect.x() + previewRect.w(),
                previewRect.y() + previewRect.h(), 0x22000000);
        Component previewLabel = Component.translatable("macrogrid.edit.preview");
        g.text(this.font, previewLabel, previewRect.x() + 4, previewRect.y() + 4, COL_TEXT, false);
        g.text(this.font, previewText(), previewRect.x() + 6 + this.font.width(previewLabel), previewRect.y() + 4,
                COL_TEXT, false);

        g.text(this.font, Component.translatable("macrogrid.edit.commands"), cmdListRect.x(), cmdListRect.y() - 11, COL_TEXT_DIM, false);
        drawCommandList(g, mouseX, mouseY);

        drawPill(g, addCmdRect, Component.translatable("macrogrid.edit.addcmd"), COL_CMD, addCmdRect.contains(mouseX, mouseY));
        drawPill(g, addMsgRect, Component.translatable("macrogrid.edit.addmsg"), COL_MSG, addMsgRect.contains(mouseX, mouseY));

        drawFilledPill(g, saveRect, Component.translatable("macrogrid.edit.save"), COL_ACCENT, COL_ACCENT_DIM, saveRect.contains(mouseX, mouseY));
        drawPill(g, cancelRect, Component.translatable("macrogrid.edit.cancel"), COL_TEXT_DIM, cancelRect.contains(mouseX, mouseY));
    }

    private void drawCommandList(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.enableScissor(cmdListRect.x(), cmdListRect.y(),
                cmdListRect.x() + cmdListRect.w(), cmdListRect.y() + cmdListRect.h());

        if (rows.isEmpty()) {
            g.text(this.font, Component.translatable("macrogrid.edit.commands.empty")
                    .withStyle(ChatFormatting.DARK_GRAY), cmdListRect.x(), cmdListRect.y() + 4, COL_TEXT_DIM, false);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                int rowY = cmdListRect.y() + i * ROW_H - scroll;
                if (rowY + ROW_H < cmdListRect.y() || rowY > cmdListRect.y() + cmdListRect.h()) continue;
                RowRects rr = rowRects(i);
                boolean isCmd = rows.get(i).type == ButtonData.CommandType.COMMAND;

                g.fill(rr.bg.x(), rr.bg.y(), rr.bg.x() + rr.bg.w(), rr.bg.y() + rr.bg.h(),
                        isCmd ? COL_CMD_DIM : COL_MSG_DIM);

                drawMiniPill(g, rr.type, isCmd ? Component.translatable("macrogrid.edit.cmdbadge") : Component.translatable("macrogrid.edit.msgbadge"), isCmd ? COL_CMD : COL_MSG);

                g.enableScissor(rr.field.x(), rr.field.y(), rr.field.x() + rr.field.w(), rr.field.y() + rr.field.h() + 4);
                if (i == activeRow && activeField != null) {
                    positionActiveField(rr);
                    activeField.extractRenderState(g, mouseX, mouseY, 0);
                } else {
                    String text = rows.get(i).text;
                    String trimmed = this.font.plainSubstrByWidth(text, Math.max(0, rr.field.w() - 2));
                    g.text(this.font, trimmed, rr.field.x() + 1, rr.field.y() + 5, COL_TEXT, false);
                }
                g.disableScissor();

                drawMiniPill(g, rr.up, "\u2191", COL_TEXT_DIM);
                drawMiniPill(g, rr.down, "\u2193", COL_TEXT_DIM);
                drawMiniPill(g, rr.del, "\u2715", COL_DANGER);
            }
        }
        g.disableScissor();

        if (maxScroll > 0) {
            int trackW = 5;
            int trackX = cmdListRect.x() + cmdListRect.w() + 3;
            g.fill(trackX, cmdListRect.y(), trackX + trackW, cmdListRect.y() + cmdListRect.h(), 0x3DFFFFFF);
            int thumbH = Math.max(12, cmdListRect.h() * cmdListRect.h() / (cmdListRect.h() + maxScroll));
            int thumbY = cmdListRect.y() + (int) ((cmdListRect.h() - thumbH) * (scroll / (float) maxScroll));
            g.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, COL_ACCENT);
        }
    }

    private record RowRects(Rect bg, Rect type, Rect field, Rect up, Rect down, Rect del) {}

    private RowRects rowRects(int index) {
        int rowY = cmdListRect.y() + index * ROW_H - scroll;
        int typeW = 34, miniW = 14, gap = 2;
        int x = cmdListRect.x();
        Rect bg = new Rect(cmdListRect.x(), rowY, cmdListRect.w(), ROW_H - 2);
        Rect type = new Rect(x, rowY, typeW, ROW_H - 2);
        x += typeW + gap;
        int fieldW = cmdListRect.w() - typeW - miniW * 3 - gap * 4;
        Rect field = new Rect(x, rowY, fieldW, ROW_H - 2);
        x += fieldW + gap;
        Rect up = new Rect(x, rowY, miniW, ROW_H - 2);
        x += miniW + gap;
        Rect down = new Rect(x, rowY, miniW, ROW_H - 2);
        x += miniW + gap;
        Rect del = new Rect(x, rowY, miniW, ROW_H - 2);
        return new RowRects(bg, type, field, up, down, del);
    }

    private void drawPill(GuiGraphicsExtractor g, Rect r, Object label, int textCol, boolean hover) {
        g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), hover ? 0x28FFFFFF : 0x18FFFFFF);
        centeredText(g, label, r.x() + r.w() / 2, r.y() + 6, textCol);
    }

    private void drawFilledPill(GuiGraphicsExtractor g, Rect r, Object label, int accent, int accentDim, boolean hover) {
        g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), hover ? brighten(accentDim) : accentDim);
        g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + 1, accent);
        centeredText(g, label, r.x() + r.w() / 2, r.y() + 6, accent);
    }

    private int brighten(int argb) {
        int a = (argb >>> 24) & 0xFF;
        return Math.min(0xFF, a + 0x18) << 24 | (argb & 0xFFFFFF);
    }

    private void drawMiniPill(GuiGraphicsExtractor g, Rect r, Object label, int color) {
        centeredText(g, label, r.x() + r.w() / 2, r.y() + (r.h() - 8) / 2, color);
    }

    private void centeredText(GuiGraphicsExtractor g, Object text, int centerX, int y, int color) {
        if (text instanceof Component c) {
            g.text(this.font, c, centerX - this.font.width(c) / 2, y, color, false);
        } else {
            String s = text.toString();
            g.text(this.font, s, centerX - this.font.width(s) / 2, y, color, false);
        }
    }

    private Component colorLabel() {
        if (currentColor == ButtonData.ButtonColor.DEFAULT) return Component.translatable("macrogrid.edit.color.default");
        Component value = Component.literal(currentColor.getCode() + currentColor.name());
        return Component.translatable("macrogrid.edit.color", value);
    }

    private Component iconLabel() {
        if (currentIcon == null || currentIcon == ButtonData.ButtonIcon.NONE) return Component.translatable("macrogrid.edit.icon.none");
        return Component.translatable("macrogrid.edit.icon", currentIcon.getSymbol(), currentIcon.getName());
    }

    private String previewText() {
        StringBuilder sb = new StringBuilder();
        if (currentIcon != null && currentIcon != ButtonData.ButtonIcon.NONE) sb.append(currentIcon.getSymbol()).append(" ");
        if (currentColor != null && currentColor != ButtonData.ButtonColor.DEFAULT) sb.append(currentColor.getCode());
        String name = nameBox.getValue();
        sb.append(name.isBlank() ? net.minecraft.locale.Language.getInstance().getOrDefault("macrogrid.edit.preview.noname") : name);
        return sb.toString();
    }

    private void cycleColor() {
        ButtonData.ButtonColor[] colors = ButtonData.ButtonColor.values();
        int cur = Arrays.asList(colors).indexOf(currentColor);
        currentColor = colors[(cur + 1) % colors.length];
    }

    private void openIconPicker() {
        ButtonData snapshot = buildButtonData();
        Minecraft.getInstance().gui.setScreen(new IconSelectionScreen(
                icon -> {
                    snapshot.setIcon(icon);
                    Minecraft.getInstance().gui.setScreen(new EditButtonScreen(snapshot, onSave));
                },
                () -> Minecraft.getInstance().gui.setScreen(new EditButtonScreen(snapshot, onSave))
        ));
    }

    private ButtonData buildButtonData() {
        commitActiveField();
        List<ButtonData.CommandEntry> cmds = new ArrayList<>();
        for (CmdRow row : rows) cmds.add(new ButtonData.CommandEntry(row.text, row.type));
        ButtonData data = new ButtonData(nameBox.getValue(), cmds);
        data.setColor(currentColor);
        data.setIcon(currentIcon);
        return data;
    }

    private void save() {
        if (nameBox.getValue().isBlank()) return;
        onSave.accept(buildButtonData());
        onClose();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button == 0) {
            if (activeRow != -1 && !rowRects(activeRow).field().contains(mouseX, mouseY)) {
                commitActiveField();
            }
            if (colorRect.contains(mouseX, mouseY)) { cycleColor(); return true; }
            if (iconRect.contains(mouseX, mouseY)) { openIconPicker(); return true; }
            if (addCmdRect.contains(mouseX, mouseY)) { addRow(ButtonData.CommandType.COMMAND, "/"); return true; }
            if (addMsgRect.contains(mouseX, mouseY)) { addRow(ButtonData.CommandType.MESSAGE, ""); return true; }
            if (saveRect.contains(mouseX, mouseY)) { save(); return true; }
            if (cancelRect.contains(mouseX, mouseY)) { onClose(); return true; }

            if (cmdListRect.contains(mouseX, mouseY)) {
                for (int i = 0; i < rows.size(); i++) {
                    int rowY = cmdListRect.y() + i * ROW_H - scroll;
                    if (rowY + ROW_H < cmdListRect.y() || rowY > cmdListRect.y() + cmdListRect.h()) continue;
                    RowRects rr = rowRects(i);
                    if (rr.field.contains(mouseX, mouseY)) { focusRow(i); return true; }
                    if (rr.type.contains(mouseX, mouseY)) {
                        CmdRow row = rows.get(i);
                        row.type = row.type == ButtonData.CommandType.COMMAND
                                ? ButtonData.CommandType.MESSAGE : ButtonData.CommandType.COMMAND;
                        return true;
                    }
                    if (rr.up.contains(mouseX, mouseY)) {
                        if (i > 0) { CmdRow tmp = rows.get(i); rows.set(i, rows.get(i - 1)); rows.set(i - 1, tmp); }
                        return true;
                    }
                    if (rr.down.contains(mouseX, mouseY)) {
                        if (i < rows.size() - 1) { CmdRow tmp = rows.get(i); rows.set(i, rows.get(i + 1)); rows.set(i + 1, tmp); }
                        return true;
                    }
                    if (rr.del.contains(mouseX, mouseY)) { removeRow(i); return true; }
                }
                // Click on empty command-list space: start drag scrolling.
                dragging = true;
                dragLastY = mouseY;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (cmdListRect.contains(mouseX, mouseY) && maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (scrollY * 16)));
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
}