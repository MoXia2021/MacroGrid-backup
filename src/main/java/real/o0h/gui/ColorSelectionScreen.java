package real.o0h.gui;

import real.o0h.config.ButtonData;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
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
        root.surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fixed(320), Sizing.content());
        panel.surface(Surface.flat(0xB0000000)).padding(Insets.of(6));

        panel.child(UIComponents.label(
                        Component.literal("Choose a Color")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
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
            var btn = UIComponents.button(Component.literal(label),
                    b -> onColorSelected.accept(captured));
            btn.horizontalSizing(Sizing.fill(25));
            btn.tooltip(Component.literal(color.getDisplayName()));

            row.child(btn);
            col++;

            if (col >= COLORS_PER_ROW) {
                colorGrid.child(row);
                col = 0;
                row = null;
            }
        }
        if (col > 0 && row != null) colorGrid.child(row);

        var scroll = UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(200), colorGrid);
        scroll.margins(Insets.bottom(4));
        panel.child(scroll);

        var cancelBtn = UIComponents.button(Component.literal("Cancel"), b -> onCancel.run());
        cancelBtn.horizontalSizing(Sizing.fill(100));
        panel.child(cancelBtn);

        root.child(panel);
    }
}
