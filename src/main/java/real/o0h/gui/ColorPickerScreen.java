package real.o0h.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
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
 * A full color editor matching the classic layout: SV gradient canvas with a
 * cross-hair marker + vertical hue bar on the left, preview swatch below,
 * H/S/V/R/G/B/A gradient sliders with numeric inputs on the right,
 * HEX input at the bottom, and a confirm button at the bottom right.
 * The window is a compact fixed-size panel (~1/4 of the screen), centered,
 * with a transparent backdrop so only the panel itself is visible.
 *
 * The picked color is reported as a hex string "RRGGBB" (alpha is shown in
 * the editor but not stored, since button colors cannot be translucent).
 */
public class ColorPickerScreen extends BaseOwoScreen<FlowLayout> {

    private final Consumer<String> onColorSelected; // hex "RRGGBB", null = default
    private final Runnable onCancel;
    private final String initialHex;

    // Working color state (all normalized 0..1)
    private float h = 0f, s = 0f, v = 1f, a = 1f;
    private boolean updating = false;

    private SvCanvas svCanvas;
    private HueBar hueBar;
    private BoxComponent previewBox;

    private GradientSlider hSlider, sSlider, vSlider, rSlider, gSlider, bSlider, aSlider;
    private TextBoxComponent hBox, sBox, vBox, rBox, gBox, bBox, aBox, hexBox;

    public ColorPickerScreen(String initialHex, Consumer<String> onColorSelected, Runnable onCancel) {
        super(Component.translatable("macrogrid.color.title"));
        this.initialHex = initialHex;
        this.onColorSelected = onColorSelected;
        this.onCancel = onCancel;
        if (initialHex != null) applyHex(initialHex);
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        // Compact fixed-size window (~1/4 of the screen), centered.
        // All sizes are fixed in GUI units; no fill() siblings, so nothing
        // overflows, stretches, or gets pushed out of the window.
        root.surface(Surface.flat(0x00000000))
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER);

        int pad = 8;
        int winW = Math.max(200, (int) (this.width * 0.5));
        int svH = Math.max(50, (int) (this.height * 0.22));
        int hueBarW = 14;
        int previewH = Math.max(20, svH / 3);
        int leftW = (int) (winW * 0.45);
        int rightW = winW - pad * 2 - leftW - 10;

        FlowLayout panel = UIContainers.verticalFlow(
            Sizing.fixed(winW),
            Sizing.content()
        );
        panel.surface(Surface.flat(0xC0000000)).padding(Insets.of(pad));

        panel.child(
            UIComponents.label(
                Component.literal("颜色编辑器")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
            ).margins(Insets.bottom(6))
        );

        // Body: left column (canvas + preview) and right column (sliders).
        FlowLayout body = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        body.gap(10);

        // ---- Left column ----
        FlowLayout left = UIContainers.verticalFlow(
            Sizing.fixed(leftW),
            Sizing.content()
        );
        left.gap(4);

        FlowLayout svRow = UIContainers.horizontalFlow(
            Sizing.fixed(leftW),
            Sizing.fixed(svH)
        );
        svRow.gap(4);

        svCanvas = new SvCanvas(arr -> onSvPicked(arr[0], arr[1]));
        svCanvas
            .horizontalSizing(Sizing.fixed(leftW - hueBarW - 4))
            .verticalSizing(Sizing.fixed(svH));
        svRow.child(svCanvas);

        hueBar = new HueBar(this::onHuePicked);
        hueBar
            .horizontalSizing(Sizing.fixed(hueBarW))
            .verticalSizing(Sizing.fixed(svH));
        svRow.child(hueBar);

        left.child(svRow);

        previewBox = UIComponents.box(Sizing.fixed(previewH), Sizing.fixed(previewH));
        previewBox.color(Color.ofArgb(0xFF000000 | rgbInt()));
        left.child(previewBox);

        // ---- Right column ----
        FlowLayout right = UIContainers.verticalFlow(
            Sizing.fixed(rightW),
            Sizing.content()
        );
        right.gap(2).verticalAlignment(VerticalAlignment.CENTER);

        hSlider = gradSlider(new int[] { 0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000 }, v -> { if (!updating) { h = v.floatValue(); refresh(); } });
        sSlider = gradSlider(new int[] { 0xFFFFFFFF, 0xFFFF0000 }, v -> { if (!updating) { s = v.floatValue(); refresh(); } });
        vSlider = gradSlider(new int[] { 0xFF000000, 0xFFFF0000 }, vv -> { if (!updating) { v = vv.floatValue(); refresh(); } });
        rSlider = gradSlider(new int[] { 0xFF000000, 0xFFFF0000 }, v -> { if (!updating) { fromRgb((int) (v * 255), g(), b()); refresh(); } });
        gSlider = gradSlider(new int[] { 0xFF000000, 0xFF00FF00 }, v -> { if (!updating) { fromRgb(r(), (int) (v * 255), b()); refresh(); } });
        bSlider = gradSlider(new int[] { 0xFF000000, 0xFF0000FF }, v -> { if (!updating) { fromRgb(r(), g(), (int) (v * 255)); refresh(); } });
        aSlider = gradSlider(new int[] { 0xFF000000, 0xFFFFFFFF }, v -> { if (!updating) { a = v.floatValue(); refresh(); } });

