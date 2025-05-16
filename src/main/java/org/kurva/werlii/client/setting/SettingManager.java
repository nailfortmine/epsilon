package org.kurva.werlii.client.setting;

import org.kurva.werlii.client.module.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SettingManager {
    private final List<Setting<?>> settings = new ArrayList<>();
    
    public void addSetting(Setting<?> setting) {
        settings.add(setting);
    }
    
    public List<Setting<?>> getSettings() {
        return settings;
    }
    
    public List<Setting<?>> getSettingsForModule(Module module) {
        return settings.stream()
                .filter(setting -> setting.getParent() == module)
                .collect(Collectors.toList());
    }
    
    public Setting<?> getSettingByName(Module module, String name) {
        return settings.stream()
                .filter(setting -> setting.getParent() == module && setting.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}

