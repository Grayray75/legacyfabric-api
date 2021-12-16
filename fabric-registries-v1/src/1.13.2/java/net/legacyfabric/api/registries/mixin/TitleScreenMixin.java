package net.legacyfabric.api.registries.mixin;

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ifThisWorksIWillCrab(CallbackInfo ci) {
        System.out.println("WHAT THE FUCK. HOW IS THIS WORKING");
    }
}
