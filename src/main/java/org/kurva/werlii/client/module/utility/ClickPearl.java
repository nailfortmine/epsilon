package org.kurva.werlii.client.module.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class ClickPearl extends Module {
    private final BooleanSetting notificationsS;
    private final BooleanSetting silentS;
    private final BooleanSetting preferHotbarS;
    private final NumberSetting cooldownS;
    private final BooleanSetting checkCooldownS;
    
    private boolean wasMiddleClickDown = false;
    private long lastThrowTime = 0;
    private int originalSlot = -1;
    
    public ClickPearl() {
        super("ClickPearl", "Throw Ender Pearl with middle click", Category.UTILITY);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN); // No keybind, activated by mouse
        this.registerKeybinding("Werlii Utility");
        
        // Add settings
        notificationsS = new BooleanSetting("Notifications", "Show notifications when throwing pearls", this, true);
        silentS = new BooleanSetting("Silent", "Silently switch back to original item", this, true);
        preferHotbarS = new BooleanSetting("Prefer Hotbar", "Prefer pearls from hotbar over inventory", this, true);
        cooldownS = new NumberSetting("Cooldown", "Cooldown between throws (ms)", this, 500.0, 0.0, 5000.0, 100.0);
        checkCooldownS = new BooleanSetting("Check Cooldown", "Check server cooldown before throwing", this, true);
        
        addSetting(notificationsS);
        addSetting(silentS);
        addSetting(preferHotbarS);
        addSetting(cooldownS);
        addSetting(checkCooldownS);
        
        // Enable by default
        this.setEnabled(true);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        boolean middleClick = isMiddleClickPressed();
        
        // Check if middle mouse was just pressed
        if (middleClick && !wasMiddleClickDown) {
            // Check cooldown
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastThrowTime >= cooldownS.getValue()) {
                throwPearl();
                lastThrowTime = currentTime;
            } else if (notificationsS.getValue()) {
                // Notify about cooldown
                long remainingCooldown = (long) (cooldownS.getValue() - (currentTime - lastThrowTime));
                mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Pearl on cooldown: §c" + (remainingCooldown / 1000.0) + "s"), true);
            }
        }
        
        // Update middle click state
        wasMiddleClickDown = middleClick;
    }
    
    private boolean isMiddleClickPressed() {
        return GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
    }
    
    private void throwPearl() {
        // Check if player is in creative mode (can't throw pearls)
        if (mc.player.getAbilities().creativeMode) {
            if (notificationsS.getValue()) {
                mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cCannot throw pearls in creative mode"), true);
            }
            return;
        }
        
        // Check if player is already holding an ender pearl
        if (mc.player.getMainHandStack().getItem() instanceof EnderPearlItem) {
            // Just throw it
            throwHeldPearl();
            return;
        }
        
        // Find ender pearl in hotbar first
        int pearlSlot = -1;
        
        if (preferHotbarS.getValue()) {
            pearlSlot = findPearlInHotbar();
            
            // If not found in hotbar, check inventory
            if (pearlSlot == -1) {
                pearlSlot = findPearlInInventory();
            }
        } else {
            // Check entire inventory
            pearlSlot = findPearlInInventory();
        }
        
        if (pearlSlot == -1) {
            // No pearls found
            if (notificationsS.getValue()) {
                mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cNo Ender Pearls found in inventory"), true);
            }
            return;
        }
        
        // Save original slot
        originalSlot = mc.player.getInventory().selectedSlot;
        
        // Switch to pearl
        if (pearlSlot < 9) {
            // Pearl is in hotbar
            mc.player.getInventory().selectedSlot = pearlSlot;
            
            // Throw pearl
            throwHeldPearl();
            
            // Switch back if silent
            if (silentS.getValue()) {
                mc.player.getInventory().selectedSlot = originalSlot;
            }
        } else {
            // Pearl is in inventory, need to swap
            swapAndThrow(pearlSlot);
        }
    }
    
    private int findPearlInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }
        return -1;
    }
    
    private int findPearlInInventory() {
        // Check hotbar first (0-8)
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }
        
        // Check main inventory (9-35)
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }
        
        return -1;
    }
    
    private void throwHeldPearl() {
        // Check server cooldown if enabled
        if (checkCooldownS.getValue() && mc.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL)) {
            if (notificationsS.getValue()) {
                mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cEnder Pearl is on server cooldown"), true);
            }
            return;
        }
        
        // Get player look vector
        Vec3d lookVec = mc.player.getRotationVector();
        
        // Create hit result
        HitResult hitResult = new BlockHitResult(
            mc.player.getEyePos().add(lookVec.multiply(5.0)), 
            Direction.getFacing(lookVec.x, lookVec.y, lookVec.z),
            BlockPos.ofFloored(mc.player.getEyePos().add(lookVec.multiply(5.0))),
            false
        );
        
        // Use item (throw pearl)
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        
        if (notificationsS.getValue()) {
            mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Threw Ender Pearl"), true);
        }
    }
    
    private void swapAndThrow(int pearlSlot) {
        // This is more complex and requires packet manipulation or inventory clicks
        // For simplicity, we'll just notify that hotbar pearls are required
        
        if (notificationsS.getValue()) {
            mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cEnder Pearl found in inventory but not in hotbar"), true);
            mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cMove pearls to your hotbar for quick throwing"), true);
        }
        
        // A real implementation would use inventory clicks to swap items
        // This is complex and requires careful timing to avoid server detection
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        wasMiddleClickDown = false;
        lastThrowTime = 0;
    }
}