        hBox = numBox(); sBox = numBox(); vBox = numBox();
        rBox = numBox(); gBox = numBox(); bBox = numBox(); aBox = numBox();

        hBox.setResponder(t -> parseIntBox(t, 360, v -> h = v / 360f));
        sBox.setResponder(t -> parseIntBox(t, 100, v -> s = v / 100f));
        vBox.setResponder(t -> parseIntBox(t, 100, vv -> v = vv / 100f));
        rBox.setResponder(t -> parseIntBox(t, 255, v -> fromRgb(v, g(), b())));
        gBox.setResponder(t -> parseIntBox(t, 255, v -> fromRgb(r(), v, b())));
        bBox.setResponder(t -> parseIntBox(t, 255, v -> fromRgb(r(), g(), v)));
        aBox.setResponder(t -> parseIntBox(t, 255, v -> a = v / 255f));

        right.child(sliderRow("H:", hSlider, hBox));
        right.child(sliderRow("S:", sSlider, sBox));
        right.child(sliderRow("V:", vSlider, vBox));
        right.child(sliderRow("R:", rSlider, rBox));
        right.child(sliderRow("G:", gSlider, gBox));
        right.child(sliderRow("B:", bSlider, bBox));
        right.child(sliderRow("A:", aSlider, aBox));

        // HEX input row.
        FlowLayout hexRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        hexRow.gap(4).verticalAlignment(VerticalAlignment.CENTER);
        hexRow.child(
            UIComponents.label(Component.literal("HEX:")).horizontalSizing(Sizing.fixed(32))
        );
        hexBox = UIComponents.textBox(Sizing.fill(100));
        hexBox.setValue(toHex());
        hexBox.setBordered(false);
        hexBox.setFilter(s -> s.matches("[0-9a-fA-F]{0,8}"));
        hexBox.setResponder(t -> {
            if (updating) return;
            if (t.length() == 6 && t.matches("[0-9a-fA-F]{6}")) { applyHex(t); refresh(); }
            else if (t.length() == 8 && t.matches("[0-9a-fA-F]{8}")) { applyArgb(t); refresh(); }
        });
        hexRow.child(hexBox);
        right.child(hexRow.margins(Insets.top(4)));

        // Confirm button at the bottom right.
        FlowLayout confirmRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        confirmRow.horizontalAlignment(HorizontalAlignment.RIGHT);
        var confirmBtn = UIComponents.button(
            Component.literal("✔ 确认"),
            b -> onColorSelected.accept(rgbHex())
        );
        confirmBtn.horizontalSizing(Sizing.fixed(90));
        confirmRow.child(confirmBtn.margins(Insets.top(6)));
        right.child(confirmRow);

        body.child(left).child(right);
        panel.child(body);

