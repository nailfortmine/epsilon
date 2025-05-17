package org.kurva.werlii.client.module.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.module.combat.KillAura;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class TargetStrafe extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private static TargetStrafe instance;
    private double angle = 0.0; // Текущий угол для кругового движения

    // Настройки
    private final NumberSetting radiusS;
    private final NumberSetting speedS;
    private final BooleanSetting clockwiseS;
    private final ModeSetting bypassModeS;
    private final BooleanSetting avoidCollisionS;
    private final BooleanSetting autoJumpS;
    private final BooleanSetting allowAirStrafeS; // Новая настройка для страйфа в воздухе

    public TargetStrafe() {
        super("TargetStrafe", "Движение вокруг цели KillAura", Category.MOVEMENT);
        instance = this;
        this.setKeyCode(GLFW.GLFW_KEY_J);
        this.registerKeybinding("Werlii Movement");

        radiusS = new NumberSetting("Радиус", "Дистанция до цели", this, 3.0, 1.0, 5.0, 0.1);
        speedS = new NumberSetting("Скорость", "Скорость страйфа", this, 0.2, 0.1, 0.5, 0.01);
        clockwiseS = new BooleanSetting("По часовой", "Двигаться по часовой стрелке", this, true);
        bypassModeS = new ModeSetting("Режим обхода", "Тип обхода античита", this, "Default", "Default", "Legit", "Matrix", "Grim");
        avoidCollisionS = new BooleanSetting("Избегать стен", "Избегать столкновений с блоками", this, true);
        autoJumpS = new BooleanSetting("Авто-прыжок", "Автоматически прыгать при страйфе", this, false);
        allowAirStrafeS = new BooleanSetting("Страйф в воздухе", "Позволять страйф в воздухе", this, true);

        addSetting(radiusS);
        addSetting(speedS);
        addSetting(clockwiseS);
        addSetting(bypassModeS);
        addSetting(avoidCollisionS);
        addSetting(autoJumpS);
        addSetting(allowAirStrafeS);
    }

    public static TargetStrafe getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        angle = 0.0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0); // Останавливаем горизонтальное движение
        }
        angle = 0.0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Проверяем, разрешён ли страйф в воздухе или игрок на земле
        if (!allowAirStrafeS.getValue() && !mc.player.isOnGround()) return;

        // Получаем KillAura и цель
        KillAura killAura = KillAura.getInstance();
        if (killAura == null || !killAura.isEnabled()) return;

        LivingEntity target = killAura.getTarget();
        if (target == null || !target.isAlive()) return;

        // Вычисляем желаемую позицию для страйфа
        double targetX = target.getX();
        double targetZ = target.getZ();
        double radius = radiusS.getValue();
        double speed = speedS.getValue();

        // Обновляем угол в зависимости от направления
        double angleSpeed = speed / radius; // Угловая скорость (рад/тик)
        angle += (clockwiseS.getValue() ? -angleSpeed : angleSpeed);

        // Вычисляем целевую позицию
        double strafeX = targetX + radius * Math.cos(angle);
        double strafeZ = targetZ + radius * Math.sin(angle);

        // Проверяем столкновения
        if (avoidCollisionS.getValue() && !isSafePosition(strafeX, mc.player.getY(), strafeZ)) {
            // Если столкновение, меняем направление
            clockwiseS.setValue(!clockwiseS.getValue());
            angle += Math.PI; // Поворачиваем на 180 градусов
            strafeX = targetX + radius * Math.cos(angle);
            strafeZ = targetZ + radius * Math.sin(angle);
        }

        // Вычисляем вектор движения
        Vec3d currentPos = mc.player.getPos();
        Vec3d targetPos = new Vec3d(strafeX, mc.player.getY(), strafeZ);
        Vec3d moveVec = targetPos.subtract(currentPos).normalize().multiply(speed);

        // Авто-прыжок
        if (autoJumpS.getValue() && mc.player.isOnGround()) {
            mc.player.jump();
        }

        // Применяем режим обхода
        switch (bypassModeS.getValue()) {
            case "Default":
                // Простое круговое движение
                break;

            case "Legit":
                // Плавное движение с лёгкой рандомизацией
                moveVec = moveVec.multiply(0.9 + Math.random() * 0.2);
                if (mc.player.isOnGround() && !autoJumpS.getValue()) {
                    // Имитация прыжков для легитности
                    if (Math.random() < 0.05) {
                        mc.player.jump();
                    }
                }
                break;

            case "Matrix":
                // Рандомизация и паузы для Matrix
                moveVec = moveVec.multiply(0.7 + Math.random() * 0.3);
                if (Math.random() < 0.1) {
                    // Случайная пауза
                    moveVec = Vec3d.ZERO;
                }
                if (Math.random() < 0.15) {
                    // Случайное изменение направления
                    clockwiseS.setValue(!clockwiseS.getValue());
                    angle += Math.PI;
                }
                break;

            case "Grim":
                // Хаотичное движение с микрокоррекциями
                moveVec = moveVec.multiply(0.6 + Math.random() * 0.4);
                moveVec = moveVec.add(new Vec3d(
                        Math.random() * 0.1 - 0.05,
                        0,
                        Math.random() * 0.1 - 0.05
                ));
                if (Math.random() < 0.2) {
                    // Случайные микроотклонения
                    angle += (Math.random() * 0.2 - 0.1);
                }
                break;
        }

        // Применяем движение
        double newX = moveVec.x;
        double newZ = moveVec.z;
        double newY = mc.player.getVelocity().y; // Сохраняем вертикальную скорость

        // Проверяем, чтобы не превысить максимальную скорость
        double maxSpeed = mc.player.isOnGround() ? 0.3 : 0.5;
        double speedSquared = newX * newX + newZ * newZ;
        if (speedSquared > maxSpeed * maxSpeed) {
            double scale = maxSpeed / Math.sqrt(speedSquared);
            newX *= scale;
            newZ *= scale;
        }

        mc.player.setVelocity(newX, newY, newZ);
    }

    private boolean isSafePosition(double x, double y, double z) {
        World world = mc.world;
        if (world == null) return false;

        // Проверяем блок под игроком и вокруг
        BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
        BlockPos below = pos.down();

        // Проверяем, что под ногами твёрдый блок (или воздух, если страйф в воздухе разрешён)
        if (!allowAirStrafeS.getValue() && !world.getBlockState(below).isSolid()) {
            return false;
        }

        // Проверяем, что текущая и следующая позиции свободны
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos checkPos = new BlockPos((int) x, (int) y + dy, (int) z);
            if (!world.getBlockState(checkPos).isAir()) {
                return false;
            }
        }

        return true;
    }
}