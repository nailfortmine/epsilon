package org.kurva.werlii.client.module.movement;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class Jesus extends Module {
    private final ModeSetting modeS;
    private final NumberSetting speedS;
    private final BooleanSetting solidS;
    private final BooleanSetting bypassS;
    private final BooleanSetting noSinkS;
    
    private boolean isOnLiquid = false;
    private int ticksOnLiquid = 0;
    
    public Jesus() {
        super("Jesus", "Allows you to walk on liquids", Category.MOVEMENT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Movement");
        
        modeS = new ModeSetting("Mode", "Walking method", this, "Solid", "Solid", "Dolphin", "Bounce");
        speedS = new NumberSetting("Speed", "Movement speed on liquids", this, 1.0, 0.1, 2.0, 0.1);
        solidS = new BooleanSetting("Solid", "Make liquids solid", this, true);
        bypassS = new BooleanSetting("Bypass", "Use bypass techniques", this, true);
        noSinkS = new BooleanSetting("No Sink", "Prevent sinking in liquids", this, true);
        
        addSetting(modeS);
        addSetting(speedS);
        addSetting(solidS);
        addSetting(bypassS);
        addSetting(noSinkS);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        BlockPos playerPos = mc.player.getBlockPos();
        boolean onLiquid = isAboveLiquid(playerPos);
        
        if (onLiquid) {
            ticksOnLiquid++;
            isOnLiquid = true;
            
            switch (modeS.getValue()) {
                case "Solid":
                    handleSolidMode();
                    break;
                case "Dolphin":
                    handleDolphinMode();
                    break;
                case "Bounce":
                    handleBounceMode();
                    break;
            }
        } else {
            ticksOnLiquid = 0;
            isOnLiquid = false;
        }
        
        if (noSinkS.getValue() && mc.player.isTouchingWater()) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0, mc.player.getVelocity().z);
        }
    }
    
    private boolean isAboveLiquid(BlockPos pos) {
        return mc.world.getBlockState(pos.down()).getBlock() == Blocks.WATER ||
               mc.world.getBlockState(pos.down()).getBlock() == Blocks.LAVA;
    }
    
    private void handleSolidMode() {
        if (mc.player.getVelocity().y < 0) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0, mc.player.getVelocity().z);
            mc.player.setOnGround(true);
        }
        
        if (mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0) {
            double speed = speedS.getValue();
            Vec3d velocity = mc.player.getVelocity();
            
            if (mc.player.forwardSpeed != 0) {
                double forward = mc.player.forwardSpeed * speed;
                velocity = velocity.add(
                    -Math.sin(Math.toRadians(mc.player.getYaw())) * forward,
                    0,
                    Math.cos(Math.toRadians(mc.player.getYaw())) * forward
                );
            }
            
            if (mc.player.sidewaysSpeed != 0) {
                double strafe = mc.player.sidewaysSpeed * speed;
                velocity = velocity.add(
                    Math.cos(Math.toRadians(mc.player.getYaw())) * strafe,
                    0,
                    Math.sin(Math.toRadians(mc.player.getYaw())) * strafe
                );
            }
            
            mc.player.setVelocity(velocity);
        }
    }
    
    private void handleDolphinMode() {
        if (mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0) {
            double upwardSpeed = 0.1;
            if (ticksOnLiquid % 4 == 0) {
                mc.player.setVelocity(
                    mc.player.getVelocity().x,
                    upwardSpeed,
                    mc.player.getVelocity().z
                );
            }
            
            double speed = speedS.getValue() * 0.7;
            Vec3d velocity = mc.player.getVelocity();
            
            if (mc.player.forwardSpeed != 0) {
                double forward = mc.player.forwardSpeed * speed;
                velocity = velocity.add(
                    -Math.sin(Math.toRadians(mc.player.getYaw())) * forward,
                    0,
                    Math.cos(Math.toRadians(mc.player.getYaw())) * forward
                );
            }
            
            if (mc.player.sidewaysSpeed != 0) {
                double strafe = mc.player.sidewaysSpeed * speed;
                velocity = velocity.add(
                    Math.cos(Math.toRadians(mc.player.getYaw())) * strafe,
                    0,
                    Math.sin(Math.toRadians(mc.player.getYaw())) * strafe
                );
            }
            
            mc.player.setVelocity(velocity);
        }
    }
    
    private void handleBounceMode() {
        if (mc.player.getVelocity().y < 0) {
            double bounceHeight = 0.15;
            mc.player.setVelocity(
                mc.player.getVelocity().x,
                bounceHeight,
                mc.player.getVelocity().z
            );
            
            if (mc.player.forwardSpeed != 0 || mc.player.sidewaysSpeed != 0) {
                double speed = speedS.getValue() * 0.5;
                Vec3d velocity = mc.player.getVelocity();
                
                if (mc.player.forwardSpeed != 0) {
                    double forward = mc.player.forwardSpeed * speed;
                    velocity = velocity.add(
                        -Math.sin(Math.toRadians(mc.player.getYaw())) * forward,
                        0,
                        Math.cos(Math.toRadians(mc.player.getYaw())) * forward
                    );
                }
                
                if (mc.player.sidewaysSpeed != 0) {
                    double strafe = mc.player.sidewaysSpeed * speed;
                    velocity = velocity.add(
                        Math.cos(Math.toRadians(mc.player.getYaw())) * strafe,
                        0,
                        Math.sin(Math.toRadians(mc.player.getYaw())) * strafe
                    );
                }
                
                mc.player.setVelocity(velocity);
            }
        }
    }
}

