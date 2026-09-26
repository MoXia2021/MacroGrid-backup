package real.o0h.gui;

import real.o0h.config.ButtonData;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;

/**
 * Grid picker for the 24 standard colors (16 Minecraft format-code colors
 * plus 8 extended custom-RGB colors), plus "Default". Mirrors the layout
 * of the icon picker: 4 columns, scrollable grid, Cancel at the bottom.
 */
public class ColorSelectionScreen extends BaseOwoScreen<FlowLayout> {

    private final Consumer<ButtonData.ButtonColor> onColorSelected;
    private final Runnable onCancel;

    private static final int COLORS_PER_ROW = 4;

    public ColorSelectionScreen(Consumer<ButtonData.ButtonColor> onColorSelected, Runnable onCancel) {
        this.onColorSelected = onColorSelected;
        this.onCancel         = onCancel;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.flat(UITheme.COL_DIM))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fixed(320), Sizing.content());
        panel.surface(UITheme.panel()).padding(Insets.of(6));

        panel.child(UIComponents.label(
                        Component.literal("Choose a Color")
                                .withStyle(ChatFormatting.BOLD))
                .color(Color.ofArgb(UITheme.COL_TEXT))
                .margins(Insets.bottom(4)));

        FlowLayout colorGrid = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());

        FlowLayout row = null;
        int col = 0;

        for (ButtonData.ButtonColor color : ButtonData.ButtonColor.values()) {
            if (col == 0) {
                row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
                row.gap(2).margins(Insets.bottom(2));
            }

            String label = (color == ButtonData.ButtonColor.DEFAULT)
                    ? "§7Default"
                    : color.getDisplayName();

            final ButtonData.ButtonColor captured = color;
            FlowLayout cell = UIContainers.horizontalFlow(
                Sizing.fill(25), Sizing.fixed(18)
            );
            cell.surface(UITheme.card(UITheme.COL_ACCENT_DIM));
            cell.horizontalAlignment(HorizontalAlignment.CENTER);
            cell.verticalAlignment(VerticalAlignment.CENTER);
            LabelComponent lbl = UIComponents.label(Component.literal(label));
            lbl.shadow(false);
            cell.child(lbl);
            cell.tooltip(Component.literal(color.getDisplayName()));
            cell.mouseDown().subscribe((click, doubled) -> {
                if (click.button() == 0) { onColorSelected.accept(captured); return true; }
                return false;
            });

            row.child(cell);
            col++;

            if (col >= COLORS_PER_ROW) {
                colorGrid.child(row);
                col = 0;
                row = null;
            }
        }
        if (col > 0 && row != null) colorGrid.child(row);

        var scroll = UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(200), colorGrid);
        scroll
            .scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
            .scrollbarThiccness(5);
        scroll.margins(Insets.bottom(4));
        panel.child(scroll);

        FlowLayout cancelBtn = UIContainers.horizontalFlow(
            Sizing.fill(100), Sizing.fixed(18)
        );
        cancelBtn.surface(UITheme.pill(UITheme.COL_DANGER, UITheme.COL_DANGER_DIM, false));
        cancelBtn.horizontalAlignment(HorizontalAlignment.CENTER);
        cancelBtn.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent cancelLbl = UIComponents.label(Component.literal("Cancel"));
        cancelLbl.shadow(false);
        cancelBtn.child(cancelLbl);
        cancelBtn.mouseDown().subscribe((click, doubled) -> {
            if (click.button() == 0) { onCancel.run(); return true; }
            return false;
        });
        panel.child(cancelBtn);

        root.child(panel);
    }
}
