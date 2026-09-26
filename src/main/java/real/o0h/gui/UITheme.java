package real.o0h.gui;

import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.ParentUIComponent;
import io.wispforest.owo.ui.core.Surface;

/**
 * Shared visual theme ported from the 3.4 (MC 26.2) UI: dark blue-black
 * panel with a blue accent bar on top, translucent card buttons with a left
 * accent stripe and hover highlight, and pill-style toolbar buttons.
 * Only the visuals are ported; no functionality is added or changed.
 */
public final class UITheme {

    private UITheme() {}

    public static final int COL_DIM = 0x99000000;
    public static final int COL_PANEL = 0xF0181A22;
    public static final int COL_PANEL_TOP = 0x1AFFFFFF;
    public static final int COL_PANEL_BORDER = 0x33FFFFFF;
    public static final int COL_ACCENT = 0xFF5B8CFF;
    public static final int COL_ACCENT_DIM = 0x405B8CFF;
    public static final int COL_CARD = 0x1EFFFFFF;
    public static final int COL_CARD_HOVER = 0x38FFFFFF;
    public static final int COL_CARD_BORDER = 0x22FFFFFF;
    public static final int COL_TEXT = 0xFFEDEDF2;
    public static final int COL_TEXT_DIM = 0xFF8B93A6;
    public static final int COL_DANGER = 0xFFE5605B;
    public static final int COL_DANGER_DIM = 0x40E5605B;
    public static final int COL_WARN = 0xFFE2B93B;
    public static final int COL_WARN_DIM = 0x40E2B93B;
    public static final int COL_MOVE = COL_ACCENT;
    public static final int COL_MOVE_DIM = COL_ACCENT_DIM;

    /** RGB int (0xRRGGBB) -> Minecraft custom color code "§x§R§R§G§G§B§B". */
    public static String fmt(int rgb) {
        StringBuilder sb = new StringBuilder("§x");
        for (int shift = 20; shift >= 0; shift -= 4) {
            int d = (rgb >> shift) & 0xF;
            sb.append('§').append(Character.toUpperCase(Character.forDigit(d, 16)));
        }
        return sb.toString();
    }

    /** 3.4-style panel: border, dark body, 2px accent bar on top, soft highlight. */
    public static Surface panel() {
        return (ctx, comp) -> {
            ctx.fill(comp.x() - 1, comp.y() - 1,
                    comp.x() + comp.width() + 1, comp.y() + comp.height() + 1, COL_PANEL_BORDER);
            ctx.fill(comp.x(), comp.y(),
                    comp.x() + comp.width(), comp.y() + comp.height(), COL_PANEL);
            ctx.fill(comp.x(), comp.y(),
                    comp.x() + comp.width(), comp.y() + 2, COL_ACCENT);
            ctx.drawGradientRect(comp.x(), comp.y() + 2, comp.width(), 12,
                    COL_PANEL_TOP, COL_PANEL_TOP, 0x00000000, 0x00000000);
        };
    }

    /** Card button background: translucent fill, left accent stripe, hover border. */
    public static Surface card(int accent) {
        return card(accent, false);
    }

    /** Card with an emphasized fill (used for pending-delete / move-source cards). */
    public static Surface card(int accent, boolean emphasized) {
        return (ctx, comp) -> {
            int bg = emphasized ? accent : COL_CARD;
            ctx.fill(comp.x(), comp.y(),
                    comp.x() + comp.width(), comp.y() + comp.height(), bg);
            ctx.fill(comp.x(), comp.y(),
                    comp.x() + 2, comp.y() + comp.height(), accent);
            if (emphasized) {
                ctx.fill(comp.x(), comp.y(),
                        comp.x() + comp.width(), comp.y() + 1, accent);
            }
        };
    }

    /** Sub-card used inside edit screen: translucent fill with a soft border. */
    public static Surface subCard() {
        return (ctx, comp) -> {
            ctx.fill(comp.x(), comp.y(),
                    comp.x() + comp.width(), comp.y() + comp.height(), COL_CARD);
            ctx.drawRectOutline(comp.x(), comp.y(),
                    comp.width(), comp.height(), COL_CARD_BORDER);
        };
    }

    /** Toolbar pill: translucent fill, accent top line and accent text when active. */
    public static Surface pill(int accent, int accentDim, boolean active) {
        return (ctx, comp) -> {
            int bg = active ? accentDim : 0x18FFFFFF;
            ctx.fill(comp.x(), comp.y(),
                    comp.x() + comp.width(), comp.y() + comp.height(), bg);
            if (active) {
                ctx.fill(comp.x(), comp.y(),
                        comp.x() + comp.width(), comp.y() + 1, accent);
            }
        };
    }

    /** Input row background: dark strip with a top hairline (search box style). */
    public static Surface inputStrip() {
        return (ctx, comp) -> {
            ctx.fill(comp.x(), comp.y(),
                    comp.x() + comp.width(), comp.y() + comp.height(), 0x22000000);
            ctx.fill(comp.x(), comp.y(),
                    comp.x() + comp.width(), comp.y() + 1, COL_PANEL_BORDER);
        };
    }

    /** Static helper to draw a surface on a component (used by draw overrides). */
    public static void draw(Surface surface, OwoUIGraphics ctx, ParentUIComponent comp) {
        surface.draw(ctx, comp);
    }
}
