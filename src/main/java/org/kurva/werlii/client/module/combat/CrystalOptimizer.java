package org.kurva.werlii.client.module.combat;

import org.kurva.werlii.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class CrystalOptimizer extends Module {
    private boolean noBreakDelay = true;
    private boolean noPlaceDelay = true;
    private boolean instantExplode = true;
    
    public CrystalOptimizer() {
        super("CrystalOptimizer", "Optimizes crystal PvP by removing delays", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_H);
        this.registerKeybinding("Werlii Combat");
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
    }
    
    public boolean isNoBreakDelay() {
        return isEnabled() && noBreakDelay;
    }
    
    public boolean isNoPlaceDelay() {
        return isEnabled() && noPlaceDelay;
    }
    
    public boolean isInstantExplode() {
        return isEnabled() && instantExplode;
    }
}

