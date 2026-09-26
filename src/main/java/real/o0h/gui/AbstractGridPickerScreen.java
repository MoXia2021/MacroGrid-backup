package real.o0h.gui;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Shared picker screen for the color and icon grids: a titled panel with a
 * 4-column scrollable grid of clickable cells and a Cancel pill at the
 * bottom. Subclasses only provide the item list and its presentation.
 *
 * @param <T> the enum type being picked (ButtonColor or ButtonIcon)
 */
public abstract class AbstractGridPickerScreen<T extends Enum<T>>
    extends MacroGridScreen {

    protected final Consumer<T> onSelected;
    protected final Runnable onCancel;

    private static final int COLUMNS = 4;

    protected AbstractGridPickerScreen(
        Consumer<T> onSelected, Runnable onCancel
    ) {
        this.onSelected = onSelected;
        this.onCancel = onCancel;
    }

    protected abstract String title();

    protected abstract List<T> values();

    protected abstract int panelWidth();

    protected abstract int gridHeight();

    /** Cell text; "default"/"none" items render dimmed by the subclass. */
    protected abstract String labelFor(T item);

    protected abstract String tooltipFor(T item);

    @Override
    protected void build(FlowLayout root) {
        configureRoot(root);

        FlowLayout panel = createPanel(panelWidth(), 6);

        panel.child(UIComponents.label(
                Component.literal(title()).withStyle(ChatFormatting.BOLD))
            .color(Color.ofArgb(UITheme.COL_TEXT))
            .margins(Insets.bottom(4)));

        FlowLayout grid = UIContainers.verticalFlow(
            Sizing.fill(100), Sizing.content()
        );

        FlowLayout row = null;
        int col = 0;
        for (T item : values()) {
            if (col == 0) {
                row = UIContainers.horizontalFlow(
                    Sizing.fill(100), Sizing.content()
                );
                row.gap(2).margins(Insets.bottom(2));
            }

            final T captured = item;
            FlowLayout cell = UIContainers.horizontalFlow(
                Sizing.fill(100 / COLUMNS), Sizing.fixed(18)
            );
            cell.surface(UITheme.card(UITheme.COL_ACCENT_DIM));
            cell.horizontalAlignment(HorizontalAlignment.CENTER);
            cell.verticalAlignment(VerticalAlignment.CENTER);
            LabelComponent lbl = UIComponents.label(
                Component.literal(labelFor(item))
            );
            lbl.shadow(false);
            cell.child(lbl);
            cell.tooltip(Component.literal(tooltipFor(item)));
            cell.mouseDown().subscribe((click, doubled) -> {
                if (click.button() == 0) {
                    onSelected.accept(captured);
                    return true;
                }
                return false;
            });

            row.child(cell);
            col++;
            if (col >= COLUMNS) {
                grid.child(row);
                col = 0;
                row = null;
            }
        }
        if (row != null) grid.child(row);

        var scroll = UiFactory.scrollable(grid, gridHeight());
        scroll.margins(Insets.bottom(4));
        panel.child(scroll);

        FlowLayout cancelBtn = UiFactory.fullWidthPill(
            Component.literal("Cancel"),
            UITheme.COL_DANGER, UITheme.COL_DANGER_DIM, onCancel
        );
        panel.child(cancelBtn);

        root.child(panel);
    }
}
