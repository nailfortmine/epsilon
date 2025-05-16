package org.kurva.werlii.client.module.combat;

import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class AutoInventoryTotem extends Module {
    private final NumberSetting healthThresholdS;
    private final BooleanSetting prioritizeHotbarS;
    private final BooleanSetting onlyWhenLowS;
    private final NumberSetting delayS;
    private int totemDelay = 0;
    
    public AutoInventoryTotem() {
        super("AutoInventoryTotem", "Automatically moves totems from inventory to hotbar", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        healthThresholdS = new NumberSetting("Health Threshold", "Health threshold to activate", this, 10.0, 1.0, 20.0, 1.0);
        prioritizeHotbarS = new BooleanSetting("Prioritize Hotbar", "Prioritize moving totems to hotbar", this, true);
        onlyWhenLowS = new BooleanSetting("Only When Low", "Only activate when health is low", this, false);
        delayS = new NumberSetting("Delay", "Delay between moves (ticks)", this, 1.0, 0.0, 20.0, 1.0);
        
        addSetting(healthThresholdS);
        addSetting(prioritizeHotbarS);
        addSetting(onlyWhenLowS);
        addSetting(delayS);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        
        if (onlyWhenLowS.getValue() && mc.player.getHealth() > healthThresholdS.getValue()) return;
        
        if (totemDelay > 0) {
            totemDelay--;
            return;
        }
        
        if (prioritizeHotbarS.getValue()) {
            moveTotemToHotbar();
        } else {
            moveTotemToOffhand();
        }
    }
    
    private void moveTotemToHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                    moveTotemToOffhand();
                }
                return;
            }
        }
        
        int totemSlot = -1;
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }
        
        if (totemSlot != -1) {
            int emptySlot = -1;
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).isEmpty()) {
                    emptySlot = i;
                    break;
                }
            }
            
            if (emptySlot == -1) emptySlot = 0;
            
            int containerTotemSlot = totemSlot < 36 ? totemSlot : totemSlot - 36;
            int containerEmptySlot = emptySlot < 9 ? emptySlot + 36 : emptySlot;
            
            mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                containerTotemSlot,
                0,
                SlotActionType.PICKUP,
                mc.player
            );
            
            mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                containerEmptySlot,
                0,
                SlotActionType.PICKUP,
                mc.player
            );
            
            if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
                mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    containerTotemSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );
            }
            
            totemDelay = delayS.getValue().intValue();
        }
    }
    
    private void moveTotemToOffhand() {
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            if (prioritizeHotbarS.getValue()) {
                moveTotemToHotbar();
            }
            return;
        }
        
        int totemSlot = -1;
        
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemSlot = i;
                break;
            }
        }
        
        if (totemSlot == -1) {
            for (int i = 9; i < 36; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                    totemSlot = i;
                    break;
                }
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
            
            if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
                mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    containerSlot,
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );
            }
            
            totemDelay = delayS.getValue().intValue();
        }
    }
}

