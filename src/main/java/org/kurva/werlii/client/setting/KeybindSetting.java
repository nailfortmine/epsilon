package org.kurva.werlii.client.setting;

import org.kurva.werlii.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class KeybindSetting extends Setting<Integer> {
    private boolean listening = false;
    
    public KeybindSetting(String name, Module parent, int defaultValue) {
        super(name, "The key to toggle this module", parent, defaultValue);
    }
    
    public boolean isListening() {
        return listening;
    }
    
    public void setListening(boolean listening) {
        this.listening = listening;
    }
    
    public String getKeyName() {
        return getKeyName(getValue());
    }
    
    public static String getKeyName(int keyCode) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_UNKNOWN: return "None";
            case GLFW.GLFW_KEY_ESCAPE: return "Escape";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "Right Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "Left Control";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "Right Control";
            case GLFW.GLFW_KEY_LEFT_ALT: return "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "Right Alt";
            case GLFW.GLFW_KEY_BACKSPACE: return "Backspace";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            case GLFW.GLFW_KEY_SPACE: return "Space";
            default:
                String keyName = GLFW.glfwGetKeyName(keyCode, 0);
                if (keyName != null) {
                    return keyName.toUpperCase();
                } else if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) {
                    return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
                } else {
                    return "Key " + keyCode;
                }
        }
    }
}

