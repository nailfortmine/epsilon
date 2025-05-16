package org.kurva.werlii.client.module.render;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.ui.screens.ClickGUIScreen;
import org.lwjgl.glfw.GLFW;

public class ClickGUI extends Module {
    private ClickGUIScreen guiScreen;
    
    public ClickGUI() {
        super("ClickGUI", "Opens the click GUI", Category.RENDER);
        this.setKeyCode(GLFW.GLFW_KEY_RIGHT_SHIFT);
        this.registerKeybinding("Werlii Render");
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        if (mc.player != null) {
            if (guiScreen == null) {
                guiScreen = new ClickGUIScreen(Text.literal("Werlii ClickGUI"));
            }
            
            mc.setScreen(guiScreen);
        }
        
        setEnabled(false);
    }
    
    public void openGUI() {
        if (mc.player != null) {
            if (guiScreen == null) {
                guiScreen = new ClickGUIScreen(Text.literal("Werlii ClickGUI"));
            }
            
            mc.setScreen(guiScreen);
        }
    }
}

