package real.o0h.gui;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import real.o0h.config.ButtonData;

/**
 * Grid picker for the 24 standard colors (16 Minecraft format-code colors
 * plus 8 extended custom-RGB colors), plus "Default".
 */
public class ColorSelectionScreen
    extends AbstractGridPickerScreen<ButtonData.ButtonColor> {

    public ColorSelectionScreen(
        Consumer<ButtonData.ButtonColor> onSelected, Runnable onCancel
    ) {
        super(onSelected, onCancel);
    }

    @Override
    protected String title() {
        return "Choose a Color";
    }

    @Override
    protected List<ButtonData.ButtonColor> values() {
        return Arrays.asList(ButtonData.ButtonColor.values());
    }

    @Override
    protected int panelWidth() {
        return 320;
    }

    @Override
    protected int gridHeight() {
        return 200;
    }

    @Override
    protected String labelFor(ButtonData.ButtonColor color) {
        return color == ButtonData.ButtonColor.DEFAULT
            ? "§7Default"
            : color.getDisplayName();
    }

    @Override
    protected String tooltipFor(ButtonData.ButtonColor color) {
        return color.getDisplayName();
    }
}
