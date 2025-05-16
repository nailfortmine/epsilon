package org.kurva.werlii.client.module.render;

import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Trajectories extends Module {
    private final ModeSetting renderModeS;
    private final BooleanSetting arrowsS;
    private final BooleanSetting pearlsS;
    private final BooleanSetting snowballsS;
    private final BooleanSetting eggsS;
    private final BooleanSetting potionsS;
    private final BooleanSetting fishingRodS;
    private final BooleanSetting showHitPositionS;
    private final BooleanSetting showHitBlockS;
    private final NumberSetting lineWidthS;
    private final NumberSetting simulationStepsS;
    
    public Trajectories() {
        super("Trajectories", "Shows the trajectory of projectiles", Category.RENDER);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Render");
        
        // Add settings
        renderModeS = new ModeSetting("Render Mode", "How to render trajectories", this, "Line", "Line", "Dots", "Both");
        arrowsS = new BooleanSetting("Arrows", "Show arrow trajectories", this, true);
        pearlsS = new BooleanSetting("Ender Pearls", "Show ender pearl trajectories", this, true);
        snowballsS = new BooleanSetting("Snowballs", "Show snowball trajectories", this, true);
        eggsS = new BooleanSetting("Eggs", "Show egg trajectories", this, true);
        potionsS = new BooleanSetting("Potions", "Show potion trajectories", this, true);
        fishingRodS = new BooleanSetting("Fishing Rod", "Show fishing rod trajectories", this, true);
        showHitPositionS = new BooleanSetting("Show Hit Position", "Show where projectile will land", this, true);
        showHitBlockS = new BooleanSetting("Show Hit Block", "Highlight block that will be hit", this, true);
        lineWidthS = new NumberSetting("Line Width", "Width of trajectory line", this, 2.0, 1.0, 5.0, 0.5);
        simulationStepsS = new NumberSetting("Simulation Steps", "Number of steps to simulate", this, 100.0, 10.0, 500.0, 10.0);
        
        addSetting(renderModeS);
        addSetting(arrowsS);
        addSetting(pearlsS);
        addSetting(snowballsS);
        addSetting(eggsS);
        addSetting(potionsS);
        addSetting(fishingRodS);
        addSetting(showHitPositionS);
        addSetting(showHitBlockS);
        addSetting(lineWidthS);
        addSetting(simulationStepsS);
    }
    
    @Override
    public void onRender(float tickDelta) {
        if (mc.player == null || mc.world == null) return;
        
        // Get held item
        ItemStack heldItem = mc.player.getMainHandStack();
        
        // Check if item is a projectile
        if (!isProjectile(heldItem)) return;
        
        // Calculate trajectory
        List<Vec3d> trajectory = calculateTrajectory(heldItem);
        
        // Render trajectory
        renderTrajectory(trajectory, getTrajectoryColor(heldItem));
    }
    
    private boolean isProjectile(ItemStack stack) {
        Item item = stack.getItem();
        
        if (item instanceof BowItem && arrowsS.getValue()) return true;
        if (item instanceof EnderPearlItem && pearlsS.getValue()) return true;
        if (item instanceof SnowballItem && snowballsS.getValue()) return true;
        if (item instanceof EggItem && eggsS.getValue()) return true;
        if (item instanceof PotionItem && potionsS.getValue()) return true;
        if (item instanceof FishingRodItem && fishingRodS.getValue()) return true;
        if (item instanceof CrossbowItem && arrowsS.getValue()) return true;
        
        return false;
    }
    
    private List<Vec3d> calculateTrajectory(ItemStack stack) {
        List<Vec3d> trajectory = new ArrayList<>();
        
        // Get player position and rotation
        Vec3d startPos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVector();
        
        // Add start position
        trajectory.add(startPos);
        
        // Calculate velocity
        double velocity = getInitialVelocity(stack);
        Vec3d motion = lookVec.multiply(velocity);
        
        // Calculate gravity
        double gravity = getGravity(stack);
        
        // Simulate trajectory
        Vec3d pos = startPos;
        int maxSteps = simulationStepsS.getValue().intValue();
        
        for (int i = 0; i < maxSteps; i++) {
            // Calculate next position
            pos = pos.add(motion);
            trajectory.add(pos);
            
            // Apply gravity
            motion = motion.add(0, gravity, 0);
            
            // Check for collision
            BlockHitResult hitResult = mc.world.raycast(new RaycastContext(
                pos, pos.add(motion), 
                RaycastContext.ShapeType.COLLIDER, 
                RaycastContext.FluidHandling.NONE, 
                mc.player
            ));
            
            if (hitResult.getType() != HitResult.Type.MISS) {
                // Add hit position
                trajectory.add(hitResult.getPos());
                break;
            }
        }
        
        return trajectory;
    }
    
    private double getInitialVelocity(ItemStack stack) {
        Item item = stack.getItem();
        
        if (item instanceof BowItem) {
            // Calculate bow charge
            int useTicks = mc.player.getItemUseTime();
            float charge = BowItem.getPullProgress(useTicks);
            return charge * 3.0;
        } else if (item instanceof CrossbowItem) {
            return 3.15; // Fully charged crossbow
        } else if (item instanceof FishingRodItem) {
            return 1.5;
        } else {
            // Default for other projectiles
            return 1.5;
        }
    }
    
    private double getGravity(ItemStack stack) {
        Item item = stack.getItem();
        
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            return -0.05;
        } else if (item instanceof PotionItem) {
            return -0.05;
        } else if (item instanceof FishingRodItem) {
            return -0.03;
        } else {
            // Default for other projectiles
            return -0.03;
        }
    }
    
    private int getTrajectoryColor(ItemStack stack) {
        Item item = stack.getItem();
        
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            return 0xFF00FF00; // Green
        } else if (item instanceof EnderPearlItem) {
            return 0xFF9900FF; // Purple
        } else if (item instanceof PotionItem) {
            // Get potion color
            return 0xFF0000FF; // Blue (default)
        } else if (item instanceof FishingRodItem) {
            return 0xFF00FFFF; // Cyan
        } else {
            // Default for other projectiles
            return 0xFFFFFFFF; // White
        }
    }
    
    private void renderTrajectory(List<Vec3d> trajectory, int color) {
        // This would be implemented with proper rendering code in a real client
        // The implementation would render the trajectory as a line, dots, or both based on settings
        
        // Render line
        if (renderModeS.getValue().equals("Line") || renderModeS.getValue().equals("Both")) {
            drawTrajectoryLine(trajectory, color, lineWidthS.getValue().floatValue());
        }
        
        // Render dots
        if (renderModeS.getValue().equals("Dots") || renderModeS.getValue().equals("Both")) {
            drawTrajectoryDots(trajectory, color);
        }
        
        // Render hit position
        if (showHitPositionS.getValue() && !trajectory.isEmpty()) {
            Vec3d hitPos = trajectory.get(trajectory.size() - 1);
            drawHitPosition(hitPos, color);
        }
        
        // Render hit block
        if (showHitBlockS.getValue() && !trajectory.isEmpty()) {
            Vec3d hitPos = trajectory.get(trajectory.size() - 1);
            drawHitBlock(hitPos, color);
        }
    }
    
    private void drawTrajectoryLine(List<Vec3d> trajectory, int color, float width) {
        // This would be implemented with proper rendering code
    }
    
    private void drawTrajectoryDots(List<Vec3d> trajectory, int color) {
        // This would be implemented with proper rendering code
    }
    
    private void drawHitPosition(Vec3d pos, int color) {
        // This would be implemented with proper rendering code
    }
    
    private void drawHitBlock(Vec3d pos, int color) {
        // This would be implemented with proper rendering code
    }
}

