package real.o0h.gui;

import real.o0h.config.ButtonData;
import real.o0h.config.ButtonManager;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RadialMenu {

    private static boolean visible      = false;
    private static int     hoveredIndex = -2;

    private static float cx, cy;

    private static final float RADIUS    = 110f;
    private static final float DEAD_ZONE = 34f;
    private static final int   BTN_W     = 86;
    private static final int   BTN_H     = 18;

    public static void register() {
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("commandguibuttons", "radial_menu"),
            new HudElement() {
                @Override
                public void extractRenderState(@NotNull GuiGraphicsExtractor ctx,
                                               @NotNull DeltaTracker tick) {
                    if (!visible) return;
                    // fuck mojang, owo, fabric.
                    render(OwoUIGraphics.of(ctx));
                }
            }
        );
    }

    public static void show() {
        Minecraft mc = Minecraft.getInstance();
        cx = mc.getWindow().getGuiScaledWidth()  / 2f;
        cy = mc.getWindow().getGuiScaledHeight() / 2f;
        hoveredIndex = -2;
        visible = true;
    }

    public static void hide(boolean execute) {
        if (!visible) return;
        visible = false;

        int idx = hoveredIndex;
        hoveredIndex = -2;

        if (!execute) return;

        if (idx == -1) {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.setScreen(new ButtonGuiScreen()));
            return;
        }
        if (idx < 0) return;

        List<ButtonData> buttons = ButtonManager.getButtons();
        if (idx < buttons.size()) executeButton(buttons.get(idx));
    }

    public static boolean isVisible() { return visible; }

    // -------------------------------------------------------------------------

    private static void render(OwoUIGraphics gfx) {
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        double scale = mc.getWindow().getGuiScale();
        double[] rawX = new double[1], rawY = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(mc.getWindow().handle(), rawX, rawY);
        float mx = (float)(rawX[0] / scale);
        float my = (float)(rawY[0] / scale);

        float dx = mx - cx, dy = my - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        List<ButtonData> buttons = ButtonManager.getButtons();
        int count = buttons.size();

        if (count > 0) {
            if (dist <= DEAD_ZONE) {
                hoveredIndex = -1;
            } else {
                float angle  = (float) Math.toDegrees(Math.atan2(dy, dx));
                if (angle < 0) angle += 360f;
                float sector = 360f / count;
                float adj    = (angle + 90f + sector / 2f) % 360f;
                hoveredIndex = (int)(adj / sector) % count;
            }
        } else {
            hoveredIndex = -1;
        }

        fill(gfx, 0, 0, sw, sh, 0x66000000);

        if (count > 0) {
            float sector = 360f / count;
            for (int i = 0; i < count; i++) {
                float aRad = (float) Math.toRadians(i * sector - 90f);
                float bx   = cx + (float)(Math.cos(aRad) * RADIUS);
                float by   = cy + (float)(Math.sin(aRad) * RADIUS);
                drawLine(gfx, (int)cx, (int)cy, (int)bx, (int)by,
                        (i == hoveredIndex) ? 0x885599FF : 0x33888888);
            }
        }
        drawCircle(gfx, (int)cx, (int)cy, (int)RADIUS, 0x33FFFFFF);

        if (count > 0) {
            float sector = 360f / count;
            for (int i = 0; i < count; i++) {
                float aRad = (float) Math.toRadians(i * sector - 90f);
                float bx   = cx + (float)(Math.cos(aRad) * RADIUS);
                float by   = cy + (float)(Math.sin(aRad) * RADIUS);
                drawBtn(gfx, mc, bx, by, BTN_W, BTN_H,
                        makeLabel(buttons.get(i)), i == hoveredIndex);
            }
        } else {
            drawCenteredText(gfx, mc, "§cNo buttons! Open editor to add some.",
                    (int)cx, (int)(cy - RADIUS - 12));
        }

        drawBtn(gfx, mc, cx, cy, 70, 18, "§e⚙ Edit/Add", hoveredIndex == -1);

        String hint = switch (hoveredIndex) {
            case -2 -> "Move mouse to select";
            case -1 -> "§eRelease G to open editor";
            default -> "§aRelease G to run";
        };
        drawCenteredText(gfx, mc, hint, (int)cx, (int)(cy + RADIUS + 14));
    }

    // -------------------------------------------------------------------------

    private static void fill(OwoUIGraphics gfx, int x1, int y1, int x2, int y2, int color) {
        gfx.drawGradientRect(RenderPipelines.GUI, x1, y1, x2, y2, color, color, color, color);
    }

    private static void drawLine(OwoUIGraphics gfx, int x1, int y1, int x2, int y2, int color) {
        float ddx = x2 - x1, ddy = y2 - y1;
        int steps = (int) Math.sqrt(ddx * ddx + ddy * ddy);
        if (steps < 1) return;
        for (int i = 0; i <= steps; i++) {
            int px = x1 + (int)(ddx * i / steps);
            int py = y1 + (int)(ddy * i / steps);
            fill(gfx, px, py, px + 1, py + 1, color);
        }
    }

    private static void drawCircle(OwoUIGraphics gfx, int cx, int cy, int r, int color) {
        int steps = 64;
        for (int i = 0; i < steps; i++) {
            if (i % 3 == 2) continue;
            double a1 = 2 * Math.PI * i       / steps;
            double a2 = 2 * Math.PI * (i + 1) / steps;
            drawLine(gfx,
                (int)(cx + Math.cos(a1) * r), (int)(cy + Math.sin(a1) * r),
                (int)(cx + Math.cos(a2) * r), (int)(cy + Math.sin(a2) * r),
                color);
        }
    }

    private static void drawBtn(OwoUIGraphics gfx, Minecraft mc,
                                 float bx, float by, int w, int h,
                                 String label, boolean hovered) {
        int x = (int)(bx - w / 2f);
        int y = (int)(by - h / 2f);

        if (hovered) fill(gfx, x-2, y-2, x+w+2, y+h+2, 0x883366CC);
        fill(gfx, x-1, y-1, x+w+1, y+h+1, hovered ? 0xFF6699FF : 0xFF555555);
        fill(gfx, x,   y,   x+w,   y+h,   hovered ? 0xEE1A3A6A : 0xBB181818);

        int tw = mc.font.width(label);
        int tx = (int)(bx - tw / 2f);
        int ty = (int)(by - mc.font.lineHeight / 2f);
        gfx.text(mc.font, Component.literal(label), tx, ty,
                hovered ? 0xFFFFFFFF : 0xFFBBBBBB, true);
    }

    private static void drawCenteredText(OwoUIGraphics gfx, Minecraft mc,
                                          String text, int x, int y) {
        int tw = mc.font.width(text);
        gfx.text(mc.font, Component.literal(text), x - tw / 2, y, 0xFFAAAAAA, true);
    }

    private static String makeLabel(ButtonData btn) {
        StringBuilder sb = new StringBuilder();
        if (btn.getIcon() != null && btn.getIcon() != ButtonData.ButtonIcon.NONE)
            sb.append(btn.getIcon().getSymbol()).append(" ");
        if (btn.getColor() != null && btn.getColor() != ButtonData.ButtonColor.DEFAULT)
            sb.append(btn.getColor().getCode());
        String name = btn.getName();
        if (name.length() > 9) name = name.substring(0, 8) + "…";
        sb.append(name);
        return sb.toString();
    }

    private static void executeButton(ButtonData btn) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.execute(() -> {
            for (ButtonData.CommandEntry e : btn.getCommands()) {
                String cmd = e.getCommand();
                if (e.getType() == ButtonData.CommandType.COMMAND) {
                    if (cmd.startsWith("/")) cmd = cmd.substring(1);
                    mc.player.connection.sendCommand(cmd);
                } else {
                    mc.player.connection.sendChat(cmd);
                }
            }
        });
    }
}