package org.kurva.werlii.client.setting;

import org.kurva.werlii.client.module.Module;

public class BooleanSetting extends Setting<Boolean> {
    
    public BooleanSetting(String name, String description, Module parent, boolean defaultValue) {
        super(name, description, parent, defaultValue);
    }
    
    public void toggle() {
        setValue(!getValue());
    }
}

