package org.kurva.werlii.client.ui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.render.HUD;

public class HudRenderer implements HudRenderCallback {
  private final MinecraftClient mc = MinecraftClient.getInstance();
  
  @Override
  public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
      if (mc.player == null || mc.world == null) return;
      
      HUD hudModule = (HUD) WerliiClient.getInstance().getModuleManager().getModuleByName("HUD");
      
      if (hudModule != null && hudModule.isEnabled()) {
          // Use a default tick delta value since we can't access it directly
          float tickDelta = 1.0f;
          
          // Try to access the field directly if possible
          try {
              // This is a fallback approach - in production code you would use proper access transformers
              java.lang.reflect.Field field = RenderTickCounter.class.getDeclaredField("tickDelta");
              field.setAccessible(true);
              tickDelta = field.getFloat(tickCounter);
          } catch (Exception e) {
              // If we can't access the field, just use the default value
          }
          
          hudModule.render(context, tickDelta);
      }
  }
}

