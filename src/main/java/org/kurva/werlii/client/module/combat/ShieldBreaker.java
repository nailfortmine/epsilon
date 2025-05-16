package org.kurva.werlii.client.module.combat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ShieldBreaker extends Module {
    private final NumberSetting rangeS;
    private final BooleanSetting autoSwitchS;
    private final BooleanSetting rotateS;
    private final BooleanSetting onlyAxeS;
    private final NumberSetting delayS;
    
    private int breakDelay = 0;
    private int originalSlot = -1;
    
    public ShieldBreaker() {
        super("ShieldBreaker", "Automatically breaks enemy shields", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        // Add settings
        rangeS = new NumberSetting("Range", "Maximum range to break shields", this, 4.0, 1.0, 6.0, 0.1);
        autoSwitchS = new BooleanSetting("Auto Switch", "Automatically switch to axe", this, true);
        rotateS = new BooleanSetting("Rotate", "Rotate to face target", this, false);
        onlyAxeS = new BooleanSetting("Only Axe", "Only break shields with axes", this, true);
        delayS = new NumberSetting("Delay", "Delay between breaks (ticks)", this, 5.0, 0.0, 20.0, 1.0);
        
        addSetting(rangeS);
        addSetting(autoSwitchS);
        addSetting(rotateS);
        addSetting(onlyAxeS);
        addSetting(delayS);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        // Handle delay
        if (breakDelay > 0) {
            breakDelay--;
            return;
        }
        
        // Find players using shields
        List<PlayerEntity> shieldUsers = findShieldUsers();
        
        if (!shieldUsers.isEmpty()) {
            PlayerEntity target = shieldUsers.get(0);
            
            // Switch to axe if needed
            if (autoSwitchS.getValue()) {
                originalSlot = mc.player.getInventory().selectedSlot;
                int axeSlot = findAxeSlot();
                
                if (axeSlot != -1) {
                    mc.player.getInventory().selectedSlot = axeSlot;
                } else if (onlyAxeS.getValue()) {
                    // No axe found and only axe is enabled
                    return;
                }
            } else if (onlyAxeS.getValue() && !(mc.player.getMainHandStack().getItem() instanceof AxeItem)) {
                // Not holding axe and auto switch is disabled
                return;
            }
            
            // Rotate to target if enabled
            if (rotateS.getValue()) {
                rotateToTarget(target);
            }
            
            // Attack the target to break shield
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            
            // Switch back to original slot
            if (autoSwitchS.getValue() && originalSlot != -1) {
                mc.player.getInventory().selectedSlot = originalSlot;
                originalSlot = -1;
            }
            
            breakDelay = delayS.getValue().intValue();
        }
    }
    
    private List<PlayerEntity> findShieldUsers() {
        return mc.world.getPlayers().stream()
                .filter(player -> player != mc.player)
                .filter(player -> player.isAlive() && !player.isSpectator())
                .filter(player -> mc.player.distanceTo(player) <= rangeS.getValue())
                .filter(this::isUsingShield)
                .sorted(Comparator.comparingDouble(mc.player::distanceTo))
                .collect(Collectors.toList());
    }
    
    private boolean isUsingShield(PlayerEntity player) {
        // Check if player is using shield
        return player.isUsingItem() && 
               (player.getActiveItem().getItem() == Items.SHIELD);
    }
    
    private int findAxeSlot() {
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (item instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }
    
    private void rotateToTarget(PlayerEntity target) {
        double x = target.getX() - mc.player.getX();
        double z = target.getZ() - mc.player.getZ();
        double y = target.getEyeY() - mc.player.getEyeY();
        
        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, dist));
        
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }
}

