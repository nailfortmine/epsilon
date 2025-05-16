package org.kurva.werlii.client.module.utility;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class AutoRespawn extends Module {
    private final NumberSetting delayS;
    private final BooleanSetting antiDeathScreenS;
    private final BooleanSetting autoKitS;
    private final BooleanSetting notificationsS;
    
    private int respawnTimer = 0;
    private boolean waitingToRespawn = false;
    
    public AutoRespawn() {
        super("AutoRespawn", "Automatically respawn after death", Category.UTILITY);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Utility");
        
        // Add settings
        delayS = new NumberSetting("Delay", "Delay before respawning (ms)", this, 500.0, 0.0, 10000.0, 100.0);
        antiDeathScreenS = new BooleanSetting("Anti Death Screen", "Skip death screen entirely", this, true);
        autoKitS = new BooleanSetting("Auto Kit", "Automatically select kit after respawn", this, false);
        notificationsS = new BooleanSetting("Notifications", "Show death notifications", this, true);
        
        addSetting(delayS);
        addSetting(antiDeathScreenS);
        addSetting(autoKitS);
        addSetting(notificationsS);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        // Check if player is on death screen
        if (mc.currentScreen instanceof DeathScreen) {
            if (antiDeathScreenS.getValue()) {
                // Close death screen immediately
                mc.setScreen(null);
            }
            
            if (!waitingToRespawn) {
                waitingToRespawn = true;
                respawnTimer = (int) (delayS.getValue() / 50); // Convert ms to ticks
                
                if (notificationsS.getValue()) {
                    String deathMessage = ((DeathScreen) mc.currentScreen).getTitle().getString();
                    mc.inGameHud.getChatHud().addMessage(Text.literal("§8[§bWerlii§8] §7" + deathMessage));
                }
            }
        }
        
        if (waitingToRespawn) {
            if (respawnTimer <= 0) {
                respawn();
                waitingToRespawn = false;
            } else {
                respawnTimer--;
            }
        }
    }
    
    private void respawn() {
        if (mc.player == null) return;
        
        // Send respawn packet
        mc.player.requestRespawn();
        
        if (notificationsS.getValue()) {
            mc.inGameHud.getChatHud().addMessage(Text.literal("§8[§bWerlii§8] §7Automatically respawned"));
        }
        
        // Handle auto kit if enabled
        if (autoKitS.getValue()) {
            // This would need to be implemented for the specific server
            // Most servers use commands like /kit to select kits
            // We'd need to add a delay and then send the command
            runAutoKitCommand();
        }
    }
    
    private void runAutoKitCommand() {
        // Schedule kit command with a delay
        new Thread(() -> {
            try {
                // Wait 1 second before sending kit command
                Thread.sleep(1000);
                
                if (mc.player != null) {
                    mc.execute(() -> {
                        
                        if (notificationsS.getValue()) {
                            mc.inGameHud.getChatHud().addMessage(Text.literal("§8[§bWerlii§8] §7Auto kit command executed"));
                        }
                    });
                }
            } catch (InterruptedException e) {
                // Ignore
            }
        }).start();
    }
}

