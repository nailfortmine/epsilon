package org.kurva.werlii.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.kurva.werlii.client.WerliiClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // Render HUD elements
        if (WerliiClient.getInstance() != null && WerliiClient.getInstance().getHudManager() != null) {
            // Use a default tick delta since we can't access it directly
            float tickDelta = 1.0f;
            
            // Try to get the actual tick delta if possible
            if (tickCounter != null) {
                try {
                    tickDelta = tickCounter.getTickDelta(false); // Pass false as default
                } catch (Exception e) {
                    // If we can't get the tick delta, use the default value
                }
            }
            
            WerliiClient.getInstance().getHudManager().render(context, tickDelta);
        }
    }
}

