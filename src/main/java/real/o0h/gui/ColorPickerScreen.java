package real.o0h.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.SliderComponent;
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
 * A full color editor: SV gradient canvas + vertical hue bar on the left,
 * linked HSV/RGBA sliders with numeric inputs, and a HEX input.
 * The picked color is reported as a hex string like "FF5B8C".
 */
public class ColorPickerScreen extends BaseOwoScreen<FlowLayout> {

    private final Consumer<String> onColorSelected; // hex "RRGGBB", null = default
    private final Runnable onCancel;
    private final String initialHex;

    // Working color state (all normalized 0..1)
    private float h = 0f, s = 0f, v = 1f;
    private boolean updating = false;

    private SvCanvas svCanvas;
    private HueBar hueBar;
    private BoxComponent previewBox;
    private LabelComponent hexLabel;

    private SliderComponent hSlider, sSlider, vSlider, rSlider, gSlider, bSlider;
    private TextBoxComponent hBox, sBox, vBox, rBox, gBox, bBox, hexBox;
    private LabelComponent hexValueLabel;

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
        root.surface(Surface.VANILLA_TRANSLUCENT)
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout panel = UIContainers.verticalFlow(
            Sizing.fixed(400),
            Sizing.content()
        );
        panel.surface(Surface.flat(0xB0000000)).padding(Insets.of(6));

        panel.child(
            UIComponents.label(
                Component.literal("Choose a Color")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
            ).margins(Insets.bottom(4))
        );

        // Top row: SV canvas + hue bar + preview swatch.
        FlowLayout topRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        topRow.gap(4).margins(Insets.bottom(4));

        svCanvas = new SvCanvas(arr -> onSvPicked(arr[0], arr[1]));
        svCanvas
            .horizontalSizing(Sizing.fill(100))
            .verticalSizing(Sizing.fixed(190));
        topRow.child(svCanvas);

        hueBar = new HueBar(this::onHuePicked);
        hueBar
            .horizontalSizing(Sizing.fixed(14))
            .verticalSizing(Sizing.fixed(190));
        topRow.child(hueBar);

        FlowLayout previewCol = UIContainers.verticalFlow(
            Sizing.fixed(58),
            Sizing.content()
        );
        previewCol.verticalAlignment(VerticalAlignment.CENTER);
        previewBox = UIComponents.box(Sizing.fixed(46), Sizing.fixed(46));
        previewBox.color(Color.ofArgb(rgbInt()));
        previewCol.child(previewBox);
        hexValueLabel = UIComponents.label(Component.literal(toHex()).withStyle(ChatFormatting.BOLD));
        previewCol.child(hexValueLabel.margins(Insets.top(2)));
        topRow.child(previewCol);

        panel.child(topRow);

        // Slider rows: H S V R G B, each with a numeric box.
        hSlider = slider(0.0); sSlider = slider(1.0); vSlider = slider(1.0);
        rSlider = slider(1.0); gSlider = slider(1.0); bSlider = slider(1.0);
        hBox = numBox(); sBox = numBox(); vBox = numBox();
        rBox = numBox(); gBox = numBox(); bBox = numBox();

        hSlider.onChanged().subscribe(val -> { if (!updating) { h = (float) val; refresh(); } });
        sSlider.onChanged().subscribe(val -> { if (!updating) { s = (float) val; refresh(); } });
        vSlider.onChanged().subscribe(val -> { if (!updating) { v = (float) val; refresh(); } });
        rSlider.onChanged().subscribe(val -> { if (!updating) { fromRgb((int) (val * 255), r(), g()); refresh(); } });
        gSlider.onChanged().subscribe(val -> { if (!updating) { fromRgb(r(), (int) (val * 255), b()); refresh(); } });
        bSlider.onChanged().subscribe(val -> { if (!updating) { fromRgb(r(), g(), (int) (val * 255)); refresh(); } });

        hBox.setResponder(t -> parseIntBox(t, 360, v -> h = v / 360f));
        sBox.setResponder(t -> parseIntBox(t, 100, v -> s = v / 100f));
        vBox.setResponder(t -> parseIntBox(t, 100, vv -> v = vv / 100f));
        rBox.setResponder(t -> parseIntBox(t, 255, v -> fromRgb(v, r(), g())));
        gBox.setResponder(t -> parseIntBox(t, 255, v -> fromRgb(r(), v, b())));
        bBox.setResponder(t -> parseIntBox(t, 255, v -> fromRgb(r(), g(), v)));

        panel.child(sliderRow("§cH:", hSlider, hBox));
        panel.child(sliderRow("§aS:", sSlider, sBox));
        panel.child(sliderRow("§9V:", vSlider, vBox));
        panel.child(sliderRow("§4R:", rSlider, rBox));
        panel.child(sliderRow("§2G:", gSlider, gBox));
        panel.child(sliderRow("§1B:", bSlider, bBox));

