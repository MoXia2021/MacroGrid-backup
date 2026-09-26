package real.o0h.gui;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.network.chat.Component;

/**
 * Shared widget factories for the owo-based MacroGrid UI.
 *
 * <p>owo leaf buttons cannot take custom surfaces, so clickable elements are
 * built as {@code FlowLayout} containers with a centered label and a
 * left-click handler. All factories keep the same visuals as before; this
 * class only centralizes the duplicated construction code.</p>
 */
public final class UiFactory {

    private UiFactory() {}

    /** Sub-card container used inside the edit screen. */
    public static FlowLayout card() {
        FlowLayout card = UIContainers.verticalFlow(
            Sizing.fill(100),
            Sizing.content()
        );
        card.surface(UITheme.subCard())
            .padding(Insets.of(4))
            .margins(Insets.bottom(4));
        return card;
    }

    /** Small dim section header (e.g. "Commands"). */
    public static LabelComponent cardHeader(String text) {
        LabelComponent label = UIComponents.label(Component.literal(text));
        label.color(Color.ofArgb(UITheme.COL_TEXT_DIM))
            .margins(Insets.bottom(2));
        return label;
    }

    /** Card-style clickable element with a fixed pixel width. */
    public static FlowLayout clickableCard(
        Component text, int accent, int width, Runnable onClick
    ) {
        return clickable(text, accent, Sizing.fixed(width), Sizing.fixed(18),
            false, onClick, null);
    }

    /** Card-style clickable element with a percentage width (grid cells). */
    public static FlowLayout clickableCard(
        Component text, int accent, int widthPct, int height,
        boolean emphasized, Runnable onClick, java.util.List<Component> tooltip
    ) {
        return clickable(text, accent, Sizing.fill(widthPct), Sizing.fixed(height),
            emphasized, onClick, tooltip);
    }

    /** Pill-style clickable element for toolbars and action rows. */
    public static FlowLayout clickablePill(
        Component text, int accent, int accentDim, boolean active,
        int width, Runnable onClick
    ) {
        FlowLayout pill = clickable(text, accent,
            Sizing.fixed(width), Sizing.fixed(18), false, onClick, null);
        pill.surface(UITheme.pill(accent, accentDim, active));
        return pill;
    }

    /** Full-width pill (used for the picker Cancel button). */
    public static FlowLayout fullWidthPill(
        Component text, int accent, int accentDim, Runnable onClick
    ) {
        FlowLayout pill = clickable(text, accent,
            Sizing.fill(100), Sizing.fixed(18), false, onClick, null);
        pill.surface(UITheme.pill(accent, accentDim, false));
        return pill;
    }

    /** Vertical scroll container with the standard visible scrollbar. */
    public static ScrollContainer<?> scrollable(FlowLayout content, int height) {
        var scroll = UIContainers.verticalScroll(
            Sizing.fill(100),
            Sizing.fixed(height),
            content
        );
        scroll.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        scroll.scrollbarThiccness(5);
        return scroll;
    }

    /** Shared construction of a clickable FlowLayout with a centered label. */
    private static FlowLayout clickable(
        Component text, int accent, Sizing width, Sizing height,
        boolean emphasized, Runnable onClick,
        java.util.List<Component> tooltip
    ) {
        FlowLayout element = UIContainers.horizontalFlow(width, height);
        element.surface(UITheme.card(accent, emphasized));
        element.horizontalAlignment(HorizontalAlignment.CENTER);
        element.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent lbl = UIComponents.label(text);
        lbl.shadow(false);
        element.child(lbl);
        element.mouseDown().subscribe((click, doubled) -> {
            if (click.button() == 0) {
                if (onClick != null) onClick.run();
                return true;
            }
            return false;
        });
        if (tooltip != null) element.tooltip(tooltip);
        return element;
    }
}
