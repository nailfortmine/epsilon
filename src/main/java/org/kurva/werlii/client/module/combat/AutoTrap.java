package org.kurva.werlii.client.module.combat;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.kurva.werlii.client.util.EntityUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoTrap extends Module {
    private final NumberSetting rangeS;
    private final NumberSetting delayS;
    private final NumberSetting blocksPerTickS;
    private final BooleanSetting autoSwitchS;
    private final BooleanSetting rotateS;
    private final BooleanSetting topBlockS;
    private final BooleanSetting fullTrapS;
    private final ModeSetting targetModeS;
    private final BooleanSetting renderS;
    
    private int placeDelay = 0;
    private PlayerEntity targetPlayer = null;
    private final List<BlockPos> trapPositions = new ArrayList<>();
    private int originalSlot = -1;
    
    public AutoTrap() {
        super("AutoTrap", "Automatically traps enemies in obsidian", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        rangeS = new NumberSetting("Range", "Maximum range to trap players", this, 4.5, 1.0, 6.0, 0.1);
        delayS = new NumberSetting("Delay", "Delay between placements (ticks)", this, 1.0, 0.0, 10.0, 1.0);
        blocksPerTickS = new NumberSetting("Blocks Per Tick", "Maximum blocks to place per tick", this, 4.0, 1.0, 8.0, 1.0);
        autoSwitchS = new BooleanSetting("Auto Switch", "Automatically switch to obsidian", this, true);
        rotateS = new BooleanSetting("Rotate", "Rotate to face placement", this, false);
        topBlockS = new BooleanSetting("Top Block", "Place block on top", this, true);
        fullTrapS = new BooleanSetting("Full Trap", "Create a complete trap (more blocks)", this, false);
        targetModeS = new ModeSetting("Target Mode", "How to select targets", this, "Closest", "Closest", "Looking", "Health");
        renderS = new BooleanSetting("Render", "Render trap positions", this, true);
        
        addSetting(rangeS);
        addSetting(delayS);
        addSetting(blocksPerTickS);
        addSetting(autoSwitchS);
        addSetting(rotateS);
        addSetting(topBlockS);
        addSetting(fullTrapS);
        addSetting(targetModeS);
        addSetting(renderS);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        targetPlayer = null;
        trapPositions.clear();
        placeDelay = 0;
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        if (autoSwitchS.getValue() && originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = originalSlot;
            originalSlot = -1;
        }
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        if (placeDelay > 0) {
            placeDelay--;
            return;
        }
        
        targetPlayer = findTargetPlayer();
        
        if (targetPlayer != null) {
            calculateTrapPositions();
            
            int blocksPlaced = 0;
            int maxBlocks = blocksPerTickS.getValue().intValue();
            
            if (autoSwitchS.getValue()) {
                originalSlot = mc.player.getInventory().selectedSlot;
                int obsidianSlot = findBlockInHotbar(Blocks.OBSIDIAN);
                
                if (obsidianSlot != -1) {
                    mc.player.getInventory().selectedSlot = obsidianSlot;
                } else {
                    return;
                }
            } else if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
                return;
            }
            
            for (BlockPos pos : new ArrayList<>(trapPositions)) {
                if (blocksPlaced >= maxBlocks) break;
                
                if (placeBlock(pos)) {
                    trapPositions.remove(pos);
                    blocksPlaced++;
                }
            }
            
            if (autoSwitchS.getValue() && originalSlot != -1 && blocksPlaced > 0) {
                mc.player.getInventory().selectedSlot = originalSlot;
                originalSlot = -1;
            }
            
            if (blocksPlaced > 0) {
                placeDelay = delayS.getValue().intValue();
            }
            
            if (trapPositions.isEmpty()) {
                setEnabled(false);
            }
        }
    }
    
    private PlayerEntity findTargetPlayer() {
        List<PlayerEntity> players = new ArrayList<>();
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && 
                !player.isSpectator() && 
                player.isAlive() && 
                EntityUtil.isValidTarget(player) && 
                mc.player.distanceTo(player) <= rangeS.getValue()) {
                players.add(player);
            }
        }
        
        if (players.isEmpty()) return null;
        
        switch (targetModeS.getValue()) {
            case "Closest":
                players.sort(Comparator.comparingDouble(mc.player::distanceTo));
                break;
            case "Health":
                players.sort(Comparator.comparingDouble(PlayerEntity::getHealth));
                break;
            case "Looking":
                if (mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult) {
                    Entity entity = ((net.minecraft.util.hit.EntityHitResult) mc.crosshairTarget).getEntity();
                    if (entity instanceof PlayerEntity && entity != mc.player) {
                        return (PlayerEntity) entity;
                    }
                }
                players.sort(Comparator.comparingDouble(mc.player::distanceTo));
                break;
        }
        
        return players.get(0);
    }
    
    private void calculateTrapPositions() {
        trapPositions.clear();
        
        BlockPos playerPos = targetPlayer.getBlockPos();
        
        trapPositions.add(playerPos.north());
        trapPositions.add(playerPos.east());
        trapPositions.add(playerPos.south());
        trapPositions.add(playerPos.west());
        
        if (topBlockS.getValue()) {
            trapPositions.add(playerPos.up(2));
        }
        
        if (fullTrapS.getValue()) {
            trapPositions.add(playerPos.north().up());
            trapPositions.add(playerPos.east().up());
            trapPositions.add(playerPos.south().up());
            trapPositions.add(playerPos.west().up());
            trapPositions.add(playerPos.up());
        }
        
        trapPositions.removeIf(pos -> !mc.world.getBlockState(pos).isReplaceable());
    }
    
    private boolean placeBlock(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isReplaceable()) {
            return false;
        }
        
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            
            if (mc.world.getBlockState(neighbor).isReplaceable()) {
                continue;
            }
            
            Direction side = dir.getOpposite();
            
            if (rotateS.getValue()) {
                rotateToPos(pos);
            }
            
            Vec3d hitVec = new Vec3d(neighbor.getX() + 0.5 + side.getOffsetX() * 0.5,
                                    neighbor.getY() + 0.5 + side.getOffsetY() * 0.5,
                                    neighbor.getZ() + 0.5 + side.getOffsetZ() * 0.5);
            
            BlockHitResult hitResult = new BlockHitResult(hitVec, side, neighbor, false);
            
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
            mc.player.swingHand(Hand.MAIN_HAND);
            
            return true;
        }
        
        return false;
    }
    
    private int findBlockInHotbar(net.minecraft.block.Block block) {
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (item instanceof BlockItem && ((BlockItem) item).getBlock() == block) {
                return i;
            }
        }
        return -1;
    }
    
    private void rotateToPos(BlockPos pos) {
        double x = pos.getX() + 0.5 - mc.player.getX();
        double z = pos.getZ() + 0.5 - mc.player.getZ();
        double y = pos.getY() + 0.5 - mc.player.getEyeY();
        
        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, dist));
        
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }
    
    @Override
    public void onRender(float tickDelta) {
        if (!renderS.getValue() || mc.player == null || mc.world == null || trapPositions.isEmpty()) return;
        
    }
}

