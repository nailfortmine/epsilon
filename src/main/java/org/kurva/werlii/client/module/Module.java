package org.kurva.werlii.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.setting.*;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private KeyBinding keyBinding;
    private int keyCode;
    private boolean visible = true;
    private boolean workInSingleplayer = true;
    private boolean workInMultiplayer = true;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final KeybindSetting keybindSetting;
    private boolean settingsExpanded = false;

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = false;
        this.keyCode = GLFW.GLFW_KEY_UNKNOWN;
        
        this.keybindSetting = new KeybindSetting("Keybind", this, keyCode);
        addSetting(keybindSetting);
        
        addSetting(new BooleanSetting("Visible", "Show this module in the HUD", this, true));
    }

    public void toggle() {
        this.enabled = !this.enabled;
        if (this.enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public void onEnable() {
        if (mc != null && mc.inGameHud != null) {
            mc.inGameHud.setOverlayMessage(Text.literal(name + " " + Formatting.GREEN + "enabled"), false);
        }
    }

    public void onDisable() {
        if (mc != null && mc.inGameHud != null) {
            mc.inGameHud.setOverlayMessage(Text.literal(name + " " + Formatting.RED + "disabled"), false);
        }
    }

    public void onTick() {}

    public void onRender(float tickDelta) {}

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (this.enabled) {
                onEnable();
            } else {
                onDisable();
            }
        } else {
            this.enabled = enabled;
        }
    }

    public int getKeyCode() {
        return keybindSetting.getValue();
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
        this.keybindSetting.setValue(keyCode);
    }

    public void registerKeybinding(String categoryName) {
        if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
            keyBinding = new KeyBinding("key.werlii." + name.toLowerCase(), InputUtil.Type.KEYSYM, keyCode, categoryName);
        }
    }

    public KeyBinding getKeyBinding() {
        return keyBinding;
    }

    public boolean isVisible() {
        BooleanSetting visibleSetting = (BooleanSetting) getSetting("Visible");
        return visibleSetting != null ? visibleSetting.getValue() : visible;
    }

    public void setVisible(boolean visible) {
        BooleanSetting visibleSetting = (BooleanSetting) getSetting("Visible");
        if (visibleSetting != null) {
            visibleSetting.setValue(visible);
        }
        this.visible = visible;
    }

    public void addSetting(Setting<?> setting) {
        settings.add(setting);
        WerliiClient.getInstance().getSettingManager().addSetting(setting);
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public Setting<?> getSetting(String name) {
        return settings.stream()
                .filter(setting -> setting.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public KeybindSetting getKeybindSetting() {
        return keybindSetting;
    }

    public boolean isSettingsExpanded() {
        return settingsExpanded;
    }

    public void toggleSettingsExpanded() {
        settingsExpanded = !settingsExpanded;
    }

    public enum Category {
        COMBAT("Combat"),
        MOVEMENT("Movement"),
        RENDER("Render"),
        PLAYER("Player"),
        MISC("Misc"),
        EXPLOIT("Exploit"),
        UTILITY("Utility");

        private final String name;

        Category(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}

