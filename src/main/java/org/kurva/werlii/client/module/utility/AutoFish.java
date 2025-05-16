package org.kurva.werlii.client.module.utility;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class AutoFish extends Module {
    private final NumberSetting catchDelayS;
    private final NumberSetting recastDelayS;
    private final BooleanSetting autoSwitchS;
    private final BooleanSetting preventBreakS;
    private final BooleanSetting antiAfkS;
    private final NumberSetting antiAfkIntervalS;
    
    private int catchDelay = 0;
    private int recastDelay = 0;
    private int antiAfkTimer = 0;
    private boolean isFishing = false;
    private boolean soundDetected = false;
    private int originalSlot = -1;
    
    public AutoFish() {
        super("AutoFish", "Automatically catches fish", Category.UTILITY);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Utility");
        
        catchDelayS = new NumberSetting("Catch Delay", "Delay before catching fish (ms)", this, 300.0, 0.0, 1000.0, 50.0);
        recastDelayS = new NumberSetting("Recast Delay", "Delay before recasting rod (ms)", this, 500.0, 0.0, 2000.0, 50.0);
        autoSwitchS = new BooleanSetting("Auto Switch", "Automatically switch to fishing rod", this, true);
        preventBreakS = new BooleanSetting("Prevent Break", "Stop when rod is about to break", this, true);
        antiAfkS = new BooleanSetting("Anti-AFK", "Move occasionally to prevent AFK kick", this, true);
        antiAfkIntervalS = new NumberSetting("Anti-AFK Interval", "Seconds between anti-AFK movements", this, 60.0, 10.0, 300.0, 10.0);
        
        addSetting(catchDelayS);
        addSetting(recastDelayS);
        addSetting(autoSwitchS);
        addSetting(preventBreakS);
        addSetting(antiAfkS);
        addSetting(antiAfkIntervalS);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        catchDelay = 0;
        recastDelay = 0;
        antiAfkTimer = 0;
        isFishing = false;
        soundDetected = false;
        originalSlot = -1;
        
        if (mc.player != null && !(mc.player.getMainHandStack().getItem() instanceof FishingRodItem)) {
            if (autoSwitchS.getValue()) {
                int rodSlot = findFishingRod();
                if (rodSlot != -1) {
                    originalSlot = mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = rodSlot;
                    mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Switched to fishing rod"), true);
                } else {
                    mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cNo fishing rod found in hotbar!"), true);
                    setEnabled(false);
                    return;
                }
            } else {
                mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cYou need to hold a fishing rod!"), true);
                setEnabled(false);
                return;
            }
        }
        
        castRod();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        if (originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = originalSlot;
            originalSlot = -1;
        }
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        if (!(mc.player.getMainHandStack().getItem() instanceof FishingRodItem)) {
            if (autoSwitchS.getValue()) {
                int rodSlot = findFishingRod();
                if (rodSlot != -1) {
                    originalSlot = mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = rodSlot;
                } else {
                    mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cNo fishing rod found in hotbar!"), true);
                    setEnabled(false);
                    return;
                }
            } else {
                mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cYou need to hold a fishing rod!"), true);
                setEnabled(false);
                return;
            }
        }
        
        if (preventBreakS.getValue() && mc.player.getMainHandStack().getDamage() >= mc.player.getMainHandStack().getMaxDamage() - 5) {
            mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cFishing rod is about to break! Disabling AutoFish."), true);
            setEnabled(false);
            return;
        }
        
        if (soundDetected) {
            if (catchDelay > 0) {
                catchDelay--;
            } else {
                useRod();
                soundDetected = false;
                isFishing = false;
                recastDelay = (int)(recastDelayS.getValue() / 50);
            }
        } else if (!isFishing) {
            if (recastDelay > 0) {
                recastDelay--;
            } else {
                castRod();
            }
        }
        
        if (antiAfkS.getValue()) {
            antiAfkTimer++;
            int interval = (int)(antiAfkIntervalS.getValue() * 20);
            
            if (antiAfkTimer >= interval) {
                performAntiAfkMovement();
                antiAfkTimer = 0;
            }
        }
    }
    
    public void onSoundPlay(SoundInstance sound) {
        if (!isEnabled() || mc.player == null) return;
        
        if (sound.getId().equals(SoundEvents.ENTITY_FISHING_BOBBER_SPLASH.getId())) {
            soundDetected = true;
            catchDelay = (int)(catchDelayS.getValue() / 50);
        }
    }
    
    private int findFishingRod() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof FishingRodItem) {
                return i;
            }
        }
        return -1;
    }
    
    private void castRod() {
        useRod();
        isFishing = true;
    }
    
    private void useRod() {
        if (mc.player != null) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
    
    private void performAntiAfkMovement() {
        if (mc.player == null) return;
        
        mc.player.setYaw(mc.player.getYaw() + 180);
        
        if (Math.random() < 0.3) {
            mc.player.jump();
        }
        
        mc.player.setYaw(mc.player.getYaw() + 180);
    }
}

