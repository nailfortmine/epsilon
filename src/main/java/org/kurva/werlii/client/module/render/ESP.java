package org.kurva.werlii.client.module.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class ESP extends Module {
    private boolean drawPlayers = true;
    private boolean drawMobs = false;
    private boolean drawItems = false;
    private boolean drawCrystals = true;
    private int playerColor = 0xFF0000;
    private int mobColor = 0x00FF00;
    private int itemColor = 0x0000FF;
    private int crystalColor = 0xFFFF00;
    
    public ESP() {
        super("ESP", "Draws boxes around entities", Category.RENDER);
        this.setKeyCode(GLFW.GLFW_KEY_O);
        this.registerKeybinding("Werlii Render");
    }
    
    public void render(MatrixStack matrices, float tickDelta) {
        if (mc.player == null || mc.world == null) return;
        
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            
            int color = 0;
            boolean shouldRender = false;
            
            if (entity instanceof PlayerEntity && drawPlayers) {
                color = playerColor;
                shouldRender = true;
            } else if (entity instanceof net.minecraft.entity.decoration.EndCrystalEntity && drawCrystals) {
                color = crystalColor;
                shouldRender = true;
            } else if (entity instanceof net.minecraft.entity.ItemEntity && drawItems) {
                color = itemColor;
                shouldRender = true;
            } else if (entity instanceof net.minecraft.entity.mob.MobEntity && drawMobs) {
                color = mobColor;
                shouldRender = true;
            }
            
            if (shouldRender) {
                drawEntityBox(matrices, entity, color, tickDelta);
            }
        }
    }
    
    private void drawEntityBox(MatrixStack matrices, Entity entity, int color, float tickDelta) {
        double x = entity.prevX + (entity.getX() - entity.prevX) * tickDelta;
        double y = entity.prevY + (entity.getY() - entity.prevY) * tickDelta;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta;
        
        Box box = entity.getBoundingBox().offset(
            x - entity.getX(),
            y - entity.getY(),
            z - entity.getZ()
        );
        
        drawBoxOutline(matrices, box, color);
    }
    
    private void drawBoxOutline(MatrixStack matrices, Box box, int color) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        if (alpha == 0) alpha = 1.0F;
        
        if (mc != null && mc.player != null) {
            mc.player.sendMessage(Text.literal("Drawing ESP box for entity"), false);
        }
    }
}