        // HEX input row.
        FlowLayout hexRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        hexRow.gap(4).verticalAlignment(VerticalAlignment.CENTER).margins(Insets.top(2));
        hexRow.child(
            UIComponents.label(Component.literal("HEX:")).horizontalSizing(Sizing.fixed(32))
        );
        hexBox = UIComponents.textBox(Sizing.fill(100));
        hexBox.setValue(toHex());
        hexBox.setFilter(s -> s.matches("[0-9a-fA-F]{0,6}"));
        hexBox.setResponder(t -> {
            if (updating) return;
            if (t.length() == 6 && t.matches("[0-9a-fA-F]{6}")) {
                applyHex(t);
                refresh();
            }
        });
        hexRow.child(hexBox);
        panel.child(hexRow);

        // Action row.
        FlowLayout actionRow = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        actionRow.gap(4).margins(Insets.top(6));

        var defaultBtn = UIComponents.button(Component.literal("§7Default"), b ->
            onColorSelected.accept(null));
        defaultBtn.horizontalSizing(Sizing.fill(25));

        var useBtn = UIComponents.button(
            Component.literal("§a✔  Use This Color"),
            b -> onColorSelected.accept(toHex())
        );
        useBtn.horizontalSizing(Sizing.fill(37));

        var cancelBtn = UIComponents.button(
            Component.literal("§c✖  Cancel"),
            b -> onCancel.run()
        );
        cancelBtn.horizontalSizing(Sizing.fill(37));

        actionRow.child(defaultBtn).child(useBtn).child(cancelBtn);
        panel.child(actionRow);

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

    /** Push the current HSV state into every control. */
    private void refresh() {
        updating = true;
        hSlider.value(h);
        sSlider.value(s);
        vSlider.value(v);
        int[] rgb = hsvToRgb(h, s, v);
        rSlider.value(rgb[0] / 255f);
        gSlider.value(rgb[1] / 255f);
        bSlider.value(rgb[2] / 255f);
        hBox.setValue(String.valueOf((int) (h * 360)));
        sBox.setValue(String.valueOf((int) (s * 100)));
        vBox.setValue(String.valueOf((int) (v * 100)));
        rBox.setValue(String.valueOf(rgb[0]));
        gBox.setValue(String.valueOf(rgb[1]));
        bBox.setValue(String.valueOf(rgb[2]));
        hexBox.setValue(toHex());
        previewBox.color(Color.ofArgb(0xFF000000 | rgbInt()));
        hexValueLabel.text(Component.literal("#" + toHex()).withStyle(ChatFormatting.BOLD));
        svCanvas.setHue(h);
        svCanvas.setSelection(s, v);
        hueBar.setHue(h);
        updating = false;
    }

    private SliderComponent slider(double value) {
        SliderComponent slider = UIComponents.slider(Sizing.fill(100));
        slider.value(value);
        slider.scrollStep(0.01);
        slider.message(v -> Component.literal(""));
        return slider;
    }

    private TextBoxComponent numBox() {
        TextBoxComponent box = UIComponents.textBox(Sizing.fixed(36));
        box.setFilter(s -> s.matches("\\d*"));
        return box;
    }

    private FlowLayout sliderRow(String label, SliderComponent slider, TextBoxComponent box) {
        FlowLayout row = UIContainers.horizontalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        row.gap(4).verticalAlignment(VerticalAlignment.CENTER);
        row.child(
            UIComponents.label(Component.literal(label)).horizontalSizing(Sizing.fixed(20))
        );
        row.child(slider.horizontalSizing(Sizing.fill(100)));
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

    private String toHex() {
        return String.format("%06X", rgbInt());
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
            int segments = 20;
            for (int i = 0; i < segments; i++) {
                float s = i / (float) segments;
                int top = 0xFF000000 | pack(hsvToRgb(hue, s, 1f));
                int bottom = 0xFF000000 | pack(hsvToRgb(hue, s, 0f));
                int x1 = x() + i * width() / segments;
                int x2 = x() + (i + 1) * width() / segments;
                ctx.drawGradientRect(x1, y(), x2, y() + height(), top, top, bottom, bottom);
            }
            // selection marker
            int cx = x() + (int) (sat * width());
            int cy = y() + (int) ((1f - val) * height());
            ctx.drawCircle(cx, cy, 4, 1.2, Color.WHITE);
            ctx.drawCircle(cx, cy, 4, 0.6, Color.BLACK);
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
            sat = (float) Math.min(1, Math.max(0, (mx - x()) / width()));
            val = (float) Math.min(1, Math.max(0, 1 - (my - y()) / height()));
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
            int segments = 24;
            for (int i = 0; i < segments; i++) {
                float h1 = i / (float) segments;
                float h2 = (i + 1) / (float) segments;
                int color = 0xFF000000 | pack(hsvToRgb(h1, 1f, 1f));
                int y1 = y() + i * height() / segments;
                int y2 = y() + (i + 1) * height() / segments;
                ctx.drawGradientRect(x(), y1, x() + width(), y2, color, color, color, color);
            }
            // selection marker: horizontal ticks at current hue
            int cy = y() + (int) (hue * height());
            ctx.drawRectOutline(x(), cy - 2, x() + width(), cy + 2, 1);
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
            hue = (float) Math.min(1, Math.max(0, (my - y()) / height()));
            if (onPick != null) onPick.accept(hue);
        }
    }

    static int pack(int[] rgb) {
        return (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }
}
