package org.kurva.werlii.client.module.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.lwjgl.glfw.GLFW;

public class InventoryMove extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private static InventoryMove instance;

    // Настройки
    private final BooleanSetting sprintS;
    private final BooleanSetting jumpS;

    public InventoryMove() {
        super("InventoryMove", "Позволяет двигаться с открытым инвентарём", Category.MOVEMENT);
        instance = this;
        this.setKeyCode(GLFW.GLFW_KEY_I);
        this.registerKeybinding("Werlii Movement");

        sprintS = new BooleanSetting("Спринт", "Разрешить спринт в инвентаре", this, true);
        jumpS = new BooleanSetting("Прыжок", "Разрешить прыжки в инвентаре", this, true);

        addSetting(sprintS);
        addSetting(jumpS);
    }

    public static InventoryMove getInstance() {
        return instance;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Проверяем, открыт ли инвентарь или другой экран
        Screen screen = mc.currentScreen;
        if (!(screen instanceof HandledScreen || screen instanceof InventoryScreen)) return;

        // Обрабатываем клавиши движения
        KeyBinding[] movementKeys = {
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey
        };

        // Перенаправляем нажатия клавиш
        for (KeyBinding key : movementKeys) {
            if (key.isPressed()) {
                KeyBinding.setKeyPressed(key.getDefaultKey(), true);
            } else {
                KeyBinding.setKeyPressed(key.getDefaultKey(), false);
            }
        }

        // Обрабатываем спринт
        if (sprintS.getValue() && mc.options.sprintKey.isPressed()) {
            mc.player.setSprinting(true);
        }

        // Обрабатываем прыжок
        if (jumpS.getValue() && mc.options.jumpKey.isPressed() && mc.player.isOnGround()) {
            mc.player.jump();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Сбрасываем нажатия клавиш, чтобы избежать залипания
        KeyBinding[] movementKeys = {
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey,
                mc.options.sprintKey
        };

        for (KeyBinding key : movementKeys) {
            KeyBinding.setKeyPressed(key.getDefaultKey(), false);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        // Устанавливаем начальное состояние спринта, если игрок уже бежит
        if (mc.player != null && sprintS.getValue()) {
            mc.player.setSprinting(mc.player.isSprinting());
        }
    }
}