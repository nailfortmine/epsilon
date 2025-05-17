package org.kurva.werlii.client.module.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import org.kurva.werlii.Werlii;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.*;
import org.kurva.werlii.client.util.EntityUtil;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

public class KillAura extends Module {
    private LivingEntity target;
    private final StopWatch attackTimer = new StopWatch();
    private float lastYaw;
    private float lastPitch;
    private static KillAura instance;

    // Кэш целей
    private List<LivingEntity> cachedTargets = new ArrayList<>();
    private int cacheTime = 0;

    // Настройки
    private final NumberSetting attackRange;
    private final NumberSetting preAimRange;
    private final NumberSetting elytraRange;
    private final ModeSetting rotationType;
    private final NumberSetting rotationSpeed;
    private final ModeSetting critType;
    private final BooleanSetting targetPlayers;
    private final BooleanSetting targetMobs;
    private final BooleanSetting targetAnimals;
    private final BooleanSetting targetFriends;
    private final BooleanSetting targetNaked;
    private final BooleanSetting targetInvis;
    private final BooleanSetting breakShield;
    private final BooleanSetting onlyCrits;
    private final BooleanSetting smartCrits;
    private final BooleanSetting throughWalls;
    private final BooleanSetting pauseOnEat;
    private final BooleanSetting movementCorrection;
    private final ModeSetting priority;
    private final BooleanSetting swing;
    private final BooleanSetting useWeaponSpeed;
    private final NumberSetting randomDelay;
    private final BooleanSetting threatPriority;

    public KillAura() {
        super("KillAura", "Автоматически атакует ближайших существ в радиусе 360°", Category.COMBAT);
        instance = this;
        this.setKeyCode(GLFW.GLFW_KEY_K);
        this.registerKeybinding("Werlii Combat");

        // Настройки
        attackRange = new NumberSetting("Дальность атаки", "Дальность для атаки", this, 4.0, 2.5, 6.0, 0.1);
        preAimRange = new NumberSetting("Дальность наводки", "Дополнительная дальность для наводки", this, 0.5, 0.0, 3.0, 0.1);
        elytraRange = new NumberSetting("Дальность с элитрами", "Дополнительная дальность при полёте с элитрами", this, 2.0, 0.0, 10.0, 0.1);
        rotationType = new ModeSetting("Тип поворота", "Стиль поворота", this, "Smooth", "Smooth", "Snap", "Grim");
        rotationSpeed = new NumberSetting("Скорость поворота", "Скорость поворота в градусах/тик", this, 90.0, 30.0, 180.0, 5.0);
        critType = new ModeSetting("Критические удары", "Помощь с критическими ударами", this, "None", "None", "Matrix", "NCP");
        targetPlayers = new BooleanSetting("Атаковать игроков", "Атаковать игроков", this, true);
        targetMobs = new BooleanSetting("Атаковать мобов", "Атаковать враждебных мобов", this, false);
        targetAnimals = new BooleanSetting("Атаковать животных", "Атаковать животных", this, false);
        targetFriends = new BooleanSetting("Атаковать друзей", "Атаковать друзей", this, false);
        targetNaked = new BooleanSetting("Атаковать без брони", "Атаковать игроков без брони", this, true);
        targetInvis = new BooleanSetting("Атаковать невидимых", "Атаковать невидимых существ", this, true);
        breakShield = new BooleanSetting("Ломать щиты", "Ломать щиты врагов топором", this, true);
        onlyCrits = new BooleanSetting("Только криты", "Атаковать только при критических ударах", this, false);
        smartCrits = new BooleanSetting("Умные криты", "Оптимизировать условия критов", this, true);
        throughWalls = new BooleanSetting("Сквозь стены", "Атаковать сквозь стены", this, true); // Установлено true для тестирования
        pauseOnEat = new BooleanSetting("Пауза при еде", "Не атаковать во время еды", this, true);
        movementCorrection = new BooleanSetting("Коррекция движения", "Исправлять движение для поворотов", this, true);
        priority = new ModeSetting("Приоритет", "Приоритет выбора цели", this, "Distance", "Distance", "Health", "Threat");
        swing = new BooleanSetting("Взмах", "Взмахивать рукой при атаке", this, true);
        useWeaponSpeed = new BooleanSetting("Скорость оружия", "Атаковать с учетом скорости оружия", this, false); // Отключено для тестирования
        randomDelay = new NumberSetting("Случайная задержка", "Добавляет случайную задержку между атаками (мс)", this, 50, 0, 200, 10);
        threatPriority = new BooleanSetting("Приоритет угрозы", "Учитывать уровень угрозы целей", this, true);

        addSetting(attackRange);
        addSetting(preAimRange);
        addSetting(elytraRange);
        addSetting(rotationType);
        addSetting(rotationSpeed);
        addSetting(critType);
        addSetting(targetPlayers);
        addSetting(targetMobs);
        addSetting(targetAnimals);
        addSetting(targetFriends);
        addSetting(targetNaked);
        addSetting(targetInvis);
        addSetting(breakShield);
        addSetting(onlyCrits);
        addSetting(smartCrits);
        addSetting(throughWalls);
        addSetting(pauseOnEat);
        addSetting(movementCorrection);
        addSetting(priority);
        addSetting(swing);
        addSetting(useWeaponSpeed);
        addSetting(randomDelay);
        addSetting(threatPriority);
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
        attackTimer.reset();
        lastYaw = 0;
        lastPitch = 0;
        cachedTargets.clear();
        cacheTime = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        attackTimer.reset();
        lastYaw = 0;
        lastPitch = 0;
        cachedTargets.clear();
        cacheTime = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Пауза при использовании предмета (например, еды)
        if (pauseOnEat.getValue() && mc.player.isUsingItem()) return;

        // Обновление кэша целей
        if (cacheTime <= 0 || target == null || !isValidTarget(target)) {
            cachedTargets = findPotentialTargets();
            cacheTime = 5; // Обновляем кэш каждые 5 тиков
        } else {
            cacheTime--;
        }

        // Выбор лучшей цели
        target = findBestTargetFromList(cachedTargets);

        if (target != null) {
            // Логирование для отладки


            // Ротация к цели (только тело, не камера)
            rotateToTarget(target);

            // Помощь с критическими ударами
            if (!mc.player.isFallFlying() && mc.player.distanceTo(target) <= attackRange.getValue()) {
                critHelper();
            }

            // Логика атаки
            if (attackTimer.hasPassed(500) && shouldAttack()) {
                Werlii.LOGGER.info("Attempting attack on: " + target.getName().getString());
                updateAttack();
            }
        } else {
            attackTimer.reset();
        }
    }

