package real.o0h.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import real.o0h.MacroGrid;
import org.jetbrains.annotations.NotNull;

public class ModeSelectScreen extends BaseOwoScreen<FlowLayout> {

    private final boolean fromSettings;

    public ModeSelectScreen(boolean fromSettings) {
        this.fromSettings = fromSettings;
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
        panel.surface(Surface.flat(0xC0000000)).padding(Insets.of(12));

        panel.child(UIComponents.label(
                        Component.literal("Choose GUI Mode")
                                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .horizontalSizing(Sizing.fill(100))
                .margins(Insets.bottom(4)));

        panel.child(UIComponents.label(
                        Component.literal("How do you want to open your buttons?")
                                .withStyle(ChatFormatting.GRAY))
                .horizontalSizing(Sizing.fill(100))
                .margins(Insets.bottom(16)));

        // Classic card
        FlowLayout classicCard = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        classicCard.surface(Surface.flat(0x80333333)).padding(Insets.of(8)).margins(Insets.bottom(8));

        classicCard.child(UIComponents.label(
                        Component.literal("Classic Menu").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .margins(Insets.bottom(3)));
        classicCard.child(UIComponents.label(
                        Component.literal("Press G → opens a screen with all your buttons in a grid. Click to run.")
                                .withStyle(ChatFormatting.GRAY))
                .horizontalSizing(Sizing.fill(100))
                .margins(Insets.bottom(6)));

        var classicBtn = UIComponents.button(Component.literal("§aUse Classic Menu"),
                b -> select(UiConfig.GuiMode.CLASSIC));
        classicBtn.horizontalSizing(Sizing.fill(100));
        classicCard.child(classicBtn);
        panel.child(classicCard);

        // Radial card
        FlowLayout radialCard = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        radialCard.surface(Surface.flat(0x80333333)).padding(Insets.of(8)).margins(Insets.bottom(16));

        radialCard.child(UIComponents.label(
                        Component.literal("Radial Menu").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .margins(Insets.bottom(3)));
        radialCard.child(UIComponents.label(
                        Component.literal("Hold G → wheel appears around cursor. Move mouse to select, release G to run.")
                                .withStyle(ChatFormatting.GRAY))
                .horizontalSizing(Sizing.fill(100))
                .margins(Insets.bottom(6)));
        radialCard.child(UIComponents.label(
                Component.literal("Sorry, Radial doesn't work with time, as a lot has changed in 26.1. And I'm too lazy to fix it :( But if you are using 1.21.11, you can go ahead and use Radial")
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC))
                .horizontalSizing(Sizing.fill(100)));        
        // var radialBtn = UIComponents.button(Component.literal("§bUse Radial Menu"),
        //         b -> select(UiConfig.GuiMode.RADIAL));
        // radialBtn.horizontalSizing(Sizing.fill(100));
        // radialCard.child(radialBtn);
        panel.child(radialCard);

        if (fromSettings) {
            var cancelBtn = UIComponents.button(Component.literal("Cancel"), b -> onClose());
            cancelBtn.horizontalSizing(Sizing.fill(100));
            panel.child(cancelBtn);
        } else {
            panel.child(UIComponents.label(
                            Component.literal("You can change this later in the mod settings.")
                                    .withStyle(ChatFormatting.DARK_GRAY))
                    .horizontalSizing(Sizing.fill(100)));
        }

        root.child(panel);
    }

    private void select(UiConfig.GuiMode mode) {
        UiConfig.setGuiMode(mode);
        onClose();
        MacroGrid.LOGGER.info("GUI mode set to: {}", mode);
    }
}
