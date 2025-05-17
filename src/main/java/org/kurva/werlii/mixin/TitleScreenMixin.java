package org.kurva.werlii.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.kurva.werlii.client.ui.screens.AccountManagerScreen;
import org.kurva.werlii.Werlii;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void addAcManagerButton(CallbackInfo ci) {
        Werlii.LOGGER.info("Adding AcManager button to TitleScreen");
        TitleScreen screen = (TitleScreen) (Object) this;
        ButtonWidget button = ButtonWidget.builder(
                        Text.literal("AcManager"),
                        btn -> MinecraftClient.getInstance().setScreen(new AccountManagerScreen(screen))
                )
                .position(screen.width / 2 - 100, screen.height / 4 + 144)
                .size(200, 20)
                .build();

        try {
            java.lang.reflect.Method addDrawableChild = Screen.class.getDeclaredMethod("addDrawableChild", net.minecraft.client.gui.Element.class);
            addDrawableChild.setAccessible(true);
            addDrawableChild.invoke(screen, button);
            addDrawableChild.setAccessible(false);
            Werlii.LOGGER.info("Successfully added AcManager button via reflection");
        } catch (Exception e) {
            Werlii.LOGGER.error("Failed to add AcManager button via reflection", e);
        }
    }
}