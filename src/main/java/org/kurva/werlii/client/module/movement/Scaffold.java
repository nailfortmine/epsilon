package org.kurva.werlii.client.module.movement;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Scaffold extends Module {
    private final BooleanSetting rotateS;
    private final BooleanSetting safeWalkS;
    private final BooleanSetting towerS;
    private final BooleanSetting autoSwitchS;
    private final NumberSetting delayS;
    private final ModeSetting modeS;
    private final BooleanSetting renderS;
    
    private int placeDelay = 0;
    private int originalSlot = -1;
    private boolean isSneaking = false;
    
    public Scaffold() {
        super("Scaffold", "Automatically places blocks under you", Category.MOVEMENT);
        this.setKeyCode(GLFW.GLFW_KEY_X);
        this.registerKeybinding("Werlii Movement");
        
        rotateS = new BooleanSetting("Rotate", "Rotate to face placement", this, false);
        safeWalkS = new BooleanSetting("Safe Walk", "Prevents falling off edges", this, true);
        towerS = new BooleanSetting("Tower", "Build upwards when jumping", this, true);
        autoSwitchS = new BooleanSetting("Auto Switch", "Automatically switch to blocks", this, true);
        delayS = new NumberSetting("Delay", "Delay between placements (ticks)", this, 0.0, 0.0, 10.0, 1.0);
        modeS = new ModeSetting("Mode", "Placement pattern", this, "Normal", "Normal", "Expand", "Smart");
        renderS = new BooleanSetting("Render", "Render placement preview", this, true);
        
        addSetting(rotateS);
        addSetting(safeWalkS);
        addSetting(towerS);
        addSetting(autoSwitchS);
        addSetting(delayS);
        addSetting(modeS);
        addSetting(renderS);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        placeDelay = 0;
        originalSlot = -1;
        isSneaking = false;
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        if (originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = originalSlot;
            originalSlot = -1;
        }
        
        if (isSneaking && mc.player != null) {
            mc.options.sneakKey.setPressed(false);
            isSneaking = false;
        }
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        if (placeDelay > 0) {
            placeDelay--;
            return;
        }
        
        List<BlockPos> positions = getPlacementPositions();
        
        if (!positions.isEmpty()) {
            int blockSlot = findBlockInHotbar();
            
            if (blockSlot != -1) {
                if (autoSwitchS.getValue()) {
                    originalSlot = mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = blockSlot;
                }
                
                for (BlockPos pos : positions) {
                    if (placeBlock(pos)) {
                        placeDelay = delayS.getValue().intValue();
                        break;
                    }
                }
                
                if (autoSwitchS.getValue() && originalSlot != -1) {
                    mc.player.getInventory().selectedSlot = originalSlot;
                    originalSlot = -1;
                }
            }
        }
        
        if (towerS.getValue() && mc.options.jumpKey.isPressed() && mc.player.isOnGround()) {
            if (mc.player.getInventory().getMainHandStack().getItem() instanceof BlockItem) {
                mc.player.setVelocity(mc.player.getVelocity().x, 0.42, mc.player.getVelocity().z);
            }
        }
    }
    
    private List<BlockPos> getPlacementPositions() {
        List<BlockPos> positions = new ArrayList<>();
        
        BlockPos playerPos = mc.player.getBlockPos().down();
        
        switch (modeS.getValue()) {
            case "Normal":
                positions.add(playerPos);
                break;
                
            case "Expand":
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        positions.add(playerPos.add(x, 0, z));
                    }
                }
                break;
                
            case "Smart":
                positions.add(playerPos);
                
                double yaw = Math.toRadians(mc.player.getYaw());
                int xOffset = (int) -Math.sin(yaw);
                int zOffset = (int) Math.cos(yaw);
                
                if (xOffset != 0 || zOffset != 0) {
                    positions.add(playerPos.add(xOffset, 0, zOffset));
                }
                break;
        }
        
        positions.removeIf(pos -> !mc.world.getBlockState(pos).isReplaceable());
        
        return positions;
    }
    
    private int findBlockInHotbar() {
        if (mc.player.getMainHandStack().getItem() instanceof BlockItem) {
            return mc.player.getInventory().selectedSlot;
        }
        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isValidBlock(stack)) {
                return i;
            }
        }
        
        return -1;
    }
    
    private boolean isValidBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem)) return false;
        
        Block block = ((BlockItem) stack.getItem()).getBlock();
        
        return block != Blocks.SLIME_BLOCK &&
               block != Blocks.TNT &&
               block != Blocks.SAND &&
               block != Blocks.GRAVEL &&
               block != Blocks.WATER &&
               block != Blocks.LAVA &&
               block != Blocks.FIRE;
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
    
    public boolean shouldPreventFalling() {
        return isEnabled() && safeWalkS.getValue();
    }
    
    @Override
    public void onRender(float tickDelta) {
        if (!renderS.getValue() || mc.player == null || mc.world == null) return;
        
    }
}

