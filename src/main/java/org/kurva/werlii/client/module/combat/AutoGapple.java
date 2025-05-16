package org.kurva.werlii.client.module.combat;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class AutoGapple extends Module {
    private final NumberSetting healthThresholdS;
    private final BooleanSetting preferEnchantedS;
    private final BooleanSetting swapBackS;
    private final BooleanSetting onlyInCombatS;
    private final NumberSetting eatDelayS;
    
    private int originalSlot = -1;
    private boolean isEating = false;
    private int eatTicks = 0;
    
    public AutoGapple() {
        super("AutoGapple", "Automatically eats golden apples when health is low", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        healthThresholdS = new NumberSetting("Health Threshold", "Health threshold to eat", this, 10.0, 1.0, 19.0, 1.0);
        preferEnchantedS = new BooleanSetting("Prefer Enchanted", "Prefer enchanted golden apples", this, true);
        swapBackS = new BooleanSetting("Swap Back", "Swap back to original item after eating", this, true);
        onlyInCombatS = new BooleanSetting("Only In Combat", "Only eat when in combat", this, false);
        eatDelayS = new NumberSetting("Eat Delay", "Delay after eating (ticks)", this, 10.0, 0.0, 40.0, 1.0);
        
        addSetting(healthThresholdS);
        addSetting(preferEnchantedS);
        addSetting(swapBackS);
        addSetting(onlyInCombatS);
        addSetting(eatDelayS);
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        if (isEating && swapBackS.getValue() && originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = originalSlot;
            originalSlot = -1;
        }
        
        isEating = false;
        eatTicks = 0;
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        if (isEating) {
            if (mc.player.isUsingItem()) {
                return;
            } else {
                isEating = false;
                eatTicks = eatDelayS.getValue().intValue();
                
                if (swapBackS.getValue() && originalSlot != -1) {
                    mc.player.getInventory().selectedSlot = originalSlot;
                    originalSlot = -1;
                }
            }
        }
        
        if (eatTicks > 0) {
            eatTicks--;
            return;
        }
        
        if (shouldEat()) {
            eatGapple();
        }
    }
    
    private boolean shouldEat() {
        if (mc.player.getHealth() > healthThresholdS.getValue()) {
            return false;
        }
        
        if (onlyInCombatS.getValue() && !isInCombat()) {
            return false;
        }
        
        return true;
    }
    
    private boolean isInCombat() {
        return mc.world.getPlayers().stream()
                .anyMatch(player -> player != mc.player && 
                          player.isAlive() && 
                          !player.isSpectator() && 
                          mc.player.distanceTo(player) < 10);
    }
    
    private void eatGapple() {
        int gappleSlot = findGappleSlot();
        
        if (gappleSlot != -1) {
            originalSlot = mc.player.getInventory().selectedSlot;
            
            mc.player.getInventory().selectedSlot = gappleSlot;
            
            mc.options.useKey.setPressed(true);
            isEating = true;
        }
    }
    
    private int findGappleSlot() {
        int normalGappleSlot = -1;
        
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            
            if (item == Items.ENCHANTED_GOLDEN_APPLE) {
                return i;
            } else if (item == Items.GOLDEN_APPLE) {
                normalGappleSlot = i;
            }
        }
        
        if (!preferEnchantedS.getValue() || normalGappleSlot != -1) {
            return normalGappleSlot;
        }
        
        return -1;
    }
}

