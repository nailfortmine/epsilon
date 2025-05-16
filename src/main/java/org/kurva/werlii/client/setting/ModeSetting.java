package org.kurva.werlii.client.setting;

import org.kurva.werlii.client.module.Module;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;
    
    public ModeSetting(String name, String description, Module parent, String defaultValue, String... modes) {
        super(name, description, parent, defaultValue);
        this.modes = Arrays.asList(modes);
        
        if (!this.modes.contains(defaultValue)) {
            throw new IllegalArgumentException("Default value must be one of the modes");
        }
    }
    
    public List<String> getModes() {
        return modes;
    }
    
    public void cycle() {
        int index = modes.indexOf(getValue());
        index = (index + 1) % modes.size();
        setValue(modes.get(index));
    }
    
    public boolean is(String mode) {
        return getValue().equals(mode);
    }
}

