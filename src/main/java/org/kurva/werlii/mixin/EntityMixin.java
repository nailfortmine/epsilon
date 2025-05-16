package org.kurva.werlii.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.combat.Hitboxes;
import org.kurva.werlii.client.util.IStepHeight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin implements IStepHeight {
    private float customStepHeight = 0.6f;

    @Override
    public void setCustomStepHeight(float height) {
        this.customStepHeight = height;
    }

    @Override
    public float getCustomStepHeight() {
        return this.customStepHeight;
    }

    @Inject(method = "getStepHeight", at = @At("HEAD"), cancellable = true)
    private void onGetStepHeight(CallbackInfoReturnable<Float> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof IStepHeight) {
            cir.setReturnValue(((IStepHeight) entity).getCustomStepHeight());
        }
    }
    
    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void onGetBoundingBox(CallbackInfoReturnable<Box> cir) {
        Entity entity = (Entity) (Object) this;
        
        if (WerliiClient.getInstance() != null && 
            WerliiClient.getInstance().getModuleManager() != null) {
            
            Hitboxes hitboxes = (Hitboxes) WerliiClient.getInstance()
                .getModuleManager().getModuleByName("Hitboxes");
            
            if (hitboxes != null && hitboxes.isEnabled() && hitboxes.shouldExpandEntity(entity)) {
                Box originalBox = cir.getReturnValue();
                Box expandedBox = hitboxes.getExpandedBox(entity, originalBox);
                cir.setReturnValue(expandedBox);
            }
        }
    }
}

