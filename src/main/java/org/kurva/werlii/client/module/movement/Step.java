package org.kurva.werlii.client.module.movement;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.kurva.werlii.client.util.IStepHeight;
import org.lwjgl.glfw.GLFW;

public class Step extends Module {
    private final NumberSetting heightS;
    private final ModeSetting modeS;
    private final BooleanSetting entityStepS;
    private final BooleanSetting bypassS;
    private float originalStepHeight;
    
    public Step() {
        super("Step", "Allows you to step up blocks", Category.MOVEMENT);
        this.setKeyCode(GLFW.GLFW_KEY_M);
        this.registerKeybinding("Werlii Movement");
        
        heightS = new NumberSetting("Height", "Maximum step height", this, 1.0, 0.5, 2.5, 0.1);
        modeS = new ModeSetting("Mode", "Step method", this, "Vanilla", "Vanilla", "NCP", "Packet");
        entityStepS = new BooleanSetting("Entity Step", "Apply step to riding entities", this, true);
        bypassS = new BooleanSetting("Bypass Mode", "Use techniques to bypass anti-cheat", this, false);
        
        addSetting(heightS);
        addSetting(modeS);
        addSetting(entityStepS);
        addSetting(bypassS);
    }
    
    private void modifyStepHeight(IStepHeight entity, float height) {
        entity.setCustomStepHeight(height);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player instanceof IStepHeight) {
            originalStepHeight = ((IStepHeight) mc.player).getCustomStepHeight();
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        if (mc.player instanceof IStepHeight) {
            modifyStepHeight((IStepHeight) mc.player, 0.6f);
            
            if (entityStepS.getValue() && mc.player.getVehicle() instanceof IStepHeight) {
                modifyStepHeight((IStepHeight) mc.player.getVehicle(), 0.6f);
            }
        }
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || !(mc.player instanceof IStepHeight)) return;
        
        switch (modeS.getValue()) {
            case "Vanilla":
                modifyStepHeight((IStepHeight) mc.player, heightS.getValue().floatValue());
                break;
                
            case "NCP":
                modifyStepHeight((IStepHeight) mc.player, Math.min(heightS.getValue().floatValue(), 1.0f));
                break;
                
            case "Packet":
                handlePacketStep();
                break;
        }
        
        if (entityStepS.getValue() && mc.player.getVehicle() instanceof IStepHeight) {
            modifyStepHeight((IStepHeight) mc.player.getVehicle(), heightS.getValue().floatValue());
        }
    }
    
    private void handlePacketStep() {
        if (!(mc.player instanceof IStepHeight)) return;
        
        modifyStepHeight((IStepHeight) mc.player, 0.6f);
        
        if (mc.player.horizontalCollision && !mc.player.verticalCollision) {
            double stepHeight = getStepHeight();
            
            if (stepHeight > 0 && stepHeight <= heightS.getValue()) {
                double startY = mc.player.getY();
                double endY = startY + stepHeight;
                
                if (bypassS.getValue()) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), startY + 0.42, mc.player.getZ(), false));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), startY + 0.75, mc.player.getZ(), false));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), endY, mc.player.getZ(), false));
                } else {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), endY, mc.player.getZ(), false));
                }
                
                mc.player.setPosition(mc.player.getX(), endY, mc.player.getZ());
            }
        }
    }
    
    private double getStepHeight() {
        return 1.0;
    }
}