        root.child(panel);
        refresh();
    }

    // ---- helpers ----

    private void parseIntBox(String text, int max, Consumer<Integer> apply) {
        if (updating) return;
        try {
            int val = Integer.parseInt(text.trim());
            apply.accept(Math.max(0, Math.min(max, val)));
            refresh();
        } catch (NumberFormatException ignored) {}
    }

    private void onSvPicked(float s, float v) {
        if (updating) return;
        this.s = s;
        this.v = v;
        refresh();
    }

    private void onHuePicked(float h) {
        if (updating) return;
        this.h = h;
        refresh();
    }

    /** Push the current color state into every control. */
    private void refresh() {
        updating = true;
        int[] rgb = hsvToRgb(h, s, v);
        hSlider.setValue(h);
        sSlider.setValue(s);
        vSlider.setValue(v);
        rSlider.setValue(rgb[0] / 255f);
        gSlider.setValue(rgb[1] / 255f);
        bSlider.setValue(rgb[2] / 255f);
        aSlider.setValue(a);
        // S/V slider gradients follow the current hue.
        int pure = 0xFF000000 | pack(rgb);
        sSlider.setColors(new int[] { 0xFFFFFFFF, pure });
        vSlider.setColors(new int[] { 0xFF000000, pure });
        hBox.setValue(String.valueOf((int) (h * 360)));
        sBox.setValue(String.valueOf((int) (s * 100)));
        vBox.setValue(String.valueOf((int) (v * 100)));
        rBox.setValue(String.valueOf(rgb[0]));
        gBox.setValue(String.valueOf(rgb[1]));
        bBox.setValue(String.valueOf(rgb[2]));
        aBox.setValue(String.valueOf((int) (a * 255)));
        hexBox.setValue(toHex());
        previewBox.color(Color.ofArgb(((int) (a * 255) << 24) | rgbInt()));
        svCanvas.setHue(h);
        svCanvas.setSelection(s, v);
        hueBar.setHue(h);
        updating = false;
    }

    private GradientSlider gradSlider(int[] colors, Consumer<Double> onChanged) {
        GradientSlider slider = new GradientSlider(colors, onChanged);
        slider.horizontalSizing(Sizing.fill(100));
        slider.verticalSizing(Sizing.fixed(12));
        return slider;
    }

    private TextBoxComponent numBox() {
        TextBoxComponent box = UIComponents.textBox(Sizing.fixed(32));
        box.setFilter(s -> s.matches("\\d*"));
        box.setBordered(false);
        return box;
    }

    private FlowLayout sliderRow(String label, GradientSlider slider, TextBoxComponent box) {
        FlowLayout row = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        row.gap(4).verticalAlignment(VerticalAlignment.CENTER);
        row.child(
            UIComponents.label(Component.literal(label)).horizontalSizing(Sizing.fixed(14))
        );
        row.child(slider);
        row.child(box);
        return row;
    }

    // ---- color math ----

    private int r() { return hsvToRgb(h, s, v)[0]; }
    private int g() { return hsvToRgb(h, s, v)[1]; }
    private int b() { return hsvToRgb(h, s, v)[2]; }

    private int rgbInt() {
        int[] rgb = hsvToRgb(h, s, v);
        return (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }

    private String rgbHex() {
        return String.format("%06X", rgbInt());
    }

    private String toHex() {
        return String.format("%02X%06X", (int) (a * 255), rgbInt());
    }

    private void fromRgb(int r, int g, int b) {
        float[] hsv = rgbToHsv(
            Math.max(0, Math.min(255, r)),
            Math.max(0, Math.min(255, g)),
            Math.max(0, Math.min(255, b))
        );
        h = hsv[0];
        s = hsv[1];
        v = hsv[2];
    }

    private void applyHex(String hex) {
        try {
            int rgb = Integer.parseInt(hex, 16);
            fromRgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (Exception ignored) {}
    }

    private void applyArgb(String hex) {
        try {
            int argb = (int) Long.parseLong(hex, 16);
            a = ((argb >> 24) & 0xFF) / 255f;
            fromRgb((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
        } catch (Exception ignored) {}
    }

    static int[] hsvToRgb(float h, float s, float v) {
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
        return new int[] { (int) (r * 255), (int) (g * 255), (int) (b * 255) };
    }

    static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == rf) h = ((gf - bf) / d) % 6;
        else if (max == gf) h = (bf - rf) / d + 2;
        else h = (rf - gf) / d + 4;
        h = (h * 60 + 360) % 360 / 360f;
        float s = max == 0 ? 0 : d / max;
        return new float[] { h, s, max };
    }

    /** SV canvas: X = saturation (left 0, right 1), Y = value (top 1, bottom 0), at fixed hue. */
    public static class SvCanvas extends BaseUIComponent {
        private float hue = 0f;
        private float sat = 0f;
        private float val = 1f;
        private final Consumer<float[]> onPick; // {sat, val}

        SvCanvas(Consumer<float[]> onPick) { this.onPick = onPick; }

        void setHue(float h) { hue = h; }
        void setSelection(float s, float v) { sat = s; val = v; }

        @Override
        public void draw(OwoUIGraphics ctx, int mouseX, int mouseY, float partialTicks, float delta) {
            if (width() <= 0 || height() <= 0) return;
            // Per-pixel gradient for smooth color transitions (no banding).
            // drawGradientRect signature: (x, y, width, height, tl, tr, br, bl)
            for (int i = 0; i < width(); i++) {
                float s = i / (float) width();
                int top = 0xFF000000 | pack(hsvToRgb(hue, s, 1f));
                int bottom = 0xFF000000 | pack(hsvToRgb(hue, s, 0f));
                ctx.drawGradientRect(x() + i, y(), 1, height(), top, top, bottom, bottom);
            }
            // Cross-hair marker spanning the whole canvas (white 2px + dark shadow).
            int cx = x() + (int) (sat * width());
            int cy = y() + (int) ((1f - val) * height());
            ctx.drawGradientRect(x(), cy + 3, width(), 1, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000);
            ctx.drawGradientRect(cx + 3, y(), 1, height(), 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000);
            ctx.drawGradientRect(x(), cy, width(), 2, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);
            ctx.drawGradientRect(cx, y(), 2, height(), 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);
        }

        @Override
        public boolean onMouseDown(MouseButtonEvent event, boolean doubled) {
            if (event.button() == 0) { pick(event.x(), event.y()); return true; }
            return super.onMouseDown(event, doubled);
        }

        @Override
        public boolean onMouseDrag(MouseButtonEvent event, double dragX, double dragY) {
            if (event.button() == 0) { pick(event.x(), event.y()); return true; }
            return super.onMouseDrag(event, dragX, dragY);
        }

        private void pick(double mx, double my) {
            if (width() <= 0 || height() <= 0) return;
            sat = (float) Math.min(1, Math.max(0, mx / width()));
            val = (float) Math.min(1, Math.max(0, 1 - my / height()));
            if (onPick != null) onPick.accept(new float[] { sat, val });
        }
    }

    /** Vertical hue bar: top red -> bottom red, hue 0..1. */
    public static class HueBar extends BaseUIComponent {
        private float hue = 0f;
        private final Consumer<Float> onPick;

        HueBar(Consumer<Float> onPick) { this.onPick = onPick; }

        void setHue(float h) { hue = h; }

        @Override
        public void draw(OwoUIGraphics ctx, int mouseX, int mouseY, float partialTicks, float delta) {
            if (height() <= 0) return;
            // Per-pixel gradient for smooth hue transitions (no banding).
            for (int i = 0; i < height(); i++) {
                float h1 = i / (float) height();
                int color = 0xFF000000 | pack(hsvToRgb(h1, 1f, 1f));
                ctx.drawGradientRect(x(), y() + i, width(), 1, color, color, color, color);
            }
            // Horizontal tick at current hue (white 2px + dark shadow).
            int cy = y() + (int) (hue * height());
            ctx.drawGradientRect(x(), cy + 2, width(), 1, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000);
            ctx.drawGradientRect(x(), cy, width(), 2, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);
        }

        @Override
        public boolean onMouseDown(MouseButtonEvent event, boolean doubled) {
            if (event.button() == 0) { pick(event.y()); return true; }
            return super.onMouseDown(event, doubled);
        }

        @Override
        public boolean onMouseDrag(MouseButtonEvent event, double dragX, double dragY) {
            if (event.button() == 0) { pick(event.y()); return true; }
            return super.onMouseDrag(event, dragX, dragY);
        }

        private void pick(double my) {
            if (height() <= 0) return;
            hue = (float) Math.min(1, Math.max(0, my / height()));
            if (onPick != null) onPick.accept(hue);
        }
    }

    /** Horizontal gradient slider: gradient track + draggable handle. */
    public static class GradientSlider extends BaseUIComponent {
        private double value = 0; // 0..1
        private int[] colors = { 0xFFFFFFFF, 0xFF000000 };
        private final Consumer<Double> onChanged;

        GradientSlider(int[] colors, Consumer<Double> onChanged) {
            this.colors = colors;
            this.onChanged = onChanged;
        }

        void setValue(double v) { value = Math.min(1, Math.max(0, v)); }
        void setColors(int[] colors) { this.colors = colors; }

        @Override
        public void draw(OwoUIGraphics ctx, int mouseX, int mouseY, float partialTicks, float delta) {
            if (width() <= 0 || height() <= 0) return;
            int n = colors.length;
            if (n >= 2) {
                // Horizontal gradient segments: left = colors[i], right = colors[i+1].
                for (int i = 0; i < n - 1; i++) {
                    int x1 = x() + i * width() / (n - 1);
                    int segW = (i + 1) * width() / (n - 1) - x1;
                    ctx.drawGradientRect(x1, y(), segW, height(),
                        colors[i], colors[i + 1], colors[i + 1], colors[i]);
                }
            } else {
                ctx.drawGradientRect(x(), y(), width(), height(),
                    colors[0], colors[0], colors[0], colors[0]);
            }
            ctx.drawRectOutline(x(), y(), width(), height(), 0xFF888888);
            // Handle: white 2px bar with dark shadow.
            int hx = x() + (int) (value * (width() - 1));
            ctx.drawGradientRect(hx + 3, y(), 1, height(), 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000);
            ctx.drawGradientRect(hx, y(), 2, height(), 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);
        }

        @Override
        public boolean onMouseDown(MouseButtonEvent event, boolean doubled) {
            if (event.button() == 0) { pick(event.x()); return true; }
            return super.onMouseDown(event, doubled);
        }

        @Override
        public boolean onMouseDrag(MouseButtonEvent event, double dragX, double dragY) {
            if (event.button() == 0) { pick(event.x()); return true; }
            return super.onMouseDrag(event, dragX, dragY);
        }

        private void pick(double mx) {
            if (width() <= 0) return;
            value = Math.min(1, Math.max(0, mx / width()));
            if (onChanged != null) onChanged.accept(value);
        }
    }

    static int pack(int[] rgb) {
        return (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }
}
