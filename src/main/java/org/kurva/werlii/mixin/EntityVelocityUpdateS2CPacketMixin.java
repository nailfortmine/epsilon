package org.kurva.werlii.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.combat.AntiKnockback;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityVelocityUpdateS2CPacket.class)
public class EntityVelocityUpdateS2CPacketMixin {
    @Shadow private int velocityX;
    @Shadow private int velocityY;
    @Shadow private int velocityZ;
    @Shadow private int entityId; // Changed from 'id' to 'entityId'
    
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && entityId == mc.player.getId()) {
            AntiKnockback antiKnockback = (AntiKnockback) WerliiClient.getInstance()
                .getModuleManager().getModuleByName("AntiKnockback");
            
            if (antiKnockback != null && antiKnockback.isEnabled()) {
                BooleanSetting attacksSetting = (BooleanSetting) antiKnockback.getSetting("Attacks");
                if (attacksSetting != null && attacksSetting.getValue()) {
                    NumberSetting horizontalSetting = (NumberSetting) antiKnockback.getSetting("Horizontal");
                    NumberSetting verticalSetting = (NumberSetting) antiKnockback.getSetting("Vertical");
                    
                    if (horizontalSetting != null && verticalSetting != null) {
                        double horizontalReduction = horizontalSetting.getValue() / 100.0;
                        double verticalReduction = verticalSetting.getValue() / 100.0;
                        
                        velocityX *= (1.0 - horizontalReduction);
                        velocityY *= (1.0 - verticalReduction);
                        velocityZ *= (1.0 - horizontalReduction);
                    }
                }
            }
        }
    }
}

