package org.kurva.werlii.client.module.movement;

import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class ElytraFlight extends Module {
    private final ModeSetting modeS;
    private final NumberSetting speedS;
    private final NumberSetting fallSpeedS;
    private final BooleanSetting autoTakeOffS;
    private final BooleanSetting instantFlyS;
    private final BooleanSetting boostS;
    private final NumberSetting boostFactorS;
    
    public ElytraFlight() {
        super("ElytraFlight", "Enhanced elytra flying", Category.MOVEMENT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Movement");
        
        modeS = new ModeSetting("Mode", "Flight mode", this, "Normal", "Normal", "Packet", "Control", "Boost");
        speedS = new NumberSetting("Speed", "Horizontal flight speed", this, 1.8, 0.1, 5.0, 0.1);
        fallSpeedS = new NumberSetting("Fall Speed", "Vertical fall speed", this, 0.0, -0.1, 0.1, 0.01);
        autoTakeOffS = new BooleanSetting("Auto Take Off", "Automatically take off when falling", this, true);
        instantFlyS = new BooleanSetting("Instant Fly", "Start flying instantly", this, false);
        boostS = new BooleanSetting("Boost", "Enable rocket boost", this, true);
        boostFactorS = new NumberSetting("Boost Factor", "Rocket boost strength", this, 1.5, 1.0, 3.0, 0.1);
        
        addSetting(modeS);
        addSetting(speedS);
        addSetting(fallSpeedS);
        addSetting(autoTakeOffS);
        addSetting(instantFlyS);
        addSetting(boostS);
        addSetting(boostFactorS);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        if (!isWearingElytra()) return;
        
        if (autoTakeOffS.getValue() && !mc.player.isFallFlying() && mc.player.fallDistance > 1.0f) {
            mc.player.startFallFlying();
        }
        
        if (mc.player.isFallFlying()) {
            switch (modeS.getValue()) {
                case "Normal":
                    handleNormalFlight();
                    break;
                case "Packet":
                    handlePacketFlight();
                    break;
                case "Control":
                    handleControlFlight();
                    break;
                case "Boost":
                    handleBoostFlight();
                    break;
            }
        }
    }
    
    private boolean isWearingElytra() {
        return !mc.player.getInventory().getArmorStack(2).isEmpty() && 
               mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA;
    }
    
    private void handleNormalFlight() {
        boolean jumpPressed = mc.options.jumpKey.isPressed();
        boolean sneakPressed = mc.options.sneakKey.isPressed();
        
        double verticalSpeed;
        if (jumpPressed && !sneakPressed) {
            verticalSpeed = 0.5;
        } else if (sneakPressed && !jumpPressed) {
            verticalSpeed = -0.5;
        } else {
            verticalSpeed = fallSpeedS.getValue();
        }
        
        Vec3d forward = Vec3d.fromPolar(0, mc.player.getYaw());
        Vec3d right = Vec3d.fromPolar(0, mc.player.getYaw() + 90);
        
        Vec3d movement = Vec3d.ZERO;
        
        if (mc.options.forwardKey.isPressed()) {
            movement = movement.add(forward);
        }
        if (mc.options.backKey.isPressed()) {
            movement = movement.add(forward.negate());
        }
        if (mc.options.rightKey.isPressed()) {
            movement = movement.add(right);
        }
        if (mc.options.leftKey.isPressed()) {
            movement = movement.add(right.negate());
        }
        
        if (movement.lengthSquared() > 0) {
            movement = movement.normalize().multiply(speedS.getValue());
            
            mc.player.setVelocity(movement.x, verticalSpeed, movement.z);
        } else {
            mc.player.setVelocity(0, verticalSpeed, 0);
        }
    }
    
    private void handlePacketFlight() {
        handleNormalFlight();
    }
    
    private void handleControlFlight() {
        handleNormalFlight();
    }
    
    private void handleBoostFlight() {
        handleNormalFlight();
        
        if (boostS.getValue() && mc.options.useKey.isPressed()) {
            Vec3d velocity = mc.player.getVelocity();
            Vec3d lookVec = mc.player.getRotationVector();
            
            double boostFactor = boostFactorS.getValue();
            
            mc.player.setVelocity(
                velocity.x + lookVec.x * 0.1 * boostFactor,
                velocity.y + lookVec.y * 0.1 * boostFactor,
                velocity.z + lookVec.z * 0.1 * boostFactor
            );
        }
    }
}

