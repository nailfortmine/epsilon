package org.kurva.werlii.client.module.combat;

import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.kurva.werlii.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class AutoTotem extends Module {
    public AutoTotem() {
        super("AutoTotem", "Automatically puts totems in offhand", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_Y);
        this.registerKeybinding("Werlii Combat");
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        
        if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
            int totemSlot = -1;
            
            for (int i = 9; i < 45; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                    totemSlot = i;
                    break;
                }
            }
            
            if (totemSlot != -1) {
                int containerSlot = totemSlot < 36 ? totemSlot : totemSlot - 36;
                
                mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    containerSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );
                
                mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    45,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );
                
                mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    containerSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );
            }
        }
    }
}

