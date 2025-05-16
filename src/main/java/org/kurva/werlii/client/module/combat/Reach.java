package org.kurva.werlii.client.module.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Reach extends Module {
    private final NumberSetting reachS;
    
    public Reach() {
        super("Reach", "Increases attack reach distance", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        // Add settings
        reachS = new NumberSetting("Distance", "Reach distance in blocks", this, 3.5, 3.0, 6.0, 0.1);
        
        addSetting(reachS);
    }
    
    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.world == null) return;
        
        // Only run this code when the player is trying to attack
        if (mc.options.attackKey.isPressed() && mc.crosshairTarget != null) {
            if (mc.crosshairTarget.getType() == HitResult.Type.MISS || 
                (mc.crosshairTarget.getType() == HitResult.Type.ENTITY && 
                 mc.player.distanceTo(((EntityHitResult)mc.crosshairTarget).getEntity()) > 3.0)) {
                
                // Get the player's look vector
                Vec3d cameraPos = mc.player.getCameraPosVec(1.0f);
                Vec3d lookVec = mc.player.getRotationVec(1.0f);
                
                // Calculate the reach distance
                double reachDistance = getReachDistance();
                
                // Calculate the end point of the ray
                Vec3d endPos = cameraPos.add(lookVec.multiply(reachDistance));
                
                // Find entities in the path
                List<Entity> entities = new ArrayList<>();
                for (Entity entity : mc.world.getEntities()) {
                    if (entity != mc.player && entity.isAlive() && entity instanceof LivingEntity) {
                        Box box = entity.getBoundingBox().expand(0.1); // Slightly expand hitbox
                        
                        // Check if the ray intersects the entity's bounding box
                        if (box.raycast(cameraPos, endPos).isPresent()) {
                            double distance = cameraPos.distanceTo(entity.getPos());
                            if (distance <= reachDistance) {
                                entities.add(entity);
                            }
                        }
                    }
                }
                
                // Sort entities by distance
                entities.sort(Comparator.comparingDouble(mc.player::distanceTo));
                
                // Attack the closest entity
                if (!entities.isEmpty()) {
                    Entity target = entities.get(0);
                    
                    // Attack the entity
                    mc.interactionManager.attackEntity(mc.player, target);
                    mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                }
            }
        }
    }
    
    public double getReachDistance() {
        return isEnabled() ? reachS.getValue() : 3.0;
    }
}

