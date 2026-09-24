package real.o0h.gui;

import real.o0h.config.ButtonData;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

/**
 * A color picker with an HSV gradient canvas. The user clicks or drags on
 * the gradient to pick an arbitrary RGB color, or picks one of the preset
 * Minecraft colors below, then confirms with "Use This Color".
 *
 * The picked color is reported as a hex string like "FF5B8C".
 */
public class ColorPickerScreen extends BaseOwoScreen<FlowLayout> {

    private final Consumer<String> onColorSelected; // hex "RRGGBB", null handled as default
    private final Runnable onCancel;
    private final String initialHex;

    private ColorCanvas canvas;
    private BoxComponent previewBox;
    private LabelComponent hexLabel;
    private String selectedHex;

    public ColorPickerScreen(String initialHex, Consumer<String> onColorSelected, Runnable onCancel) {
        super(Component.translatable("macrogrid.color.title"));
        this.initialHex = initialHex;
        this.selectedHex = initialHex;
        this.onColorSelected = onColorSelected;
        this.onCancel = onCancel;
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

        FlowLayout panel = UIContainers.verticalFlow(
            Sizing.fixed(300),
            Sizing.content()
        );
        panel.surface(Surface.flat(0xB0000000)).padding(Insets.of(6));

        panel.child(
            UIComponents.label(
                Component.literal("Choose a Color")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
            ).margins(Insets.bottom(4))
        );

        // The HSV gradient canvas.
        canvas = new ColorCanvas(initialHex, this::setSelected);
        canvas
            .horizontalSizing(Sizing.fill(100))
            .verticalSizing(Sizing.fixed(180))
            .margins(Insets.bottom(4));
        panel.child(canvas);

        // Live preview row: swatch + hex text.
        FlowLayout previewRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        previewRow.gap(4).verticalAlignment(VerticalAlignment.CENTER);
        previewBox = UIComponents.box(Sizing.fixed(28), Sizing.fixed(14));
        previewBox.color(Color.ofArgb(parseHex(selectedHex)));
        previewRow.child(previewBox);
        hexLabel = UIComponents.label(Component.literal("#" + selectedHex));
        previewRow.child(hexLabel);
        panel.child(previewRow.margins(Insets.bottom(6)));

        // Preset Minecraft colors (one row).
        FlowLayout presetRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        presetRow.gap(2).margins(Insets.bottom(6));

        String[][] presets = {
            { "Default", null },
            { "§cRed", "FF5555" },
            { "§aGreen", "55FF55" },
            { "§9Blue", "5555FF" },
            { "§eYellow", "FFFF55" },
            { "§5Purple", "AA00AA" },
            { "§bAqua", "55FFFF" },
            { "§6Gold", "FFAA00" },
            { "§7Gray", "AAAAAA" },
            { "§0Black", "000000" },
        };
        for (String[] preset : presets) {
            final String label = preset[0];
            final String hex = preset[1];
            var btn = UIComponents.button(Component.literal(label), b -> {
                if (hex == null) setSelected(null);
                else setSelected(hex);
            });
            btn.horizontalSizing(Sizing.fill(10));
            btn.tooltip(Component.literal(hex == null ? "Default color" : "#" + hex));
            presetRow.child(btn);
        }
        panel.child(presetRow);

        // Confirm / cancel row.
        FlowLayout actionRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        actionRow.gap(4);

        var useBtn = UIComponents.button(
            Component.literal("§a✔  Use This Color"),
            b -> onColorSelected.accept(selectedHex)
        );
        useBtn.horizontalSizing(Sizing.fill(50));

        var cancelBtn = UIComponents.button(
            Component.literal("§c✖  Cancel"),
            b -> onCancel.run()
        );
        cancelBtn.horizontalSizing(Sizing.fill(50));

        actionRow.child(useBtn).child(cancelBtn);
        panel.child(actionRow);

        root.child(panel);
    }

    private void setSelected(String hex) {
        selectedHex = hex;
        if (previewBox != null) previewBox.color(Color.ofArgb(parseHex(hex)));
        if (hexLabel != null) hexLabel.text(Component.literal(hex == null ? "Default" : "#" + hex));
        if (canvas != null) canvas.setSelected(hex);
    }

