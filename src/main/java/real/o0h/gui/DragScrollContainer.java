package real.o0h.gui;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.ParentUIComponent;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Vertical scroll container with two extra behaviors used by MacroGrid:
 *
 * <ul>
 *   <li><b>Drag-to-scroll</b> - pressing on non-interactive background
 *       (labels, gaps, card surfaces) and dragging scrolls the content;
 *       buttons and text boxes still receive normal clicks, and the mouse
 *       wheel also works.</li>
 *   <li><b>Centering</b> - when the content fits inside the viewport it is
 *       centered vertically instead of being pinned to the top, so the
 *       panel never "fills the screen".</li>
 * </ul>
 */

public class DragScrollContainer<C extends UIComponent> extends ScrollContainer<C> {

    private boolean dragScrolling = false;
    private double lastDragScreenY = -1;

    public DragScrollContainer(
        ScrollDirection direction,
        Sizing horizontalSizing,
        Sizing verticalSizing,
        C child
    ) {
        super(direction, horizontalSizing, verticalSizing, child);
    }

    @Override
    protected int childMountY() {
        int mounted = super.childMountY();
        // When the content is smaller than the viewport, center it vertically.
        if (childSize < height()) {
            return mounted + (height() - childSize) / 2;
        }
        return mounted;
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent event, boolean inside) {
        double screenX = this.x() + event.x();
        double screenY = this.y() + event.y();

        // Only claim the press when it lands on the container's background
        // (non-interactive area), not on the scrollbar itself, and there is
        // actually something to scroll.
        if (
            inside &&
            !isInScrollbar(screenX, screenY) &&
            maxScroll > 0 &&
            !overInteractive(screenX, screenY)
        ) {
            dragScrolling = true;
            lastDragScreenY = screenY;
            return true;
        }
        return super.onMouseDown(event, inside);
    }

    @Override
    public boolean onMouseDrag(MouseButtonEvent event, double mouseX, double mouseY) {
        if (dragScrolling) {
            double screenY = this.y() + event.y();
            // Dragging up reveals content below; dragging down goes back up.
            scrollBy(lastDragScreenY - screenY, true, true);
            lastDragScreenY = screenY;
            return true;
        }
        return super.onMouseDrag(event, mouseX, mouseY);
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent event) {
        dragScrolling = false;
        return super.onMouseUp(event);
    }

    /**
     * Returns true when the given screen-space point is over a widget that
     * should keep normal mouse handling (buttons, text boxes).
     */
    private boolean overInteractive(double screenX, double screenY) {
        UIComponent child = child();
        if (child == null || !(child instanceof ParentUIComponent parent)) {
            return false;
        }
        UIComponent hit = parent.childAt((int) screenX, (int) screenY);
        return hit instanceof ButtonComponent || hit instanceof TextBoxComponent;
    }
}
