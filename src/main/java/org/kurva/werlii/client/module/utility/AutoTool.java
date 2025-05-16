package org.kurva.werlii.client.module.utility;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class AutoTool extends Module {
    private final BooleanSetting switchBackS;
    private final BooleanSetting onlyHotbarS;
    private final BooleanSetting preferSwordS;
    private final BooleanSetting checkDurabilityS;
    private final NumberSetting minDurabilityS;
    
    private int originalSlot = -1;
    private boolean isSwitched = false;
    
    public AutoTool() {
        super("AutoTool", "Automatically switches to the best tool", Category.UTILITY);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Utility");
        
        switchBackS = new BooleanSetting("Switch Back", "Switch back to original slot", this, true);
        onlyHotbarS = new BooleanSetting("Only Hotbar", "Only use tools from hotbar", this, true);
        preferSwordS = new BooleanSetting("Prefer Sword", "Prefer sword for combat", this, true);
        checkDurabilityS = new BooleanSetting("Check Durability", "Don't use low durability tools", this, true);
        minDurabilityS = new NumberSetting("Min Durability", "Minimum tool durability percentage", this, 10.0, 1.0, 100.0, 1.0);
        
        addSetting(switchBackS);
        addSetting(onlyHotbarS);
        addSetting(preferSwordS);
        addSetting(checkDurabilityS);
        addSetting(minDurabilityS);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        if (mc.options.attackKey.isPressed()) {
            if (mc.crosshairTarget instanceof EntityHitResult) {
                switchToBestWeapon();
            } else if (mc.crosshairTarget instanceof BlockHitResult) {
                BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
                Block block = mc.world.getBlockState(hitResult.getBlockPos()).getBlock();
                switchToBestTool(block);
            }
        } else if (isSwitched && switchBackS.getValue() && originalSlot != -1) {
            mc.player.getInventory().selectedSlot = originalSlot;
            originalSlot = -1;
            isSwitched = false;
        }
    }
    
    private void switchToBestWeapon() {
        int bestSlot = -1;
        float bestDamage = -1;
        
        for (int i = 0; i < (onlyHotbarS.getValue() ? 9 : 36); i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            
            if (item instanceof SwordItem) {
                SwordItem sword = (SwordItem) item;
                float damage = 1.0f;
                if (sword instanceof ToolItem) {
                    damage += ((ToolItem) sword).getMaterial().getAttackDamage();
                }
                
                if (checkDurabilityS.getValue()) {
                    double durability = getDurabilityPercent(mc.player.getInventory().getStack(i));
                    if (durability < minDurabilityS.getValue() / 100.0) {
                        continue;
                    }
                }
                
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestSlot = i;
                }
            }
        }
        
        if (bestSlot != -1 && bestSlot < 9) {
            switchToSlot(bestSlot);
        }
    }
    
    private void switchToBestTool(Block block) {
        int bestSlot = -1;
        float bestSpeed = -1;
        
        for (int i = 0; i < (onlyHotbarS.getValue() ? 9 : 36); i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            
            if (item instanceof MiningToolItem) {
                float speed = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(block.getDefaultState());
                
                if (checkDurabilityS.getValue()) {
                    double durability = getDurabilityPercent(mc.player.getInventory().getStack(i));
                    if (durability < minDurabilityS.getValue() / 100.0) {
                        continue;
                    }
                }
                
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }
        
        if (bestSlot != -1 && bestSlot < 9) {
            switchToSlot(bestSlot);
        }
    }
    
    private void switchToSlot(int slot) {
        if (!isSwitched) {
            originalSlot = mc.player.getInventory().selectedSlot;
        }
        
        mc.player.getInventory().selectedSlot = slot;
        isSwitched = true;
    }
    
    private double getDurabilityPercent(net.minecraft.item.ItemStack stack) {
        if (!stack.isDamageable()) return 1.0;
        return 1.0 - (double) stack.getDamage() / stack.getMaxDamage();
    }
}

