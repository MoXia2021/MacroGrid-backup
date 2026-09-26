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

public class IconSelectionScreen extends BaseOwoScreen<FlowLayout> {

    private final Consumer<ButtonData.ButtonIcon> onIconSelected;
    private final Runnable onCancel;

    private static final int ICONS_PER_ROW = 4;

    public IconSelectionScreen(Consumer<ButtonData.ButtonIcon> onIconSelected, Runnable onCancel) {
        this.onIconSelected = onIconSelected;
        this.onCancel       = onCancel;
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

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fixed(300), Sizing.content());
        panel.surface(UITheme.panel()).padding(Insets.of(6));

        panel.child(UIComponents.label(
                        Component.literal("Choose an Icon")
                                .withStyle(ChatFormatting.BOLD))
                .color(Color.ofArgb(UITheme.COL_TEXT))
                .margins(Insets.bottom(4)));

        FlowLayout iconGrid = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());

        FlowLayout row = null;
        int col = 0;

        for (ButtonData.ButtonIcon icon : ButtonData.ButtonIcon.values()) {
            if (col == 0) {
                row = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
                row.gap(2).margins(Insets.bottom(2));
            }

            String label = (icon == ButtonData.ButtonIcon.NONE)
                    ? "None"
                    : icon.getSymbol() + " " + icon.getName();

            final ButtonData.ButtonIcon captured = icon;
            var btn = UIComponents.button(Component.literal(label),
                    b -> onIconSelected.accept(captured));
            btn.horizontalSizing(Sizing.fill(25));
            btn.tooltip(Component.literal(icon.getName()));

            row.child(btn);
            col++;

            if (col >= ICONS_PER_ROW) {
                iconGrid.child(row);
                col = 0;
                row = null;
            }
        }
        if (col > 0 && row != null) iconGrid.child(row);

        var scroll = UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(180), iconGrid);
        scroll.margins(Insets.bottom(4));
        panel.child(scroll);

        var cancelBtn = UIComponents.button(Component.literal("Cancel"), b -> onCancel.run());
        cancelBtn.horizontalSizing(Sizing.fill(100));
        panel.child(cancelBtn);

        root.child(panel);
    }
}
