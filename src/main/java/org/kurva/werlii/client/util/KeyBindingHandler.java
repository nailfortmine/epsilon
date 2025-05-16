package org.kurva.werlii.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.module.utility.PanicMode;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class KeyBindingHandler {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<Integer, Boolean> keyStates = new HashMap<>();
    
    public void handleKeyBindings() {
        if (mc.player == null || mc.world == null) return;
    
        if (mc.currentScreen != null) return;
    
        boolean isDeletePressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_DELETE);
        Boolean wasDeletePressed = keyStates.getOrDefault(GLFW.GLFW_KEY_DELETE, false);
    
        if (isDeletePressed && !wasDeletePressed) {
            PanicMode.trigger();
        }
    
        keyStates.put(GLFW.GLFW_KEY_DELETE, isDeletePressed);
    
        for (Module module : WerliiClient.getInstance().getModuleManager().getModules()) {
            int keyCode = module.getKeyCode();
        
            if (keyCode == GLFW.GLFW_KEY_UNKNOWN || keyCode == GLFW.GLFW_KEY_DELETE) continue;
        
            boolean isPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyCode);
        
            Boolean wasPressed = keyStates.getOrDefault(keyCode, false);
        
            if (isPressed && !wasPressed) {
                module.toggle();
            }
        
            keyStates.put(keyCode, isPressed);
        }
    }
    
    public void registerKeybindings() {
    }
}

