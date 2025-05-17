package org.kurva.werlii.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.kurva.werlii.Werlii;
import org.kurva.werlii.client.module.ModuleManager;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.SettingManager;
import org.kurva.werlii.client.ui.HudManager;
import org.kurva.werlii.client.ui.HudRenderer;
import org.kurva.werlii.client.util.ConfigManager;
import org.kurva.werlii.client.util.KeyBindingHandler;
import org.kurva.werlii.client.util.font.FontManager;

public class WerliiClient implements ClientModInitializer {
    private static WerliiClient INSTANCE;
    private ModuleManager moduleManager;
    private SettingManager settingManager;
    private HudManager hudManager;
    private ConfigManager configManager;
    private KeyBindingHandler keyBindingHandler;
    private boolean disableModulesOnJoin = false;
    private boolean bypassMode = false;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        Werlii.LOGGER.info("Initializing Werlii Client");

        // Initialize managers
        settingManager = new SettingManager();
        moduleManager = new ModuleManager();
        hudManager = new HudManager();
        configManager = new ConfigManager();
        keyBindingHandler = new KeyBindingHandler();

        // Register keybindings
        keyBindingHandler.registerKeybindings();

        // Register client tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            moduleManager.onTick(client);
        });

        // Register HUD rendering using Fabric API
        HudRenderCallback.EVENT.register(new HudRenderer());

        // Register connection events
        registerConnectionEvents();

        // Load config
        configManager.loadConfig();

        Werlii.LOGGER.info("Werlii Client initialized successfully");
        Werlii.LOGGER.info("AcManager feature initialized");
    }

    private void registerConnectionEvents() {
        // Handle server join event
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Werlii.LOGGER.info("Connected to server: " + handler.getServerInfo().address);

            if (client.player != null) {
                client.player.sendMessage(Text.literal("§8[§bWerlii§8] §aWerlii client activated. All modules are available."), false);
                client.player.sendMessage(Text.literal("§8[§bWerlii§8] §aPress RIGHT SHIFT to open ClickGUI."), false);
                client.player.sendMessage(Text.literal("§8[§bWerlii§8] §aAcManager available in main menu."), false);
            }

            if (bypassMode) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§8[§bWerlii§8] §aBypass mode active. Anti-cheat evasion enabled."), false);
                }
            }
        });

        // Handle server disconnect event
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Werlii.LOGGER.info("Disconnected from server");

            // Save config on disconnect
            configManager.saveConfig();
        });
    }

    public static WerliiClient getInstance() {
        return INSTANCE;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public SettingManager getSettingManager() {
        return settingManager;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public KeyBindingHandler getKeyBindingHandler() {
        return keyBindingHandler;
    }

    public void setDisableModulesOnJoin(boolean disable) {
        this.disableModulesOnJoin = disable;
    }

    public boolean getDisableModulesOnJoin() {
        return disableModulesOnJoin;
    }

    public boolean isBypassMode() {
        return bypassMode;
    }

    public void setBypassMode(boolean bypass) {
        this.bypassMode = bypass;

        // Apply bypass settings to all modules
        if (moduleManager != null) {
            moduleManager.getModules().forEach(module -> {
                // Apply bypass-specific settings
                if (module.getSetting("Bypass Mode") instanceof BooleanSetting) {
                    ((BooleanSetting) module.getSetting("Bypass Mode")).setValue(bypass);
                }
            });
        }
    }

    public void toggleBypassMode() {
        setBypassMode(!bypassMode);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Bypass Mode: " +
                    (bypassMode ? "§aEnabled" : "§cDisabled")), false);
        }
    }
}