    private static int parseHex(String hex) {
        try {
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }

    /** HSV gradient canvas component: hue along X, brightness along Y. */
    public static class ColorCanvas extends BaseUIComponent {

        private float hue = 0f;      // 0..1
        private float brightness = 1f; // 0..1
        private int selectedRgb = 0xFFFFFFFF;

        private final Consumer<String> onPick;

        ColorCanvas(String initialHex, Consumer<String> onPick) {
            this.onPick = onPick;
            if (initialHex != null) setSelected(initialHex);
        }

        void setSelected(String hex) {
            if (hex == null) return;
            try {
                int rgb = Integer.parseInt(hex, 16);
                float[] hsv = rgbToHsv(rgb);
                hue = hsv[0];
                brightness = hsv[2];
                selectedRgb = rgb;
            } catch (Exception ignored) {}
        }

        @Override
        public void draw(OwoUIGraphics ctx, int mouseX, int mouseY, float partialTicks, float delta) {
            // Horizontal hue gradient, each column a vertical brightness ramp.
            int segments = 24;
            for (int i = 0; i < segments; i++) {
                float h = i / (float) segments;
                int top = 0xFF000000 | hsvToRgb(h, 1f, 1f);
                int bottom = 0xFF000000 | hsvToRgb(h, 1f, 0f);
                int x1 = x() + i * width() / segments;
                int x2 = x() + (i + 1) * width() / segments;
                ctx.drawGradientRect(x1, y(), x2, y() + height(), top, top, bottom, bottom);
            }

            // Selection marker.
            int cx = x() + (int) (hue * width());
            int cy = y() + (int) ((1f - brightness) * height());
            ctx.drawCircle(cx, cy, 4, 1.2, Color.WHITE);
            ctx.drawCircle(cx, cy, 4, 0.6, Color.BLACK);
        }

        @Override
        public boolean onMouseDown(MouseButtonEvent event, boolean doubled) {
            if (event.button() == 0) {
                pick(event.x(), event.y());
                return true;
            }
            return super.onMouseDown(event, doubled);
        }

        @Override
        public boolean onMouseDrag(MouseButtonEvent event, double dragX, double dragY) {
            if (event.button() == 0) {
                pick(event.x(), event.y());
                return true;
            }
            return super.onMouseDrag(event, dragX, dragY);
        }

        private void pick(double mx, double my) {
            if (width() <= 0 || height() <= 0) return;
            float h = (float) Math.min(1, Math.max(0, (mx - x()) / width()));
            float v = (float) Math.min(1, Math.max(0, 1 - (my - y()) / height()));
            hue = h;
            brightness = v;
            selectedRgb = 0xFF000000 | hsvToRgb(h, 1f, v);
            if (onPick != null) onPick.accept(String.format("%06X", selectedRgb & 0xFFFFFF));
        }

        static int hsvToRgb(float h, float s, float v) {
            int i = (int) (h * 6);
            float f = h * 6 - i;
            float p = v * (1 - s);
            float q = v * (1 - f * s);
            float t = v * (1 - (1 - f) * s);
            float r, g, b;
            switch (i % 6) {
                case 0: r = v; g = t; b = p; break;
                case 1: r = q; g = v; b = p; break;
                case 2: r = p; g = v; b = t; break;
                case 3: r = p; g = q; b = v; break;
                case 4: r = t; g = p; b = v; break;
                default: r = v; g = p; b = q; break;
            }
            return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
        }

        static float[] rgbToHsv(int rgb) {
            float r = ((rgb >> 16) & 0xFF) / 255f;
            float g = ((rgb >> 8) & 0xFF) / 255f;
            float b = (rgb & 0xFF) / 255f;
            float max = Math.max(r, Math.max(g, b));
            float min = Math.min(r, Math.min(g, b));
            float d = max - min;
            float h;
            if (d == 0) h = 0;
            else if (max == r) h = ((g - b) / d) % 6;
            else if (max == g) h = (b - r) / d + 2;
            else h = (r - g) / d + 4;
            h = (h * 60 + 360) % 360 / 360f;
            float s = max == 0 ? 0 : d / max;
            return new float[] { h, s, max };
        }
    }
}
