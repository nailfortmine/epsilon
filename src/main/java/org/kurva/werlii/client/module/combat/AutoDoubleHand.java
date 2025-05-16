package org.kurva.werlii.client.module.combat;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class AutoDoubleHand extends Module {
    private final ModeSetting mainHandItemS;
    private final ModeSetting offHandItemS;
    private final BooleanSetting swapBackS;
    private final BooleanSetting onlyInCombatS;
    private final NumberSetting delayS;
    private int swapDelay = 0;
    private int originalOffhandSlot = -1;
    private boolean isSwapped = false;
    
    public AutoDoubleHand() {
        super("AutoDoubleHand", "Automatically manages items in both hands", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        mainHandItemS = new ModeSetting("Main Hand", "Item to keep in main hand", this, "Sword", 
                "Sword", "Axe", "Crystal", "Anchor", "Gapple", "Potion", "Shield");
        
        offHandItemS = new ModeSetting("Off Hand", "Item to keep in off hand", this, "Totem", 
                "Totem", "Crystal", "Gapple", "Shield", "Anchor", "Potion");
        
        swapBackS = new BooleanSetting("Swap Back", "Swap back to original item after use", this, true);
        onlyInCombatS = new BooleanSetting("Only In Combat", "Only activate when in combat", this, true);
        delayS = new NumberSetting("Delay", "Delay between swaps (ticks)", this, 2.0, 0.0, 20.0, 1.0);
        
        addSetting(mainHandItemS);
        addSetting(offHandItemS);
        addSetting(swapBackS);
        addSetting(onlyInCombatS);
        addSetting(delayS);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        
        if (onlyInCombatS.getValue() && !isInCombat()) return;
        
        if (swapDelay > 0) {
            swapDelay--;
            return;
        }
        
        updateMainHand();
        
        updateOffHand();
    }
    
    private void updateMainHand() {
        Item targetItem = getTargetItem(mainHandItemS.getValue());
        if (targetItem == null) return;
        
        if (mc.player.getMainHandStack().getItem() == targetItem) return;
        
        int slot = findItemInHotbar(targetItem);
        if (slot != -1) {
            mc.player.getInventory().selectedSlot = slot;
            swapDelay = delayS.getValue().intValue();
        }
    }
    
    private void updateOffHand() {
        Item targetItem = getTargetItem(offHandItemS.getValue());
        if (targetItem == null) return;
        
        if (mc.player.getOffHandStack().getItem() == targetItem) return;
        
        int slot = findItemInInventory(targetItem);
        if (slot != -1) {
            if (swapBackS.getValue() && !isSwapped) {
                originalOffhandSlot = 45;
                isSwapped = true;
            }
            
            int containerSlot = slot < 36 ? slot : slot - 36;
            
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
            
            swapDelay = delayS.getValue().intValue();
        }
    }
    
    private Item getTargetItem(String itemName) {
        switch (itemName) {
            case "Sword":
                return Items.NETHERITE_SWORD;
            case "Axe":
                return Items.NETHERITE_AXE;
            case "Crystal":
                return Items.END_CRYSTAL;
            case "Totem":
                return Items.TOTEM_OF_UNDYING;
            case "Gapple":
                return Items.ENCHANTED_GOLDEN_APPLE;
            case "Shield":
                return Items.SHIELD;
            case "Anchor":
                return Items.RESPAWN_ANCHOR;
            case "Potion":
                return Items.SPLASH_POTION;
            default:
                return null;
        }
    }
    
    private int findItemInHotbar(Item targetItem) {
        boolean isSword = targetItem == Items.NETHERITE_SWORD;
        boolean isAxe = targetItem == Items.NETHERITE_AXE;
        
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            
            if (isSword && item.toString().contains("_sword")) {
                return i;
            } else if (isAxe && item.toString().contains("_axe")) {
                return i;
            } else if (item == targetItem) {
                return i;
            }
        }
        
        return -1;
    }
    
    private int findItemInInventory(Item targetItem) {
        for (int i = 9; i < 45; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == targetItem) {
                return i;
            }
        }
        
        return -1;
    }
    
    private boolean isInCombat() {
        return mc.world.getPlayers().stream()
                .anyMatch(player -> player != mc.player && 
                          player.isAlive() && 
                          !player.isSpectator() && 
                          mc.player.distanceTo(player) < 10);
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        if (swapBackS.getValue() && isSwapped && mc.player != null) {
            isSwapped = false;
        }
    }
}

