package org.kurva.werlii.client.module.combat;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HoleFiller extends Module {
    private final NumberSetting rangeS;
    private final NumberSetting delayS;
    private final NumberSetting blocksPerTickS;
    private final BooleanSetting autoSwitchS;
    private final BooleanSetting rotateS;
    private final BooleanSetting smartS;
    private final NumberSetting targetRangeS;
    private final ModeSetting blockTypeS;
    private final BooleanSetting renderS;
    
    private int placeDelay = 0;
    private final List<BlockPos> holePositions = new ArrayList<>();
    private int originalSlot = -1;
    
    public HoleFiller() {
        super("HoleFiller", "Fills holes that enemies might use for safety", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        rangeS = new NumberSetting("Range", "Maximum range to fill holes", this, 4.5, 1.0, 6.0, 0.1);
        delayS = new NumberSetting("Delay", "Delay between placements (ticks)", this, 1.0, 0.0, 10.0, 1.0);
        blocksPerTickS = new NumberSetting("Blocks Per Tick", "Maximum blocks to place per tick", this, 4.0, 1.0, 8.0, 1.0);
        autoSwitchS = new BooleanSetting("Auto Switch", "Automatically switch to blocks", this, true);
        rotateS = new BooleanSetting("Rotate", "Rotate to face placement", this, false);
        smartS = new BooleanSetting("Smart", "Only fill holes near enemies", this, true);
        targetRangeS = new NumberSetting("Target Range", "Range to check for enemies near holes", this, 3.0, 1.0, 5.0, 0.5);
        blockTypeS = new ModeSetting("Block Type", "Type of block to use", this, "Obsidian", "Obsidian", "Web", "Any");
        renderS = new BooleanSetting("Render", "Render hole positions", this, true);
        
        addSetting(rangeS);
        addSetting(delayS);
        addSetting(blocksPerTickS);
        addSetting(autoSwitchS);
        addSetting(rotateS);
        addSetting(smartS);
        addSetting(targetRangeS);
        addSetting(blockTypeS);
        addSetting(renderS);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        holePositions.clear();
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
        
        findHoles();
        
        int blocksPlaced = 0;
        int maxBlocks = blocksPerTickS.getValue().intValue();
        
        if (!holePositions.isEmpty()) {
            if (autoSwitchS.getValue()) {
                originalSlot = mc.player.getInventory().selectedSlot;
                int blockSlot = findBlockInHotbar();
                
                if (blockSlot != -1) {
                    mc.player.getInventory().selectedSlot = blockSlot;
                } else {
                    return;
                }
            } else if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
                return;
            }
            
            for (BlockPos pos : new ArrayList<>(holePositions)) {
                if (blocksPlaced >= maxBlocks) break;
                
                if (placeBlock(pos)) {
                    holePositions.remove(pos);
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
        }
    }
    
    private void findHoles() {
        holePositions.clear();
        int range = (int) Math.ceil(rangeS.getValue());
        
        BlockPos playerPos = mc.player.getBlockPos();
        
        for (int x = -range; x <= range; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    
                    if (isHole(pos)) {
                        if (smartS.getValue()) {
                            if (isEnemyNearby(pos)) {
                                holePositions.add(pos);
                            }
                        } else {
                            holePositions.add(pos);
                        }
                    }
                }
            }
        }
        
        holePositions.sort(Comparator.comparingDouble(pos -> 
            mc.player.getPos().distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5))));
    }
    
    private boolean isHole(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) {
            return false;
        }
        
        if (mc.world.getBlockState(pos.down()).isAir()) {
            return false;
        }
        
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos offset = pos.offset(dir);
            Block block = mc.world.getBlockState(offset).getBlock();
            
            if (block != Blocks.BEDROCK && block != Blocks.OBSIDIAN) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isEnemyNearby(BlockPos pos) {
        double range = targetRangeS.getValue();
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && 
                !player.isSpectator() && 
                player.isAlive()) {
                
                double distance = player.getPos().distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                if (distance <= range) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private int findBlockInHotbar() {
        switch (blockTypeS.getValue()) {
            case "Obsidian":
                return findBlockInHotbar(Blocks.OBSIDIAN);
            case "Web":
                return findItemInHotbar(Items.COBWEB);
            case "Any":
                int obsidianSlot = findBlockInHotbar(Blocks.OBSIDIAN);
                if (obsidianSlot != -1) return obsidianSlot;
                
                int webSlot = findItemInHotbar(Items.COBWEB);
                if (webSlot != -1) return webSlot;
                
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem) {
                        return i;
                    }
                }
                return -1;
            default:
                return findBlockInHotbar(Blocks.OBSIDIAN);
        }
    }
    
    private int findBlockInHotbar(Block block) {
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getStack(i).getItem();
            if (item instanceof BlockItem && ((BlockItem) item).getBlock() == block) {
                return i;
            }
        }
        return -1;
    }
    
    private int findItemInHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
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
    
    @Override
    public void onRender(float tickDelta) {
        if (!renderS.getValue() || mc.player == null || mc.world == null || holePositions.isEmpty()) return;
        
    }
}

