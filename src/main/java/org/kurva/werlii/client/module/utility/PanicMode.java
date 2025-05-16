package org.kurva.werlii.client.module.utility;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class PanicMode extends Module {
    private final BooleanSetting disableAllModulesS;
    private final BooleanSetting hideHUDS;
    private final BooleanSetting restoreOnToggleS;
    private final BooleanSetting closeScreenS;
    
    private final List<Module> previouslyEnabled = new ArrayList<>();
    private boolean hudWasEnabled = false;
    
    public PanicMode() {
        super("PanicMode", "Quickly hide all client features", Category.UTILITY);
        this.setKeyCode(GLFW.GLFW_KEY_DELETE);
        this.registerKeybinding("Werlii Utility");
        
        disableAllModulesS = new BooleanSetting("Disable Modules", "Disable all active modules", this, true);
        hideHUDS = new BooleanSetting("Hide HUD", "Hide the client HUD", this, true);
        restoreOnToggleS = new BooleanSetting("Restore On Toggle", "Restore previous state when toggled again", this, false);
        closeScreenS = new BooleanSetting("Close Screen", "Close any open screens", this, true);
        
        addSetting(disableAllModulesS);
        addSetting(hideHUDS);
        addSetting(restoreOnToggleS);
        addSetting(closeScreenS);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        if (mc.player == null) return;
        
        Module hudModule = WerliiClient.getInstance().getModuleManager().getModuleByName("HUD");
        if (hudModule != null) {
            hudWasEnabled = hudModule.isEnabled();
        }
        
        previouslyEnabled.clear();
        for (Module module : WerliiClient.getInstance().getModuleManager().getModules()) {
            if (module != this && module.isEnabled()) {
                previouslyEnabled.add(module);
            }
        }
        
        if (disableAllModulesS.getValue()) {
            for (Module module : WerliiClient.getInstance().getModuleManager().getModules()) {
                if (module != this && module.isEnabled()) {
                    module.setEnabled(false);
                }
            }
        }
        
        if (hideHUDS.getValue() && hudModule != null && hudModule.isEnabled()) {
            hudModule.setEnabled(false);
        }
        
        if (closeScreenS.getValue() && mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen)) {
            mc.setScreen(null);
        }
        
        mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Panic mode activated"), true);
        
        if (!restoreOnToggleS.getValue()) {
            setEnabled(false);
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        if (mc.player == null) return;
        
        if (restoreOnToggleS.getValue()) {
            Module hudModule = WerliiClient.getInstance().getModuleManager().getModuleByName("HUD");
            if (hudModule != null && hudWasEnabled) {
                hudModule.setEnabled(true);
            }
            
            for (Module module : previouslyEnabled) {
                if (!module.isEnabled()) {
                    module.setEnabled(true);
                }
            }
            
            mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Panic mode deactivated, modules restored"), true);
        }
    }
    
    public static void trigger() {
        PanicMode panicModule = (PanicMode) WerliiClient.getInstance()
            .getModuleManager().getModuleByName("PanicMode");
        
        if (panicModule != null && !panicModule.isEnabled()) {
            panicModule.setEnabled(true);
        }
    }
}

