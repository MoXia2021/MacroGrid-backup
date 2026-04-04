package real.o0h.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RadialOverlayScreen extends Screen {

    public RadialOverlayScreen() {
        super(Component.empty());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }


    @Override
    public void onClose() {
        if (RadialMenu.isVisible()) RadialMenu.hide(false);
        super.onClose();
    }
}
