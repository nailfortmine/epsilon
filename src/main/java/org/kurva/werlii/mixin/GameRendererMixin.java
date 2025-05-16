
package org.kurva.werlii.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.kurva.werlii.client.WerliiClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        // This will be called after rendering
        if (WerliiClient.getInstance() != null && WerliiClient.getInstance().getModuleManager() != null) {
            // Use a default tick delta since we can't access it directly
            float tickDelta = 1.0f;

            // Try to get the actual tick delta if possible
            if (tickCounter != null) {
                try {
                    tickDelta = tickCounter.getTickDelta(tick); // Pass the tick boolean parameter
                } catch (Exception e) {
                    // If we can't get the tick delta, use the default value
                }
            }

            WerliiClient.getInstance().getModuleManager().onRender(tickDelta);
        }
    }
}

