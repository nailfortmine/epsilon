package org.kurva.werlii.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.render.HUD;

public class HudManager {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    
    public void render(DrawContext context, float tickDelta) {
        HUD hudModule = (HUD) WerliiClient.getInstance().getModuleManager().getModuleByName("HUD");
        
        if (hudModule != null && hudModule.isEnabled()) {
            hudModule.render(context, tickDelta);
        }
    }
}

