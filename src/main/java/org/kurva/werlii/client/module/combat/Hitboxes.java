package org.kurva.werlii.client.module.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class Hitboxes extends Module {
    private final NumberSetting expandAmountS;
    private final BooleanSetting onlyPlayersS;
    private final BooleanSetting onlyEnemiesS;
    private final BooleanSetting renderS;
    
    public Hitboxes() {
        super("Hitboxes", "Expands entity hitboxes for easier hitting", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        expandAmountS = new NumberSetting("Expand Amount", "Amount to expand hitboxes by", this, 0.3, 0.0, 1.0, 0.05);
        onlyPlayersS = new BooleanSetting("Only Players", "Only expand player hitboxes", this, true);
        onlyEnemiesS = new BooleanSetting("Only Enemies", "Only expand enemy hitboxes", this, true);
        renderS = new BooleanSetting("Render", "Render expanded hitboxes", this, true);
        
        addSetting(expandAmountS);
        addSetting(onlyPlayersS);
        addSetting(onlyEnemiesS);
        addSetting(renderS);
    }
    
    public float getExpandAmount() {
        return isEnabled() ? expandAmountS.getValue().floatValue() : 0.0f;
    }
    
    public boolean shouldExpandEntity(Entity entity) {
        if (!isEnabled()) return false;
        
        if (onlyPlayersS.getValue() && !(entity instanceof PlayerEntity)) {
            return false;
        }
        
        if (onlyEnemiesS.getValue() && entity instanceof PlayerEntity) {
            return entity != mc.player;
        }
        
        return true;
    }
    
    public Box getExpandedBox(Entity entity, Box originalBox) {
        if (!shouldExpandEntity(entity)) return originalBox;
        
        float expand = getExpandAmount();
        return originalBox.expand(expand, expand, expand);
    }
    
    @Override
    public void onRender(float tickDelta) {
        if (!renderS.getValue() || mc.player == null || mc.world == null) return;
        
    }
}

