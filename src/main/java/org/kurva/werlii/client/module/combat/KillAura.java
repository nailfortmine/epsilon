
package org.kurva.werlii.client.module.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.kurva.werlii.client.util.EntityUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class KillAura extends Module {
    private LivingEntity target;
    private int attackTicks = 0;
    private final StopWatch attackTimer = new StopWatch();
    private float lastYaw;
    private float lastPitch;
    private static KillAura instance;

    // Настройки
    private final NumberSetting attackRangeS;
    private final NumberSetting preAimRangeS;
    private final NumberSetting elytraRangeS;
    private final ModeSetting rotationTypeS;
    private final ModeSetting rotationSpeedS;
    private final ModeSetting critTypeS;
    private final BooleanSetting targetPlayersS;
    private final BooleanSetting targetMobsS;
    private final BooleanSetting targetAnimalsS;
    private final BooleanSetting targetFriendsS;
    private final BooleanSetting targetNakedS;
    private final BooleanSetting targetInvisS;
    private final BooleanSetting breakShieldS;
    private final BooleanSetting onlyCritsS;
    private final BooleanSetting smartCritsS;
    private final BooleanSetting throughWallsS;
    private final BooleanSetting wallBypassS;
    private final BooleanSetting pauseOnEatS;
    private final BooleanSetting movementCorrectionS;
    private final ModeSetting priorityS;
    private final NumberSetting fovS;
    private final BooleanSetting swingS;

    public KillAura() {
        super("KillAura", "Автоматически атакует ближайших существ", Category.COMBAT);
        instance = this;
        this.setKeyCode(GLFW.GLFW_KEY_K);
        this.registerKeybinding("Werlii Combat");

        attackRangeS = new NumberSetting("Дальность атаки", "Дальность для атаки", this, 3.6, 2.5, 6.0, 0.05);
        preAimRangeS = new NumberSetting("Дальность наводки", "Дополнительная дальность для наводки", this, 0.3, 0.0, 3.0, 0.05);
        elytraRangeS = new NumberSetting("Дальность с элитрами", "Дополнительная дальность при полёте с элитрами", this, 6.0, 0.0, 16.0, 0.05);
        rotationTypeS = new ModeSetting("Тип поворота", "Стиль поворота", this, "Smooth", "Smooth", "Sharp", "Grim", "Matrix");
        rotationSpeedS = new ModeSetting("Скорость поворота", "Скорость поворота", this, "Medium", "Fast", "Medium", "Slow");
        critTypeS = new ModeSetting("Критические удары", "Помощь с критическими ударами", this, "None", "None", "Matrix", "NCP");
        targetPlayersS = new BooleanSetting("Атаковать игроков", "Атаковать игроков", this, true);
        targetMobsS = new BooleanSetting("Атаковать мобов", "Атаковать враждебных мобов", this, false);
        targetAnimalsS = new BooleanSetting("Атаковать животных", "Атаковать животных", this, false);
        targetFriendsS = new BooleanSetting("Атаковать друзей", "Атаковать друзей", this, false);
        targetNakedS = new BooleanSetting("Атаковать без брони", "Атаковать игроков без брони", this, true);
        targetInvisS = new BooleanSetting("Атаковать невидимых", "Атаковать невидимых игроков", this, true);
        breakShieldS = new BooleanSetting("Ломать щиты", "Ломать щиты врагов топором", this, true);
        onlyCritsS = new BooleanSetting("Только криты", "Атаковать только при критических ударах", this, true);
        smartCritsS = new BooleanSetting("Умные криты", "Оптимизировать условия критов", this, false);
        throughWallsS = new BooleanSetting("Сквозь стены", "Атаковать сквозь стены", this, true);
        wallBypassS = new BooleanSetting("Обход стен", "Обходить проверки стен с рандомизацией", this, false);
        pauseOnEatS = new BooleanSetting("Пауза при еде", "Не атаковать во время еды", this, false);
        movementCorrectionS = new BooleanSetting("Коррекция движения", "Исправлять движение для поворотов", this, true);
        priorityS = new ModeSetting("Приоритет", "Приоритет выбора цели", this, "Distance", "Distance", "Health", "Armor");
        fovS = new NumberSetting("Поле зрения", "Поле зрения для наводки", this, 180.0, 30.0, 360.0, 15.0);
        swingS = new BooleanSetting("Взмах", "Взмахивать рукой при атаке", this, true);

        addSetting(attackRangeS);
        addSetting(preAimRangeS);
        addSetting(elytraRangeS);
        addSetting(rotationTypeS);
        addSetting(rotationSpeedS);
        addSetting(critTypeS);
        addSetting(targetPlayersS);
        addSetting(targetMobsS);
        addSetting(targetAnimalsS);
        addSetting(targetFriendsS);
        addSetting(targetNakedS);
        addSetting(targetInvisS);
        addSetting(breakShieldS);
        addSetting(onlyCritsS);
        addSetting(smartCritsS);
        addSetting(throughWallsS);
        addSetting(wallBypassS);
        addSetting(pauseOnEatS);
        addSetting(movementCorrectionS);
        addSetting(priorityS);
        addSetting(fovS);
        addSetting(swingS);
    }

    public static KillAura getInstance() {
        return instance;
    }

    public LivingEntity getTarget() {
        return target;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
        attackTicks = 0;
        attackTimer.reset();
        lastYaw = 0;
        lastPitch = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        attackTicks = 0;
        attackTimer.reset();
        lastYaw = 0;
        lastPitch = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        if (pauseOnEatS.getValue() && mc.player.isUsingItem()) return;

        // Обновляем цель
        if (target == null || !isValidTarget(target)) {
            target = findBestTarget();
        }

        if (target != null) {
            // Обрабатываем поворот
            float yawSpeed = rotationSpeedS.getValue().equals("Medium") ? 115 : rotationSpeedS.getValue().equals("Fast") ? 180 : 40;
            float pitchSpeed = rotationSpeedS.getValue().equals("Medium") ? 65 : rotationSpeedS.getValue().equals("Fast") ? 90 : 35;
            rotateToTarget(target, yawSpeed, pitchSpeed);

            // Помощь с критическими ударами
            if (!mc.player.isFallFlying() && mc.player.distanceTo(target) <= attackRangeS.getValue()) {
                critHelper();
            }

            // Логика атаки
            if (attackTimer.hasPassed(500) && shouldAttack()) {
                updateAttack();
            }
        } else {
            attackTimer.reset();
        }
    }

    private boolean shouldAttack() {
        if (onlyCritsS.getValue() && !isInCriticalHitCondition()) return false;
        if (mc.player.distanceTo(target) > attackRangeS.getValue()) return false;
        if (!throughWallsS.getValue() && !mc.player.canSee(target)) return false;
        if (throughWallsS.getValue() && wallBypassS.getValue() && !mc.player.canSee(target)) {
            target.setPosition(
                    target.getX() + (Math.random() * 0.3 - 0.15),
                    target.getY(),
                    target.getZ() + (Math.random() * 0.3 - 0.15)
            );
        }
        return true;
    }

    private void updateAttack() {
        if (breakShieldS.getValue() && target instanceof PlayerEntity player && player.isBlocking()) {
            breakShield(player);
        }

        mc.interactionManager.attackEntity(mc.player, target);
        if (swingS.getValue()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        attackTimer.reset();
    }

    private void breakShield(PlayerEntity player) {
        int axeSlot = findAxeSlot();
        if (axeSlot != -1) {
            int currentSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = axeSlot;
            mc.interactionManager.attackEntity(mc.player, player);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.getInventory().selectedSlot = currentSlot;
        }
    }

    private int findAxeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private boolean isInCriticalHitCondition() {
        if (smartCritsS.getValue()) {
            return mc.player.fallDistance > 0.5 && !mc.player.isOnGround() && !mc.player.isSubmergedInWater() &&
                    !mc.player.isClimbing() && !mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);
        }
        return mc.player.fallDistance > 0.0f && !mc.player.isOnGround() && !mc.player.isSubmergedInWater() &&
                !mc.player.isClimbing() && !mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);
    }

    private void critHelper() {
        switch (critTypeS.getValue()) {
            case "Matrix":
                if (mc.options.jumpKey.isPressed() && mc.player.getVelocity().y < -0.1 && mc.player.fallDistance > 0.5) {
                    mc.player.setVelocity(mc.player.getVelocity().x, -1.0, mc.player.getVelocity().z);
                }
                break;
            case "NCP":
                if (mc.options.jumpKey.isPressed() && mc.player.fallDistance > 0.0f) {
                    mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y - 0.078, mc.player.getVelocity().z);
                }
                break;
        }
    }

    private LivingEntity findBestTarget() {
        List<LivingEntity> targets = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(this::isValidTarget)
                .filter(entity -> mc.player.distanceTo(entity) <= (attackRangeS.getValue() + preAimRangeS.getValue() + (mc.player.isFallFlying() ? elytraRangeS.getValue() : 0)))
                .filter(this::isInFov)
                .collect(Collectors.toList());

        if (targets.isEmpty()) return null;

        switch (priorityS.getValue()) {
            case "Distance":
                targets.sort(Comparator.comparingDouble(mc.player::distanceTo));
                break;
            case "Health":
                targets.sort(Comparator.comparingDouble(LivingEntity::getHealth));
                break;
            case "Armor":
                targets.sort(Comparator.comparingInt(entity -> entity instanceof PlayerEntity ?
                        getArmorValue((PlayerEntity) entity) : 0));
                break;
        }

        return targets.get(0);
    }

    private int getArmorValue(PlayerEntity player) {
        int armorValue = 0;
        for (ItemStack stack : player.getArmorItems()) {
            if (stack.getItem() instanceof ArmorItem armor) {
                armorValue += armor.getProtection();
            }
        }
        return armorValue;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == mc.player || !entity.isAlive()) return false;

        if (entity instanceof PlayerEntity player) {
            if (!targetPlayersS.getValue()) return false;
            if (!targetFriendsS.getValue() /* && EntityUtil.isFriend(player) */) return false;
            if (!targetNakedS.getValue() && getArmorValue(player) == 0) return false;
            if (!targetInvisS.getValue() && player.isInvisible()) return false;
            return EntityUtil.isValidTarget(player);
        }

        boolean isMob = entity.isAttackable();
        boolean isAnimal = !isMob && entity.isAlive();
        return (isMob && targetMobsS.getValue()) || (isAnimal && targetAnimalsS.getValue());
    }

    private boolean isInFov(LivingEntity entity) {
        double fov = fovS.getValue();
        if (fov >= 360) return true;

        Vec3d playerRotation = mc.player.getRotationVec(1.0f);
        Vec3d entityVec = new Vec3d(
                entity.getX() - mc.player.getX(),
                entity.getEyeY() - mc.player.getEyeY(),
                entity.getZ() - mc.player.getZ()
        ).normalize();

        double angle = Math.toDegrees(Math.acos(playerRotation.dotProduct(entityVec)));
        return angle <= fov / 2;
    }

    private void rotateToTarget(LivingEntity target, float yawSpeed, float pitchSpeed) {
        // Вычисляем вектор до цели
        Vec3d vec = new Vec3d(
                target.getX() - mc.player.getX(),
                target.getEyeY() - mc.player.getEyeY(),
                target.getZ() - mc.player.getZ()
        );
        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
        float pitchToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z))));
        float yawDelta = MathHelper.wrapDegrees(yawToTarget - lastYaw);
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - lastPitch);
        float yaw, pitch;
        float gcd = getGCDValue();

        switch (rotationTypeS.getValue()) {
            case "Smooth":
                // Плавная интерполяция с переменной скоростью
                float yawLerp = Math.min(Math.max(Math.abs(yawDelta), 1.0f), yawSpeed * 0.5f);
                float pitchLerp = Math.min(Math.max(Math.abs(pitchDelta), 1.0f), pitchSpeed * 0.5f);
                yaw = lastYaw + (yawDelta > 0 ? yawLerp : -yawLerp);
                pitch = lastPitch + (pitchDelta > 0 ? pitchLerp : -pitchLerp);
                // Добавляем небольшую рандомизацию для естественности
                yaw += (float) (Math.random() * 0.1 - 0.05);
                pitch += (float) (Math.random() * 0.1 - 0.05);
                break;

            case "Sharp":
                // Мгновенный поворот с лёгкой задержкой и рандомизацией
                yaw = yawToTarget + (float) (Math.random() * 0.2 - 0.1);
                pitch = pitchToTarget + (float) (Math.random() * 0.2 - 0.1);
                // Задержка для реалистичности
                if (attackTimer.hasPassed(50)) {
                    yaw = MathHelper.lerp(0.8f, lastYaw, yaw);
                    pitch = MathHelper.lerp(0.8f, lastPitch, pitch);
                }
                break;

            case "Grim":
                // Ротация для Grim: микрокоррекции и сильная рандомизация
                float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1.0f), yawSpeed * 0.3f);
                float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1.0f), pitchSpeed * 0.3f);
                yaw = lastYaw + (yawDelta > 0 ? clampedYaw : -clampedYaw) + (float) (Math.random() * 0.5 - 0.25);
                pitch = lastPitch + (pitchDelta > 0 ? clampedPitch : -clampedPitch) + (float) (Math.random() * 0.5 - 0.25);
                // Случайные микроотклонения
                if (Math.random() < 0.1) {
                    yaw += (float) (Math.random() * 1.0 - 0.5);
                    pitch += (float) (Math.random() * 1.0 - 0.5);
                }
                break;

            case "Matrix":
                // Ротация для Matrix: имитация человеческой неточности
                yaw = yawToTarget + (float) (Math.random() * 0.4 - 0.2);
                pitch = pitchToTarget + (float) (Math.random() * 0.4 - 0.2);
                // Случайные паузы (имитация "задумчивости")
                if (Math.random() < 0.05) {
                    yaw = lastYaw;
                    pitch = lastPitch;
                } else {
                    // Плавное сглаживание для естественности
                    yaw = MathHelper.lerp(0.6f, lastYaw, yaw);
                    pitch = MathHelper.lerp(0.6f, lastPitch, pitch);
                }
                break;

            default:
                yaw = yawToTarget;
                pitch = pitchToTarget;
                break;
        }

        // Применяем GCD корректировку
        yaw -= (yaw - lastYaw) % gcd;
        pitch -= (pitch - lastPitch) % gcd;

        // Ограничиваем pitch
        pitch = MathHelper.clamp(pitch, -90.0f, 90.0f);

        // Обновляем только bodyYaw и headYaw, оставляя камеру свободной
        mc.player.bodyYaw = yaw;
        mc.player.headYaw = yaw;

        // Отправляем пакет для синхронизации
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                    mc.player.getX(),
                    mc.player.getY(),
                    mc.player.getZ(),
                    mc.player.getYaw(), // Камера не меняется
                    mc.player.getPitch(), // Камера не меняется
                    mc.player.isOnGround()
            ));
        }

        // Коррекция движения
        if (movementCorrectionS.getValue()) {
            mc.player.bodyYaw = yaw;
        }

        // Сохраняем текущие значения
        lastYaw = yaw;
        lastPitch = pitch;
    }

    private float getGCDValue() {
        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue();
        return (float) (0.6 + sensitivity * 0.2) * 0.15f;
    }

    private static class StopWatch {
        private long lastMS;

        public StopWatch() {
            reset();
        }

        public boolean hasPassed(long ms) {
            return System.currentTimeMillis() - lastMS >= ms;
        }

        public void reset() {
            lastMS = System.currentTimeMillis();
        }
    }
}
