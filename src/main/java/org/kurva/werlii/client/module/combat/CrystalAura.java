package org.kurva.werlii.client.module.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CrystalAura extends Module {
    private int breakDelay = 0;
    private final int maxBreakDelay = 2;
    
    public CrystalAura() {
        super("CrystalAura", "Automatically breaks nearby end crystals", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_R);
        this.registerKeybinding("Werlii Combat");
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        
        if (mc != null && mc.player != null && !mc.isInSingleplayer()) {
            mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cWarning: CrystalAura may cause server kicks or bans."), false);
        }
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        if (breakDelay > 0) {
            breakDelay--;
            return;
        }
        
        List<EndCrystalEntity> crystals = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
                .filter(entity -> entity instanceof EndCrystalEntity)
                .map(entity -> (EndCrystalEntity) entity)
                .filter(crystal -> mc.player.distanceTo(crystal) <= 4.5)
                .sorted(Comparator.comparingDouble(mc.player::distanceTo))
                .collect(Collectors.toList());
        
        if (!crystals.isEmpty()) {
            EndCrystalEntity crystal = crystals.get(0);
            
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            
            breakDelay = maxBreakDelay;
        }
    }
}

