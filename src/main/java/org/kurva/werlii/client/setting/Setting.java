package org.kurva.werlii.client.setting;

import org.kurva.werlii.client.module.Module;

import java.util.function.Predicate;

public class Setting<T> {
    private final String name;
    private final String description;
    private T value;
    private final T defaultValue;
    private final T minValue;
    private final T maxValue;
    private final Module parent;
    private boolean visible = true;
    private Predicate<T> restriction;

    public Setting(String name, String description, Module parent, T defaultValue) {
        this.name = name;
        this.description = description;
        this.parent = parent;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.minValue = null;
        this.maxValue = null;
    }

    public Setting(String name, String description, Module parent, T defaultValue, T minValue, T maxValue) {
        this.name = name;
        this.description = description;
        this.parent = parent;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        if (restriction != null && !restriction.test(value)) {
            return;
        }
        this.value = value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public T getMinValue() {
        return minValue;
    }

    public T getMaxValue() {
        return maxValue;
    }

    public Module getParent() {
        return parent;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setRestriction(Predicate<T> restriction) {
        this.restriction = restriction;
    }

    public void reset() {
        this.value = defaultValue;
    }
}

