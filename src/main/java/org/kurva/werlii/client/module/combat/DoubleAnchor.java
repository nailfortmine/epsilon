package org.kurva.werlii.client.module.combat;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
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

public class DoubleAnchor extends Module {
    private final NumberSetting rangeS;
    private final NumberSetting placeDelayS;
    private final BooleanSetting autoSwitchS;
    private final BooleanSetting rotateS;
    private final BooleanSetting renderS;
    private final ModeSetting targetingS;
    private final BooleanSetting autoChargeS;
    private final BooleanSetting autoGlowstoneS;
    private final BooleanSetting preventSelfS;
    
    private int placeDelay = 0;
    private BlockPos targetPos = null;
    private PlayerEntity targetPlayer = null;
    
    public DoubleAnchor() {
        super("DoubleAnchor", "Places and charges respawn anchors to damage enemies", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_L);
        this.registerKeybinding("Werlii Combat");
        
        rangeS = new NumberSetting("Range", "Maximum placement range", this, 4.5, 1.0, 6.0, 0.1);
        placeDelayS = new NumberSetting("Place Delay", "Delay between placements", this, 2.0, 0.0, 20.0, 1.0);
        autoSwitchS = new BooleanSetting("Auto Switch", "Automatically switch to anchors and glowstone", this, true);
        rotateS = new BooleanSetting("Rotate", "Rotate to face placement", this, false);
        renderS = new BooleanSetting("Render", "Render target position", this, true);
        targetingS = new ModeSetting("Targeting", "Target selection method", this, "Closest", "Closest", "Health", "Angle");
        autoChargeS = new BooleanSetting("Auto Charge", "Automatically charge anchors", this, true);
        autoGlowstoneS = new BooleanSetting("Auto Glowstone", "Automatically place glowstone for charging", this, false);
        preventSelfS = new BooleanSetting("Prevent Self Damage", "Prevent placing anchors that would damage you", this, true);
        
        addSetting(rangeS);
        addSetting(placeDelayS);
        addSetting(autoSwitchS);
        addSetting(rotateS);
        addSetting(renderS);
        addSetting(targetingS);
        addSetting(autoChargeS);
        addSetting(autoGlowstoneS);
        addSetting(preventSelfS);
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
            targetPos = findPlacementPos(targetPlayer);
            
            if (targetPos != null) {
                placeAndDetonateAnchor(targetPos);
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
                mc.player.distanceTo(player) <= rangeS.getValue() * 2) {
                players.add(player);
            }
        }
        
        if (players.isEmpty()) return null;
        
        switch (targetingS.getValue()) {
            case "Closest":
                players.sort(Comparator.comparingDouble(mc.player::distanceTo));
                break;
            case "Health":
                players.sort(Comparator.comparingDouble(PlayerEntity::getHealth));
                break;
            case "Angle":
                players.sort(Comparator.comparingDouble(this::getAngleToEntity));
                break;
        }
        
        return players.get(0);
    }
    
    private double getAngleToEntity(PlayerEntity entity) {
        Vec3d playerRotation = mc.player.getRotationVec(1.0f);
        Vec3d entityVec = new Vec3d(
            entity.getX() - mc.player.getX(),
            entity.getEyeY() - mc.player.getEyeY(),
            entity.getZ() - mc.player.getZ()
        ).normalize();
        
        return Math.toDegrees(Math.acos(playerRotation.dotProduct(entityVec)));
    }
    
    private BlockPos findPlacementPos(PlayerEntity target) {
        BlockPos playerPos = target.getBlockPos();
        List<BlockPos> possiblePositions = new ArrayList<>();
        
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    
                    BlockPos pos = playerPos.add(x, y, z);
                    
                    if (isValidPosition(pos) && 
                        mc.player.getPos().distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) <= rangeS.getValue()) {
                        
                        if (preventSelfS.getValue() && wouldDamageSelf(pos)) {
                            continue;
                        }
                        
                        possiblePositions.add(pos);
                    }
                }
            }
        }
        
        if (possiblePositions.isEmpty()) return null;
        
        possiblePositions.sort(Comparator.comparingDouble(pos -> 
            target.getPos().distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5))));
        
        return possiblePositions.get(0);
    }
    
    private boolean isValidPosition(BlockPos pos) {
        return mc.world.getBlockState(pos).isReplaceable() && 
               mc.world.getBlockState(pos.up()).isReplaceable() &&
               mc.world.getBlockState(pos.down()).isFullCube(mc.world, pos.down());
    }
    
    private boolean wouldDamageSelf(BlockPos pos) {
        double distance = mc.player.getPos().distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        return distance < 6.0;
    }
    
    private void placeAndDetonateAnchor(BlockPos pos) {
        int anchorSlot = findItemInHotbar(Items.RESPAWN_ANCHOR);
        int glowstoneSlot = findItemInHotbar(Items.GLOWSTONE);
        
        if (anchorSlot == -1) return;
        
        int originalSlot = -1;
        if (autoSwitchS.getValue()) {
            originalSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = anchorSlot;
        } else if (mc.player.getMainHandStack().getItem() != Items.RESPAWN_ANCHOR) {
            return;
        }
        
        if (rotateS.getValue()) {
            rotateToPos(pos);
        }
        
        placeBlock(pos);
        placeDelay = placeDelayS.getValue().intValue();
        
        if (autoChargeS.getValue()) {
            if (autoSwitchS.getValue() && glowstoneSlot != -1) {
                mc.player.getInventory().selectedSlot = glowstoneSlot;
            } else if (mc.player.getMainHandStack().getItem() != Items.GLOWSTONE) {
                if (originalSlot != -1) {
                    mc.player.getInventory().selectedSlot = originalSlot;
                }
                return;
            }
            
            interactWithBlock(pos);
            placeDelay = placeDelayS.getValue().intValue();
            
            if (autoSwitchS.getValue()) {
                mc.player.getInventory().selectedSlot = anchorSlot;
            } else if (mc.player.getMainHandStack().getItem() != Items.RESPAWN_ANCHOR) {
                if (originalSlot != -1) {
                    mc.player.getInventory().selectedSlot = originalSlot;
                }
                return;
            }
            
            interactWithBlock(pos);
            placeDelay = placeDelayS.getValue().intValue();
        }
        
        if (originalSlot != -1) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }
    }
    
    private int findItemInHotbar(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }
    
    private void placeBlock(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            
            if (mc.world.getBlockState(neighbor).isReplaceable()) {
                continue;
            }
            
            Direction side = dir.getOpposite();
            
            Vec3d hitVec = new Vec3d(neighbor.getX() + 0.5 + side.getOffsetX() * 0.5,
                                    neighbor.getY() + 0.5 + side.getOffsetY() * 0.5,
                                    neighbor.getZ() + 0.5 + side.getOffsetZ() * 0.5);
            
            BlockHitResult hitResult = new BlockHitResult(hitVec, side, neighbor, false);
            
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
            mc.player.swingHand(Hand.MAIN_HAND);
            
            return;
        }
    }
    
    private void interactWithBlock(BlockPos pos) {
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);
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
        if (!renderS.getValue() || mc.player == null || mc.world == null || targetPos == null) return;
        
    }
}

