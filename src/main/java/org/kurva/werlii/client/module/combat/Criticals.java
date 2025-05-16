package org.kurva.werlii.client.module.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.ModeSetting;
import org.lwjgl.glfw.GLFW;

public class Criticals extends Module {
    private final ModeSetting modeS;
    private boolean attacking = false;
    
    public Criticals() {
        super("Criticals", "Always deals critical hits", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        modeS = new ModeSetting("Mode", "Critical hit method", this, "Packet", "Packet", "Jump", "MiniJump");
        
        addSetting(modeS);
    }
    
    public void onAttack(Entity target) {
        if (!isEnabled() || mc.player == null || !mc.player.isOnGround() || attacking) return;
        
        if (target instanceof LivingEntity) {
            attacking = true;
            
            switch (modeS.getValue()) {
                case "Packet":
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY() + 0.11, mc.player.getZ(), false));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY() + 0.1100013579, mc.player.getZ(), false));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY() + 0.0000013579, mc.player.getZ(), false));
                    break;
                    
                case "Jump":
                    mc.player.jump();
                    break;
                    
                case "MiniJump":
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.25, mc.player.getVelocity().z);
                    break;
            }
            
            attacking = false;
        }
    }
}

