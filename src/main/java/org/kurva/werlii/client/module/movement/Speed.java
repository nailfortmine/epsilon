package org.kurva.werlii.client.module.movement;

import net.minecraft.entity.effect.StatusEffects;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class Speed extends Module {
    private final ModeSetting speedMode = new ModeSetting("Режим", "Режим скорости", this, "Strafe", "Strafe", "LowHop", "YPort", "Vanilla");
    private final NumberSetting speedFactor = new NumberSetting("Скорость", "Множитель скорости", this, 1.1, 0.5, 2.0, 0.1);
    private final ModeSetting strafeType = new ModeSetting("Тип стрейфа", "Стиль стрейфа", this, "Strict", "Strict", "Smooth", "Grim");
    private final BooleanSetting autoJump = new BooleanSetting("Авто-прыжок", "Автоматически прыгать", this, true);

    public Speed() {
        super("Speed", "Увеличивает скорость передвижения", Category.MOVEMENT);
        this.setKeyCode(GLFW.GLFW_KEY_V);
        this.registerKeybinding("Werlii Movement");

        addSetting(speedMode);
        addSetting(speedFactor);
        addSetting(strafeType);
        addSetting(autoJump);
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

        // Применение эффектов зелий
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1);
        }

        if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amplifier = mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            baseSpeed /= 1.0 + 0.2 * (amplifier + 1);
        }

        baseSpeed *= speedFactor.getValue();

        double yaw = Math.toRadians(mc.player.getYaw());
        double motionX = -Math.sin(yaw) * baseSpeed;
        double motionZ = Math.cos(yaw) * baseSpeed;

        switch (speedMode.getValue()) {
            case "Strafe":
                if (mc.player.isOnGround() && autoJump.getValue()) {
                    mc.player.jump();
                }
                applyStrafe(motionX, motionZ);
                break;

            case "LowHop":
                if (mc.player.isOnGround() && autoJump.getValue()) {
                    mc.player.jump();
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.31, mc.player.getVelocity().z);
                }
                applyStrafe(motionX * 0.8, motionZ * 0.8);
                break;

            case "YPort":
                if (mc.player.isOnGround() && autoJump.getValue()) {
                    mc.player.jump();
                } else {
                    mc.player.setVelocity(mc.player.getVelocity().x, -0.1, mc.player.getVelocity().z);
                }
                applyStrafe(motionX, motionZ);
                break;

            case "Vanilla":
                if (mc.player.isOnGround() && autoJump.getValue()) {
                    mc.player.jump();
                }
                mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
                break;
        }
    }

    private void applyStrafe(double motionX, double motionZ) {
        switch (strafeType.getValue()) {
            case "Strict":
                mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
                break;

            case "Smooth":
                double currentX = mc.player.getVelocity().x;
                double currentZ = mc.player.getVelocity().z;
                mc.player.setVelocity(
                        currentX + (motionX - currentX) * 0.5,
                        mc.player.getVelocity().y,
                        currentZ + (motionZ - currentZ) * 0.5
                );
                break;

            case "Grim":
                if (mc.player.isOnGround()) {
                    mc.player.setVelocity(motionX * 1.2, mc.player.getVelocity().y, motionZ * 1.2);
                } else {
                    mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
                }
                break;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}