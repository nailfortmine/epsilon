package org.kurva.werlii.client.setting;

import org.kurva.werlii.client.module.Module;

public class NumberSetting extends Setting<Double> {
    private final double increment;
    
    public NumberSetting(String name, String description, Module parent, double defaultValue, double minValue, double maxValue, double increment) {
        super(name, description, parent, defaultValue, minValue, maxValue);
        this.increment = increment;
    }
    
    public double getIncrement() {
        return increment;
    }
    
    public void increment() {
        double newValue = getValue() + increment;
        if (newValue <= getMaxValue()) {
            setValue(newValue);
        } else {
            setValue(getMaxValue());
        }
    }
    
    public void decrement() {
        double newValue = getValue() - increment;
        if (newValue >= getMinValue()) {
            setValue(newValue);
        } else {
            setValue(getMinValue());
        }
    }
    
    @Override
    public void setValue(Double value) {
        double clampedValue = Math.max(getMinValue(), Math.min(getMaxValue(), value));
        super.setValue(clampedValue);
    }
}

