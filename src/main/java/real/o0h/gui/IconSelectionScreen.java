package real.o0h.gui;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import real.o0h.config.ButtonData;

/**
 * Grid picker for the icon set (sword, heart, arrows, numbers, ...).
 */
public class IconSelectionScreen
    extends AbstractGridPickerScreen<ButtonData.ButtonIcon> {

    public IconSelectionScreen(
        Consumer<ButtonData.ButtonIcon> onSelected, Runnable onCancel
    ) {
        super(onSelected, onCancel);
    }

    @Override
    protected String title() {
        return "Choose an Icon";
    }

    @Override
    protected List<ButtonData.ButtonIcon> values() {
        return Arrays.asList(ButtonData.ButtonIcon.values());
    }

    @Override
    protected int panelWidth() {
        return 300;
    }

    @Override
    protected int gridHeight() {
        return 180;
    }

    @Override
    protected String labelFor(ButtonData.ButtonIcon icon) {
        return icon == ButtonData.ButtonIcon.NONE
            ? "None"
            : icon.getSymbol() + " " + icon.getName();
    }

    @Override
    protected String tooltipFor(ButtonData.ButtonIcon icon) {
        return icon.getName();
    }
}
