package org.kurva.werlii.client.module.movement;

import net.minecraft.entity.effect.StatusEffects;
import org.kurva.werlii.client.module.Module;
import org.lwjgl.glfw.GLFW;

public class Speed extends Module {
    private double speedFactor = 1.1;
    private boolean useTimer = false;
    private float timerSpeed = 1.08f;
    
    public Speed() {
        super("Speed", "Increases movement speed", Category.MOVEMENT);
        this.setKeyCode(GLFW.GLFW_KEY_V);
        this.registerKeybinding("Werlii Movement");
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        if (mc.player.forwardSpeed == 0 && mc.player.sidewaysSpeed == 0) {
            return;
        }
        
        if (mc.player.isTouchingWater() || mc.player.isInLava()) {
            return;
        }
        
        double baseSpeed = 0.2873;
        
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }
        
        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            baseSpeed /= 1.0 + 0.2 * (amplifier + 1);
        }
        
        baseSpeed *= speedFactor;
        
        if (mc.player.isOnGround()) {
            mc.player.jump();
        }
        
        double yaw = Math.toRadians(mc.player.getYaw());
        mc.player.setVelocity(
            -Math.sin(yaw) * baseSpeed,
            mc.player.getVelocity().y,
            Math.cos(yaw) * baseSpeed
        );
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        if (useTimer) {
        }
    }
}

