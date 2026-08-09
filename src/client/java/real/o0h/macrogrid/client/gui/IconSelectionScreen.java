package real.o0h.macrogrid.client.gui;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import real.o0h.macrogrid.client.config.ButtonData;

public class IconSelectionScreen extends Screen {

    private static final int COL_DIM = 0x99000000;
    private static final int COL_PANEL = 0xF0181A22;
    private static final int COL_PANEL_TOP = 0x1AFFFFFF;
    private static final int COL_PANEL_BORDER = 0x33FFFFFF;
    private static final int COL_ACCENT = 0xFF5B8CFF;
    private static final int COL_CARD = 0x1EFFFFFF;
    private static final int COL_CARD_HOVER = 0x38FFFFFF;
    private static final int COL_TEXT = 0xFFEDEDF2;
    private static final int COL_TEXT_DIM = 0xFF8B93A6;

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final Consumer<ButtonData.ButtonIcon> onIconSelected;
    private final Runnable onCancel;

    private static final int COLS = 4;
    private static final int CARD_H = 20;
    private static final int CARD_GAP = 3;
    private static final int VISIBLE_ROWS = 6;

    private int scroll = 0;
    private int maxScroll = 0;

    private int panelX, panelY, panelW, panelH;
    private Rect gridRect, cancelRect;
    private int cardW;

    public IconSelectionScreen(Consumer<ButtonData.ButtonIcon> onIconSelected, Runnable onCancel) {
        super(Component.translatable("macrogrid.icon.title"));
        this.onIconSelected = onIconSelected;
        this.onCancel = onCancel;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        ButtonGuiScreen.setOpen(true);
        panelW = 260;
        int titleH = 24;
        int gridH = VISIBLE_ROWS * CARD_H + (VISIBLE_ROWS - 1) * CARD_GAP;
        int gap = 8;
        int cancelH = 20;
        int bottomPad = 10;
        panelH = titleH + gridH + gap + cancelH + bottomPad;

        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int pad = 10;
        int innerX = panelX + pad;
        int innerW = panelW - pad * 2;

        gridRect = new Rect(innerX, panelY + titleH, innerW, gridH);
        cardW = (gridRect.w() - (COLS - 1) * CARD_GAP) / COLS;
        cancelRect = new Rect(innerX, gridRect.y() + gridH + gap, innerW, cancelH);

        int count = ButtonData.ButtonIcon.values().length;
        int rows = (int) Math.ceil(count / (double) COLS);
        int contentH = rows * CARD_H + (rows - 1) * CARD_GAP;
        maxScroll = Math.max(0, contentH - gridRect.h());
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
        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, COL_PANEL_BORDER);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, COL_PANEL);
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, COL_ACCENT);
        g.fillGradient(panelX, panelY + 2, panelX + panelW, panelY + 14, COL_PANEL_TOP, 0);
        centeredText(g, this.title, panelX + panelW / 2, panelY + 8, COL_TEXT);

        g.enableScissor(gridRect.x(), gridRect.y(), gridRect.x() + gridRect.w(), gridRect.y() + gridRect.h());
        ButtonData.ButtonIcon[] icons = ButtonData.ButtonIcon.values();
        for (int i = 0; i < icons.length; i++) {
            Rect r = cardRect(i);
            if (r.y() + r.h() < gridRect.y() || r.y() > gridRect.y() + gridRect.h()) continue;
            boolean hover = r.contains(mouseX, mouseY) && gridRect.contains(mouseX, mouseY);
            g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), hover ? COL_CARD_HOVER : COL_CARD);
            ButtonData.ButtonIcon icon = icons[i];
            String label = icon == ButtonData.ButtonIcon.NONE
                    ? net.minecraft.locale.Language.getInstance().getOrDefault("macrogrid.icon.none")
                    : icon.getSymbol() + " " + icon.getName();
            String trimmed = this.font.plainSubstrByWidth(label, r.w() - 4);
            g.text(this.font, trimmed, r.x() + 3, r.y() + r.h() / 2 - 4,
                    hover ? COL_TEXT : COL_TEXT_DIM, false);
        }
        g.disableScissor();

        if (maxScroll > 0) {
            int trackX = gridRect.x() + gridRect.w() + 2;
            g.fill(trackX, gridRect.y(), trackX + 2, gridRect.y() + gridRect.h(), 0x22FFFFFF);
            int thumbH = Math.max(8, gridRect.h() * gridRect.h() / (gridRect.h() + maxScroll));
            int thumbY = gridRect.y() + (int) ((gridRect.h() - thumbH) * (scroll / (float) maxScroll));
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, COL_ACCENT);
        }

        boolean cancelHover = cancelRect.contains(mouseX, mouseY);
        g.fill(cancelRect.x(), cancelRect.y(), cancelRect.x() + cancelRect.w(), cancelRect.y() + cancelRect.h(),
                cancelHover ? 0x28FFFFFF : 0x18FFFFFF);
        centeredText(g, Component.translatable("macrogrid.icon.cancel"), cancelRect.x() + cancelRect.w() / 2, cancelRect.y() + 6, COL_TEXT);
    }

    private void centeredText(GuiGraphicsExtractor g, Object text, int centerX, int y, int color) {
        if (text instanceof Component c) {
            g.text(this.font, c, centerX - this.font.width(c) / 2, y, color, false);
        } else {
            String s = text.toString();
            g.text(this.font, s, centerX - this.font.width(s) / 2, y, color, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button == 0) {
            if (cancelRect.contains(mouseX, mouseY)) { onCancel.run(); return true; }
            if (gridRect.contains(mouseX, mouseY)) {
                ButtonData.ButtonIcon[] icons = ButtonData.ButtonIcon.values();
                for (int i = 0; i < icons.length; i++) {
                    Rect r = cardRect(i);
                    if (r.y() + r.h() < gridRect.y() || r.y() > gridRect.y() + gridRect.h()) continue;
                    if (r.contains(mouseX, mouseY)) { onIconSelected.accept(icons[i]); return true; }
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (gridRect.contains(mouseX, mouseY) && maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (scrollY * 16)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void removed() {
        ButtonGuiScreen.setOpen(false);
        super.removed();
    }

    @Override
    public void onClose() {
        onCancel.run();
    }
}