    private boolean shouldAttack() {
        if (onlyCrits.getValue() && !isInCriticalHitCondition()) {
            Werlii.LOGGER.info("Attack blocked: Not in critical hit condition");
            return false;
        }
        if (mc.player.distanceTo(target) > attackRange.getValue()) {
            Werlii.LOGGER.info("Attack blocked: Target out of range (" + mc.player.distanceTo(target) + " > " + attackRange.getValue() + ")");
            return false;
        }
        if (!throughWalls.getValue() && !mc.player.canSee(target)) {
            Werlii.LOGGER.info("Attack blocked: Cannot see target");
            return false;
        }
        return true;
    }

    private void updateAttack() {
        // Проверка скорости атаки оружия (отключено для теста)
        if (useWeaponSpeed.getValue()) {
            float cooldown = mc.player.getAttackCooldownProgress(0.5f);
            if (cooldown < 1.0f) {
                Werlii.LOGGER.info("Attack blocked: Weapon cooldown not ready (" + cooldown + ")");
                return;
            }
        }

        // Случайная задержка
        long delay = 500 + (randomDelay.getValue() > 0 ? (long) (Math.random() * randomDelay.getValue()) : 0);
        if (!attackTimer.hasPassed(delay)) {
            Werlii.LOGGER.info("Attack blocked: Delay not elapsed (" + (System.currentTimeMillis() - attackTimer.lastMS) + " < " + delay + ")");
            return;
        }

        // Проверка Raycast (упрощена для теста)
        if (throughWalls.getValue() || raycastCheck(target)) {
            // Ломание щита
            if (breakShield.getValue() && target instanceof PlayerEntity player && player.isBlocking()) {
                Werlii.LOGGER.info("Breaking shield of: " + player.getName().getString());
                breakShieldPlayer(player);
            } else {
                Werlii.LOGGER.info("Attacking: " + target.getName().getString());
                mc.interactionManager.attackEntity(mc.player, target);
                if (swing.getValue()) mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else {
            Werlii.LOGGER.info("Attack blocked: Raycast failed");
        }

        attackTimer.reset();
    }

    private boolean raycastCheck(LivingEntity target) {
        HitResult hit = mc.player.raycast(attackRange.getValue() + 2, 1.0f, false);
        boolean result = hit.getType() == HitResult.Type.ENTITY && ((EntityHitResult) hit).getEntity() == target;
        if (!result) Werlii.LOGGER.info("Raycast check failed for: " + target.getName().getString());
        return result;
    }

    private boolean isInCriticalHitCondition() {
        if (smartCrits.getValue()) {
            return mc.player.fallDistance > 0.5 && !mc.player.isOnGround() && !mc.player.isSubmergedInWater() &&
                    !mc.player.isClimbing() && !mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);
        }
        return mc.player.fallDistance > 0.0f && !mc.player.isOnGround() && !mc.player.isSubmergedInWater() &&
                !mc.player.isClimbing() && !mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);
    }

