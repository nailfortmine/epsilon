package org.kurva.werlii.client.module.combat;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
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

public class AutoCity extends Module {
    private final NumberSetting rangeS;
    private final BooleanSetting autoSwitchS;
    private final BooleanSetting rotateS;
    private final BooleanSetting instantMineS;
    private final ModeSetting targetModeS;
    private final BooleanSetting renderS;
    private final BooleanSetting autoDisableS;
    
    private PlayerEntity targetPlayer = null;
    private BlockPos targetBlock = null;
    private int originalSlot = -1;
    
    public AutoCity() {
        super("AutoCity", "Automatically mines blocks next to enemies", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        rangeS = new NumberSetting("Range", "Maximum range to city players", this, 5.0, 1.0, 6.0, 0.1);
        autoSwitchS = new BooleanSetting("Auto Switch", "Automatically switch to pickaxe", this, true);
        rotateS = new BooleanSetting("Rotate", "Rotate to face mining", this, false);
        instantMineS = new BooleanSetting("Instant Mine", "Use packet mining for instant breaking", this, true);
        targetModeS = new ModeSetting("Target Mode", "How to select targets", this, "Closest", "Closest", "Looking", "Health");
        renderS = new BooleanSetting("Render", "Render target block", this, true);
        autoDisableS = new BooleanSetting("Auto Disable", "Disable after starting to mine", this, true);
        
        addSetting(rangeS);
        addSetting(autoSwitchS);
        addSetting(rotateS);
        addSetting(instantMineS);
        addSetting(targetModeS);
        addSetting(renderS);
        addSetting(autoDisableS);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        targetPlayer = null;
        targetBlock = null;
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
        
        if (targetBlock != null) {
            if (autoDisableS.getValue()) {
                setEnabled(false);
            }
            return;
        }
        
        targetPlayer = findTargetPlayer();
        
        if (targetPlayer != null) {
            targetBlock = findTargetBlock();
            
            if (targetBlock != null) {
                if (autoSwitchS.getValue()) {
                    originalSlot = mc.player.getInventory().selectedSlot;
                    int pickaxeSlot = findPickaxeSlot();
                    
                    if (pickaxeSlot != -1) {
                        mc.player.getInventory().selectedSlot = pickaxeSlot;
                    } else {
                        targetBlock = null;
                        return;
                    }
                } else if (!(mc.player.getMainHandStack().getItem() instanceof PickaxeItem)) {
                    targetBlock = null;
                    return;
                }
                
                if (rotateS.getValue()) {
                    rotateToPos(targetBlock);
                }
                
                if (instantMineS.getValue()) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                        targetBlock,
                        Direction.UP
                    ));
                    
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                        targetBlock,
                        Direction.UP
                    ));
                    
                    mc.player.swingHand(Hand.MAIN_HAND);
                } else {
                    mc.interactionManager.updateBlockBreakingProgress(targetBlock, Direction.UP);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                
                if (autoDisableS.getValue()) {
                    setEnabled(false);
                }
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
    
    private BlockPos findTargetBlock() {
        BlockPos playerPos = targetPlayer.getBlockPos();
        List<BlockPos> possibleBlocks = new ArrayList<>();
        
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos pos = playerPos.offset(dir);
            Block block = mc.world.getBlockState(pos).getBlock();
            
            if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) {
                if (block == Blocks.BEDROCK) continue;
                
                double distance = mc.player.getPos().distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                if (distance <= rangeS.getValue()) {
                    possibleBlocks.add(pos);
                }
            }
        }
        
        if (possibleBlocks.isEmpty()) return null;
        
        possibleBlocks.sort(Comparator.comparingDouble(pos -> 
            mc.player.getPos().distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5))));
        
        return possibleBlocks.get(0);
    }
    
    private int findPickaxeSlot() {
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (item instanceof PickaxeItem) {
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
        if (!renderS.getValue() || mc.player == null || mc.world == null || targetBlock == null) return;
        
    }
}

