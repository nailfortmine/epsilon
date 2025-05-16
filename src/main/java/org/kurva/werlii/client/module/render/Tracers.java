package org.kurva.werlii.client.module.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.kurva.werlii.client.util.EntityUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Tracers extends Module {
    private final BooleanSetting playersS;
    private final BooleanSetting friendsS;
    private final BooleanSetting hostileMobsS;
    private final BooleanSetting passiveMobsS;
    private final BooleanSetting itemsS;
    private final BooleanSetting crystalsS;
    private final NumberSetting widthS;
    private final NumberSetting opacityS;
    private final NumberSetting rangeS;
    private final ModeSetting colorModeS;
    private final BooleanSetting throughWallsS;
    private final ModeSetting originPointS;
    
    public Tracers() {
        super("Tracers", "Draws lines to entities", Category.RENDER);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Render");
        
        // Add settings
        playersS = new BooleanSetting("Players", "Draw tracers to players", this, true);
        friendsS = new BooleanSetting("Friends", "Draw tracers to friends", this, true);
        hostileMobsS = new BooleanSetting("Hostile Mobs", "Draw tracers to hostile mobs", this, false);
        passiveMobsS = new BooleanSetting("Passive Mobs", "Draw tracers to passive mobs", this, false);
        itemsS = new BooleanSetting("Items", "Draw tracers to dropped items", this, false);
        crystalsS = new BooleanSetting("Crystals", "Draw tracers to end crystals", this, true);
        widthS = new NumberSetting("Width", "Line width", this, 1.5, 0.1, 5.0, 0.1);
        opacityS = new NumberSetting("Opacity", "Line opacity", this, 0.8, 0.1, 1.0, 0.1);
        rangeS = new NumberSetting("Range", "Maximum tracer range", this, 100.0, 10.0, 500.0, 10.0);
        colorModeS = new ModeSetting("Color Mode", "How to color tracers", this, "Distance", "Distance", "Type", "Health", "Custom");
        throughWallsS = new BooleanSetting("Through Walls", "Draw tracers through walls", this, true);
        originPointS = new ModeSetting("Origin", "Where tracers start from", this, "Crosshair", "Crosshair", "Player", "Bottom");
        
        addSetting(playersS);
        addSetting(friendsS);
        addSetting(hostileMobsS);
        addSetting(passiveMobsS);
        addSetting(itemsS);
        addSetting(crystalsS);
        addSetting(widthS);
        addSetting(opacityS);
        addSetting(rangeS);
        addSetting(colorModeS);
        addSetting(throughWallsS);
        addSetting(originPointS);
    }
    
    @Override
    public void onRender(float tickDelta) {
        if (mc.player == null || mc.world == null) return;
        
        // Get origin point for tracers
        Vec3d origin = getOrigin();
        
        // Get entities to draw tracers to
        List<Entity> entities = getTargetEntities();
        
        // Draw tracers
        for (Entity entity : entities) {
            // Calculate entity position with interpolation
            Vec3d entityPos = getInterpolatedPosition(entity, tickDelta);
            
            // Get color based on settings
            int color = getEntityColor(entity);
            
            // Draw tracer
            drawLine(origin, entityPos, color, widthS.getValue().floatValue(), opacityS.getValue().floatValue());
        }
    }
    
    private List<Entity> getTargetEntities() {
        List<Entity> result = new ArrayList<>();
        double range = rangeS.getValue();
        
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            
            // Check if entity is within range
            if (mc.player.distanceTo(entity) > range) continue;
            
            // Filter entities based on settings
            if (entity instanceof PlayerEntity && playersS.getValue()) {
                // Check if player is a friend
                boolean isFriend = false; // Would be implemented with a friend system
                
                if (isFriend && !friendsS.getValue()) continue;
                
                result.add(entity);
            } else if (entity instanceof HostileEntity && hostileMobsS.getValue()) {
                result.add(entity);
            } else if (entity instanceof MobEntity && !(entity instanceof HostileEntity) && passiveMobsS.getValue()) {
                result.add(entity);
            } else if (entity instanceof ItemEntity && itemsS.getValue()) {
                result.add(entity);
            } else if (entity instanceof EndCrystalEntity && crystalsS.getValue()) {
                result.add(entity);
            }
        }
        
        return result;
    }
    
    private Vec3d getOrigin() {
        switch (originPointS.getValue()) {
            case "Crosshair":
                // Return position at the center of the screen
                return new Vec3d(0, 0, 1)
                    .rotateX(-(float) Math.toRadians(mc.player.getPitch()))
                    .rotateY(-(float) Math.toRadians(mc.player.getYaw()))
                    .add(mc.player.getEyePos());
            case "Player":
                // Return player's eye position
                return mc.player.getEyePos();
            case "Bottom":
                // Return bottom center of the screen
                return new Vec3d(mc.getWindow().getScaledWidth() / 2.0, mc.getWindow().getScaledHeight(), 0);
            default:
                return mc.player.getEyePos();
        }
    }
    
    private Vec3d getInterpolatedPosition(Entity entity, float tickDelta) {
        // Interpolate entity position between previous and current position
        double x = entity.prevX + (entity.getX() - entity.prevX) * tickDelta;
        double y = entity.prevY + (entity.getY() - entity.prevY) * tickDelta;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta;
        
        return new Vec3d(x, y, z);
    }
    
    private int getEntityColor(Entity entity) {
        switch (colorModeS.getValue()) {
            case "Distance":
                // Color based on distance - red (close) to blue (far)
                float distance = mc.player.distanceTo(entity);
                float maxDist = rangeS.getValue().floatValue();
                float ratio = Math.min(distance / maxDist, 1.0f);
                
                // Transition from red to blue
                int r = (int) (255 * (1 - ratio));
                int b = (int) (255 * ratio);
                int g = 0;
                
                return (r << 16) | (g << 8) | b;
                
            case "Type":
                // Color based on entity type
                if (entity instanceof PlayerEntity) {
                    boolean isFriend = false; // Would be implemented with a friend system
                    return isFriend ? 0x00FF00 : 0xFF0000; // Green for friends, red for other players
                } else if (entity instanceof HostileEntity) {
                    return 0xFF0000; // Red for hostile mobs
                } else if (entity instanceof MobEntity) {
                    return 0x00FF00; // Green for passive mobs
                } else if (entity instanceof ItemEntity) {
                    return 0xFFFF00; // Yellow for items
                } else if (entity instanceof EndCrystalEntity) {
                    return 0xFF00FF; // Purple for end crystals
                }
                return 0xFFFFFF; // White for anything else
                
            case "Health":
                // Color based on entity health if applicable
                if (entity instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) entity;
                    float health = player.getHealth() + player.getAbsorptionAmount();
                    float maxHealth = player.getMaxHealth() + player.getAbsorptionAmount();
                    float healthRatio = Math.min(health / maxHealth, 1.0f);
                    
                    // Transition from red (low health) to green (high health)

                    

                }
                return 0xFFFFFF; // White for non-living entities
                
            case "Custom":
                // Custom color based on entity type (more specific than "Type")
                if (entity instanceof PlayerEntity) {
                    boolean isFriend = false; // Would be implemented with a friend system
                    return isFriend ? 0x00FF00 : 0xFF0000; // Green for friends, red for other players
                } else if (entity instanceof HostileEntity) {
                    if (entity instanceof net.minecraft.entity.mob.CreeperEntity) {
                        return 0x00FF00; // Green for creepers
                    } else if (entity instanceof net.minecraft.entity.mob.SkeletonEntity) {
                        return 0xBBBBBB; // Light gray for skeletons
                    } else if (entity instanceof net.minecraft.entity.mob.ZombieEntity) {
                        return 0x003300; // Dark green for zombies
                    }
                    return 0xFF0000; // Red for other hostile mobs
                } else if (entity instanceof ItemEntity) {
                    // Color based on item rarity would be implemented here
                    return 0xFFFF00; // Yellow for items
                } else if (entity instanceof EndCrystalEntity) {
                    return 0xFF00FF; // Purple for end crystals
                }
                return 0xFFFFFF; // White for anything else
        }
        
        // Default color
        return 0xFFFFFF;
    }
    
    private void drawLine(Vec3d start, Vec3d end, int color, float width, float opacity) {
        // This would be implemented with proper rendering code in a real client
        // The implementation would draw a line from start to end with the specified color, width, and opacity
    }
}

