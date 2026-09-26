package real.o0h.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for every MacroGrid screen. Centralizes the parts every screen
 * shares: the adapter creation (vertical flow root), the dark dim overlay
 * with centered layout, and a themed panel container.
 */
public abstract class MacroGridScreen extends BaseOwoScreen<FlowLayout> {

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    /** Dim the world behind the UI and center the panel both ways. */
    protected void configureRoot(FlowLayout root) {
        root.surface(Surface.flat(UITheme.COL_DIM))
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER);
    }

    /** Create a themed panel with the given fixed width and padding. */
    protected FlowLayout createPanel(int width, int padding) {
        FlowLayout panel = UIContainers.verticalFlow(
            Sizing.fixed(width),
            Sizing.content()
        );
        panel.surface(UITheme.panel()).padding(Insets.of(padding));
        return panel;
    }
}
