package org.kurva.werlii.client.module.combat;

import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class AntiKnockback extends Module {
    private final NumberSetting horizontalS;
    private final NumberSetting verticalS;
    private final BooleanSetting explosionsS;
    private final BooleanSetting attacksS;
    private final BooleanSetting waterS;
    
    public AntiKnockback() {
        super("AntiKnockback", "Reduces or cancels knockback", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        horizontalS = new NumberSetting("Horizontal", "Horizontal knockback reduction", this, 0.0, 0.0, 100.0, 5.0);
        verticalS = new NumberSetting("Vertical", "Vertical knockback reduction", this, 0.0, 0.0, 100.0, 5.0);
        explosionsS = new BooleanSetting("Explosions", "Reduce explosion knockback", this, true);
        attacksS = new BooleanSetting("Attacks", "Reduce attack knockback", this, true);
        waterS = new BooleanSetting("Water", "Reduce water push", this, true);
        
        addSetting(horizontalS);
        addSetting(verticalS);
        addSetting(explosionsS);
        addSetting(attacksS);
        addSetting(waterS);
    }
    
    
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        if (waterS.getValue() && mc.player.isTouchingWater()) {
            mc.player.setVelocity(
                mc.player.getVelocity().x * (1.0 - verticalS.getValue() / 100.0),
                mc.player.getVelocity().y * (1.0 - horizontalS.getValue() / 100.0),
                mc.player.getVelocity().z * (1.0 - verticalS.getValue() / 100.0)
            );
        }
    }
}

