package org.kurva.werlii.client.module.utility;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class StreamMode extends Module {
    private final BooleanSetting hideHUDS;
    private final BooleanSetting hideVisualModulesS;
    private final BooleanSetting hideWatermarkS;
    private final BooleanSetting hideKeybindingsS;
    private final BooleanSetting hideNotificationsS;
    private final ModeSetting activationModeS;
    
    private boolean hudWasEnabled = false;
    private final List<Module> disabledVisualModules = new ArrayList<>();
    
    public StreamMode() {
        super("StreamMode", "Hides client elements when streaming", Category.UTILITY);
        this.setKeyCode(GLFW.GLFW_KEY_F12);
        this.registerKeybinding("Werlii Utility");
        
        hideHUDS = new BooleanSetting("Hide HUD", "Hide the client HUD", this, true);
        hideVisualModulesS = new BooleanSetting("Hide Visual Modules", "Disable visual modules", this, true);
        hideWatermarkS = new BooleanSetting("Hide Watermark", "Hide client watermark", this, true);
        hideKeybindingsS = new BooleanSetting("Hide Keybindings", "Disable keybinding display", this, true);
        hideNotificationsS = new BooleanSetting("Hide Notifications", "Hide client notifications", this, true);
        activationModeS = new ModeSetting("Activation", "How to activate stream mode", this, "Manual", "Manual", "Auto-Discord", "Auto-OBS");
        
        addSetting(hideHUDS);
        addSetting(hideVisualModulesS);
        addSetting(hideWatermarkS);
        addSetting(hideKeybindingsS);
        addSetting(hideNotificationsS);
        addSetting(activationModeS);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        if (mc.player == null) return;
        
        Module hudModule = WerliiClient.getInstance().getModuleManager().getModuleByName("HUD");
        if (hudModule != null) {
            hudWasEnabled = hudModule.isEnabled();
            
            if (hideHUDS.getValue() && hudModule.isEnabled()) {
                hudModule.setEnabled(false);
            }
        }
        
        if (hideVisualModulesS.getValue()) {
            disabledVisualModules.clear();
            
            for (Module module : WerliiClient.getInstance().getModuleManager().getModules()) {
                if (module != this && module.isEnabled() && isVisualModule(module)) {
                    disabledVisualModules.add(module);
                    module.setEnabled(false);
                }
            }
        }
        
        if (!hideNotificationsS.getValue()) {
            mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Stream Mode activated"), true);
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        if (mc.player == null) return;
        
        Module hudModule = WerliiClient.getInstance().getModuleManager().getModuleByName("HUD");
        if (hudModule != null && hudWasEnabled && hideHUDS.getValue()) {
            hudModule.setEnabled(true);
        }
        
        if (hideVisualModulesS.getValue()) {
            for (Module module : disabledVisualModules) {
                if (!module.isEnabled()) {
                    module.setEnabled(true);
                }
            }
            disabledVisualModules.clear();
        }
        
        if (!hideNotificationsS.getValue()) {
            mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Stream Mode deactivated"), true);
        }
    }
    
    @Override
    public void onTick() {
        if (!isEnabled() && !activationModeS.getValue().equals("Manual")) {
            if (isStreamingActive()) {
                setEnabled(true);
            }
        } else if (isEnabled() && !activationModeS.getValue().equals("Manual")) {
            if (!isStreamingActive()) {
                setEnabled(false);
            }
        }
    }
    
    private boolean isVisualModule(Module module) {
        if (module.getCategory() == Category.RENDER) {
            return true;
        }
        
        String name = module.getName().toLowerCase();
        return name.contains("esp") || 
               name.contains("tracer") || 
               name.contains("waypoint") || 
               name.contains("nametag") || 
               name.contains("chams") ||
               name.contains("highlight");
    }
    
    private boolean isStreamingActive() {
        return false;
    }
    
    public boolean shouldHideWatermark() {
        return isEnabled() && hideWatermarkS.getValue();
    }
    
    public boolean shouldHideKeybindings() {
        return isEnabled() && hideKeybindingsS.getValue();
    }
    
    public boolean shouldHideNotifications() {
        return isEnabled() && hideNotificationsS.getValue();
    }
}

