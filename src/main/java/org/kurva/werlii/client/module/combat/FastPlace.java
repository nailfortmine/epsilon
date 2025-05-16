package org.kurva.werlii.client.module.combat;

import net.minecraft.item.BlockItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Items;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.kurva.werlii.mixin.MinecraftClientAccessor;
import org.lwjgl.glfw.GLFW;

public class FastPlace extends Module {
    private final NumberSetting delayS;
    private final BooleanSetting blocksS;
    private final BooleanSetting expBottlesS;
    private final BooleanSetting crystalsS;
    private final BooleanSetting enderPearlsS;
    
    public FastPlace() {
        super("FastPlace", "Reduces item placement delay", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        delayS = new NumberSetting("Delay", "Placement delay in ticks", this, 0.0, 0.0, 4.0, 1.0);
        blocksS = new BooleanSetting("Blocks", "Fast place blocks", this, true);
        expBottlesS = new BooleanSetting("Exp Bottles", "Fast throw experience bottles", this, true);
        crystalsS = new BooleanSetting("Crystals", "Fast place end crystals", this, true);
        enderPearlsS = new BooleanSetting("Ender Pearls", "Fast throw ender pearls", this, false);
        
        addSetting(delayS);
        addSetting(blocksS);
        addSetting(expBottlesS);
        addSetting(crystalsS);
        addSetting(enderPearlsS);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.interactionManager == null) return;
        
        if (shouldFastPlace()) {
            if (mc instanceof MinecraftClientAccessor) {
                ((MinecraftClientAccessor) mc).setRightClickDelayTimer(delayS.getValue().intValue());
            }
        }
    }
    
    private boolean shouldFastPlace() {
        if (mc.player.getMainHandStack().isEmpty()) return false;
        
        if (blocksS.getValue() && mc.player.getMainHandStack().getItem() instanceof BlockItem) {
            return true;
        }
        
        if (expBottlesS.getValue() && mc.player.getMainHandStack().getItem() instanceof ExperienceBottleItem) {
            return true;
        }
        
        if (crystalsS.getValue() && mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
            return true;
        }
        
        if (enderPearlsS.getValue() && mc.player.getMainHandStack().getItem() instanceof EnderPearlItem) {
            return true;
        }
        
        return false;
    }
}

