package org.kurva.werlii.client.module.utility;

import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class AutoEXP extends Module {
    private final NumberSetting delayS;
    private final BooleanSetting autoSwitchS;
    private final BooleanSetting onlyArmorS;
    private final BooleanSetting onlyDamagedS;
    private final NumberSetting minDurabilityS;
    private final BooleanSetting silentS;
    
    private int throwDelay = 0;
    private int originalSlot = -1;
    
    public AutoEXP() {
        super("AutoEXP", "Automatically throws experience bottles", Category.UTILITY);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Utility");
        
        delayS = new NumberSetting("Delay", "Delay between throws (ticks)", this, 2.0, 0.0, 20.0, 1.0);
        autoSwitchS = new BooleanSetting("Auto Switch", "Automatically switch to XP bottles", this, true);
        onlyArmorS = new BooleanSetting("Only Armor", "Only throw when armor needs repair", this, true);
        onlyDamagedS = new BooleanSetting("Only Damaged", "Only throw when items are damaged", this, true);
        minDurabilityS = new NumberSetting("Min Durability", "Minimum durability percentage", this, 50.0, 1.0, 100.0, 1.0);
        silentS = new BooleanSetting("Silent", "Silently switch back after throwing", this, true);
        
        addSetting(delayS);
        addSetting(autoSwitchS);
        addSetting(onlyArmorS);
        addSetting(onlyDamagedS);
        addSetting(minDurabilityS);
        addSetting(silentS);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        if (throwDelay > 0) {
            throwDelay--;
            return;
        }
        
        if (shouldThrowXP()) {
            int xpSlot = findXPBottles();
            
            if (xpSlot != -1) {
                if (autoSwitchS.getValue()) {
                    originalSlot = mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = xpSlot;
                }
                
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                
                if (silentS.getValue() && autoSwitchS.getValue() && originalSlot != -1) {
                    mc.player.getInventory().selectedSlot = originalSlot;
                    originalSlot = -1;
                }
                
                throwDelay = delayS.getValue().intValue();
            }
        }
    }
    
    private boolean shouldThrowXP() {
        if (onlyArmorS.getValue() || onlyDamagedS.getValue()) {
            double minDurability = minDurabilityS.getValue() / 100.0;
            
            if (onlyArmorS.getValue()) {
                for (int i = 0; i < 4; i++) {
                    if (!mc.player.getInventory().getArmorStack(i).isEmpty()) {
                        double durabilityPercent = getDurabilityPercent(mc.player.getInventory().getArmorStack(i));
                        if (durabilityPercent <= minDurability) {
                            return true;
                        }
                    }
                }
            }
            
            if (onlyDamagedS.getValue() && !mc.player.getMainHandStack().isEmpty()) {
                double durabilityPercent = getDurabilityPercent(mc.player.getMainHandStack());
                if (durabilityPercent <= minDurability) {
                    return true;
                }
            }
            
            return false;
        }
        
        return true;
    }
    
    private double getDurabilityPercent(net.minecraft.item.ItemStack stack) {
        if (!stack.isDamageable()) return 1.0;
        return 1.0 - (double) stack.getDamage() / stack.getMaxDamage();
    }
    
    private int findXPBottles() {
        if (mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE) {
            return mc.player.getInventory().selectedSlot;
        }
        
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.EXPERIENCE_BOTTLE) {
                return i;
            }
        }
        
        return -1;
    }
}