    private void critHelper() {
        switch (critType.getValue()) {
            case "Matrix":
                if (mc.options.jumpKey.isPressed() && mc.player.getVelocity().y < -0.1 && mc.player.fallDistance > 0.5) {
                    mc.player.setVelocity(mc.player.getVelocity().x, -0.5, mc.player.getVelocity().z);
                }
                break;
            case "NCP":
                if (mc.options.jumpKey.isPressed() && mc.player.fallDistance > 0.0f) {
                    mc.player.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y - 0.078, mc.player.getVelocity().z);
                }
                break;
        }
    }

    private List<LivingEntity> findPotentialTargets() {
        return mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(attackRange.getValue() + preAimRange.getValue()), this::isValidTarget)
                .stream()
                .filter(this::isInRange)
                .collect(Collectors.toList());
    }

    private LivingEntity findBestTargetFromList(List<LivingEntity> targets) {
        if (targets.isEmpty()) {
            Werlii.LOGGER.info("No valid targets found");
            return null;
        }

        // Сортировка по приоритету
        targets.sort((e1, e2) -> {
            double priority1 = calculatePriority(e1);
            double priority2 = calculatePriority(e2);
            return Double.compare(priority2, priority1);
        });

        LivingEntity selectedTarget = targets.get(0);
        Werlii.LOGGER.info("Selected target: " + selectedTarget.getName().getString());
        return selectedTarget;
    }

    private double calculatePriority(LivingEntity entity) {
        double priorityValue = 0;

        // Основной приоритет
        switch (priority.getValue()) {
            case "Distance":
                priorityValue = -mc.player.distanceTo(entity);
                break;
            case "Health":
                priorityValue = -entity.getHealth();
                break;
            case "Threat":
                priorityValue = -calculateThreatLevel(entity);
                break;
        }

        // Дополнительный приоритет угрозы
        if (threatPriority.getValue()) {
            if (entity instanceof PlayerEntity player) {
                ItemStack mainHand = player.getMainHandStack();
                if (mainHand.getItem() instanceof net.minecraft.item.SwordItem) priorityValue += 15;
                if (mainHand.getItem() instanceof AxeItem) priorityValue += 10;
            }
        }

        return priorityValue;
    }

    private double calculateThreatLevel(LivingEntity entity) {
        double threat = entity.getHealth();
        if (entity instanceof PlayerEntity player) {
            threat += getArmorValue(player) * 0.5;
            ItemStack mainHand = player.getMainHandStack();
            if (mainHand.getItem() instanceof net.minecraft.item.SwordItem) threat += 5;
            if (mainHand.getItem() instanceof AxeItem) threat += 3;
        }
        return threat;
    }

    private boolean isInRange(LivingEntity entity) {
        double range = attackRange.getValue() + preAimRange.getValue();
        if (mc.player.isFallFlying()) range += elytraRange.getValue();
        return mc.player.distanceTo(entity) <= range;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == mc.player || !entity.isAlive() || entity.isInvulnerable()) return false;

        if (entity instanceof PlayerEntity player) {
            if (!targetPlayers.getValue()) return false;
            if (!targetFriends.getValue() /* && EntityUtil.isFriend(player) */) return false;
            if (!targetNaked.getValue() && getArmorValue(player) == 0) return false;
            if (!targetInvis.getValue() && player.isInvisible()) return false;
            return EntityUtil.isValidTarget(player);
        }

        if (entity instanceof Monster && targetMobs.getValue()) return true;
        if (entity instanceof AnimalEntity && targetAnimals.getValue()) return true;
        return false;
    }

    private int getArmorValue(PlayerEntity player) {
        int armorValue = 0;
        RegistryEntry<net.minecraft.enchantment.Enchantment> protection = mc.world.getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT).getEntry(net.minecraft.enchantment.Enchantments.PROTECTION).orElse(null);
        for (ItemStack stack : player.getArmorItems()) {
            if (stack.getItem() instanceof net.minecraft.item.ArmorItem armor) {
                armorValue += armor.getProtection();
                if (protection != null) {
                    armorValue += EnchantmentHelper.getLevel(protection, stack) * 0.25;
                }
            }
        }
        return armorValue;
    }

    private void rotateToTarget(LivingEntity target) {
        ClientPlayerEntity player = mc.player;
        float yawSpeed = rotationSpeed.getValue().floatValue();
        float pitchSpeed = yawSpeed / 2;

        // Рандомизация для античитов
        float randomOffset = (float) (Math.random() * 0.5 - 0.25);
        float gcd = getGCDValue() * (0.95f + (float) Math.random() * 0.1f);

        // Имитация человеческого дрожания
        float yawJitter = (float) Math.sin(System.currentTimeMillis() / 300.0) * 0.3f;
        float pitchJitter = (float) Math.cos(System.currentTimeMillis() / 400.0) * 0.2f;

        // Вычисление углов
        Vec3d vec = new Vec3d(
                target.getX() - player.getX(),
                target.getEyeY() - player.getEyeY(),
                target.getZ() - player.getZ()
        );
        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
        float pitchToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z))));

        float yaw, pitch;
        switch (rotationType.getValue()) {
            case "Snap":
                yaw = yawToTarget + randomOffset + yawJitter;
                pitch = MathHelper.clamp(pitchToTarget + randomOffset + pitchJitter, -90.0f, 90.0f);
                break;
            case "Grim":
                float dynamicSpeed = yawSpeed + (float) (Math.random() * 10 - 5);
                float yawDelta = MathHelper.wrapDegrees(yawToTarget - lastYaw);
                float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - lastPitch);
                yaw = lastYaw + MathHelper.clamp(yawDelta, -dynamicSpeed, dynamicSpeed) + (float) (Math.random() * 0.15);
                pitch = MathHelper.clamp(lastPitch + MathHelper.clamp(pitchDelta, -dynamicSpeed / 2, dynamicSpeed / 2) + (float) (Math.random() * 0.05), -90.0f, 90.0f);
                break;
            default: // Smooth
                yawDelta = MathHelper.wrapDegrees(yawToTarget - lastYaw);
                pitchDelta = MathHelper.wrapDegrees(pitchToTarget - lastPitch);
                yaw = lastYaw + MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed) + randomOffset + yawJitter;
                pitch = MathHelper.clamp(lastPitch + MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed) + randomOffset + pitchJitter, -90.0f, 90.0f);
                break;
        }

        // Применение GCD коррекции
        yaw -= (yaw - lastYaw) % gcd;
        pitch -= (pitch - lastPitch) % gcd;

        // Обновление только тела игрока, не камеры
        player.bodyYaw = yaw;
        player.headYaw = yaw;

        // Отправка пакета для синхронизации с сервером
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                    player.getX(), player.getY(), player.getZ(),
                    yaw, pitch, player.isOnGround()
            ));
        }

        // Коррекция движения (только для тела)
        if (movementCorrection.getValue()) {
            player.bodyYaw = yaw;
        }

        lastYaw = yaw;
        lastPitch = pitch;
    }

    private void breakShieldPlayer(PlayerEntity player) {
        int axeSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                axeSlot = i;
                break;
            }
        }

        if (axeSlot != -1) {
            int currentSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = axeSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(axeSlot));
            mc.interactionManager.attackEntity(mc.player, player);
            if (swing.getValue()) mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.getInventory().selectedSlot = currentSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
        } else {
            mc.interactionManager.attackEntity(mc.player, player);
            if (swing.getValue()) mc.player.swingHand(Hand.MAIN_HAND);
        }
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

        public void reset(long offset) {
            lastMS = System.currentTimeMillis() + offset;
        }
    }
}