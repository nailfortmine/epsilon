package org.kurva.werlii.client.module.combat;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BurrowDetector extends Module {
    private final NumberSetting rangeS;
    private final BooleanSetting notifyS;
    private final BooleanSetting renderS;
    
    private final Map<String, Boolean> burrowedPlayers = new HashMap<>();
    
    public BurrowDetector() {
        super("BurrowDetector", "Detects when players are burrowed in blocks", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        rangeS = new NumberSetting("Range", "Detection range", this, 20.0, 5.0, 50.0, 5.0);
        notifyS = new BooleanSetting("Notify", "Send chat notifications", this, true);
        renderS = new BooleanSetting("Render", "Render burrowed players", this, true);
        
        addSetting(rangeS);
        addSetting(notifyS);
        addSetting(renderS);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        burrowedPlayers.clear();
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        List<PlayerEntity> nearbyPlayers = new ArrayList<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && 
                !player.isSpectator() && 
                player.isAlive() && 
                mc.player.distanceTo(player) <= rangeS.getValue()) {
                nearbyPlayers.add(player);
            }
        }
        
        for (PlayerEntity player : nearbyPlayers) {
            boolean wasBurrowed = burrowedPlayers.getOrDefault(player.getName().getString(), false);
            boolean isBurrowed = isPlayerBurrowed(player);
            
            burrowedPlayers.put(player.getName().getString(), isBurrowed);
            
            if (notifyS.getValue() && wasBurrowed != isBurrowed) {
                if (isBurrowed) {
                    mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §c" + player.getName().getString() + " §7has burrowed!"), false);
                } else {
                    mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §c" + player.getName().getString() + " §7is no longer burrowed!"), false);
                }
            }
        }
        
        List<String> playersToRemove = new ArrayList<>();
        for (String playerName : burrowedPlayers.keySet()) {
            boolean found = false;
            for (PlayerEntity player : nearbyPlayers) {
                if (player.getName().getString().equals(playerName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                playersToRemove.add(playerName);
            }
        }
        
        for (String playerName : playersToRemove) {
            burrowedPlayers.remove(playerName);
        }
    }
    
    private boolean isPlayerBurrowed(PlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        Block block = mc.world.getBlockState(playerPos).getBlock();
        
        return block != Blocks.AIR && 
               block != Blocks.WATER && 
               block != Blocks.LAVA && 
               block != Blocks.FIRE;
    }
    
    @Override
    public void onRender(float tickDelta) {
        if (!renderS.getValue() || mc.player == null || mc.world == null) return;
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            String playerName = player.getName().getString();
            if (burrowedPlayers.getOrDefault(playerName, false)) {
            }
        }
    }
}

