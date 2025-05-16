package org.kurva.werlii.mixin;

import net.minecraft.client.MinecraftClient;
import org.kurva.werlii.client.WerliiClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        // This will be called every tick
        if (WerliiClient.getInstance() != null && 
            WerliiClient.getInstance().getModuleManager() != null) {
            WerliiClient.getInstance().getModuleManager().onTick((MinecraftClient)(Object)this);
        }
    }
    
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(boolean tick, CallbackInfo ci) {
        // This will be called after rendering
        if (WerliiClient.getInstance() != null && 
            WerliiClient.getInstance().getModuleManager() != null) {
            // Use a default tick delta of 1.0f since we can't access it directly
            float tickDelta = 1.0f;
            WerliiClient.getInstance().getModuleManager().onRender(tickDelta);
        }
    }
    
    @Inject(method = "handleInputEvents", at = @At("RETURN"))
    private void onHandleInputEvents(CallbackInfo ci) {
        // This will be called when handling input events
        if (WerliiClient.getInstance() != null && 
            WerliiClient.getInstance().getModuleManager() != null) {
            WerliiClient.getInstance().getKeyBindingHandler().handleKeyBindings();
        }
    }
}

