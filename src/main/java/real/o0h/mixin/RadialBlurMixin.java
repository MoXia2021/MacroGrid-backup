package real.o0h.mixin;
import real.o0h.gui.RadialOverlayScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class RadialBlurMixin {

    @Inject(method = "applyBlur(F)V", at = @At("HEAD"), cancellable = true)
    private void noBlurForRadial(float blurRadius, CallbackInfo ci) {
        if ((Object) this instanceof RadialOverlayScreen) {
            ci.cancel();
        }
    }
}
