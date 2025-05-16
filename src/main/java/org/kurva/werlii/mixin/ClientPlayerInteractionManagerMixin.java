package org.kurva.werlii.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.combat.Criticals;
import org.kurva.werlii.client.module.combat.CrystalOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        CrystalOptimizer optimizer = (CrystalOptimizer) WerliiClient.getInstance()
            .getModuleManager().getModuleByName("CrystalOptimizer");
        
        if (optimizer != null && optimizer.isEnabled()) {
        }
        
        Criticals criticals = (Criticals) WerliiClient.getInstance()
            .getModuleManager().getModuleByName("Criticals");
        
        if (criticals != null && criticals.isEnabled()) {
            criticals.onAttack(target);
        }
    }
}

