package org.kurva.werlii.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.kurva.werlii.client.module.render.TargetESP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class HudRenderMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        TargetESP esp = TargetESP.getInstance();
        if (esp != null && esp.isEnabled()) {
            esp.onRender2D(context);
        }
    }
}