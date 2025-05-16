package org.kurva.werlii.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.MinecraftClient;
import org.kurva.werlii.Werlii;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private final Gson gson;
    private final Path configDir;
    private final Path configFile;
    
    public ConfigManager() {
        gson = new GsonBuilder().setPrettyPrinting().create();
        configDir = Paths.get(MinecraftClient.getInstance().runDirectory.getPath(), "werlii");
        configFile = configDir.resolve("config.json");
        
        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            Werlii.LOGGER.error("Failed to create config directory", e);
        }
    }
    
    public void saveConfig() {
        try {
            if (!Files.exists(configFile)) {
                Files.createFile(configFile);
            }
            
            JsonObject config = new JsonObject();
            JsonObject modulesConfig = new JsonObject();
            
            JsonObject clientConfig = new JsonObject();
            clientConfig.add("disableModulesOnJoin", new JsonPrimitive(WerliiClient.getInstance().getDisableModulesOnJoin()));
            clientConfig.add("bypassMode", new JsonPrimitive(WerliiClient.getInstance().isBypassMode()));
            config.add("client", clientConfig);
            
            for (Module module : WerliiClient.getInstance().getModuleManager().getModules()) {
                JsonObject moduleConfig = new JsonObject();
                moduleConfig.add("enabled", new JsonPrimitive(module.isEnabled()));
                
                JsonObject settingsConfig = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting) {
                        settingsConfig.add(setting.getName(), new JsonPrimitive(((BooleanSetting) setting).getValue()));
                    } else if (setting instanceof NumberSetting) {
                        settingsConfig.add(setting.getName(), new JsonPrimitive(((NumberSetting) setting).getValue()));
                    } else if (setting instanceof ModeSetting) {
                        settingsConfig.add(setting.getName(), new JsonPrimitive(((ModeSetting) setting).getValue()));
                    } else if (setting instanceof KeybindSetting) {
                        settingsConfig.add(setting.getName(), new JsonPrimitive(((KeybindSetting) setting).getValue()));
                    }
                }
                
                moduleConfig.add("settings", settingsConfig);
                modulesConfig.add(module.getName(), moduleConfig);
            }
            
            config.add("modules", modulesConfig);
            
            try (Writer writer = new FileWriter(configFile.toFile())) {
                gson.toJson(config, writer);
            }
            
            Werlii.LOGGER.info("Config saved successfully");
        } catch (IOException e) {
            Werlii.LOGGER.error("Failed to save config", e);
        }
    }
    
    public void loadConfig() {
        try {
            if (!Files.exists(configFile)) {
                Werlii.LOGGER.info("Config file not found, using default settings");
                return;
            }
            
            JsonObject config = gson.fromJson(new FileReader(configFile.toFile()), JsonObject.class);
            
            if (config.has("client")) {
                JsonObject clientConfig = config.getAsJsonObject("client");
                if (clientConfig.has("disableModulesOnJoin")) {
                    WerliiClient.getInstance().setDisableModulesOnJoin(clientConfig.get("disableModulesOnJoin").getAsBoolean());
                }
                if (clientConfig.has("bypassMode")) {
                    WerliiClient.getInstance().setBypassMode(clientConfig.get("bypassMode").getAsBoolean());
                }
            }
            
            if (config.has("modules")) {
                JsonObject modulesConfig = config.getAsJsonObject("modules");
                
                for (Module module : WerliiClient.getInstance().getModuleManager().getModules()) {
                    if (modulesConfig.has(module.getName())) {
                        JsonObject moduleConfig = modulesConfig.getAsJsonObject(module.getName());
                        
                        if (moduleConfig.has("enabled")) {
                            module.setEnabled(moduleConfig.get("enabled").getAsBoolean());
                        }
                        
                        if (moduleConfig.has("settings")) {
                            JsonObject settingsConfig = moduleConfig.getAsJsonObject("settings");
                            
                            for (Setting<?> setting : module.getSettings()) {
                                if (settingsConfig.has(setting.getName())) {
                                    if (setting instanceof BooleanSetting) {
                                        ((BooleanSetting) setting).setValue(settingsConfig.get(setting.getName()).getAsBoolean());
                                    } else if (setting instanceof NumberSetting) {
                                        ((NumberSetting) setting).setValue(settingsConfig.get(setting.getName()).getAsDouble());
                                    } else if (setting instanceof ModeSetting) {
                                        ((ModeSetting) setting).setValue(settingsConfig.get(setting.getName()).getAsString());
                                    } else if (setting instanceof KeybindSetting) {
                                        ((KeybindSetting) setting).setValue(settingsConfig.get(setting.getName()).getAsInt());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Werlii.LOGGER.info("Config loaded successfully");
        } catch (Exception e) {
            Werlii.LOGGER.error("Failed to load config", e);
        }
    }
}

