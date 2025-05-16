package org.kurva.werlii.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.render.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        NoRender noRender = (NoRender) WerliiClient.getInstance().getModuleManager().getModuleByName("NoRender");
        if (noRender != null && !noRender.shouldRenderFire()) {
            ci.cancel();
        }
    }

    
    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderUnderwaterOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        NoRender noRender = (NoRender) WerliiClient.getInstance().getModuleManager().getModuleByName("NoRender");
        if (noRender != null && !noRender.shouldRenderWater()) {
            ci.cancel();
        }
    }
}

