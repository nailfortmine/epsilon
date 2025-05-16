package org.kurva.werlii.client.module.combat;

import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
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

public class BedAura extends Module {
    private final NumberSetting rangeS;
    private final NumberSetting delayS;
    private final BooleanSetting autoSwitchS;
    private final BooleanSetting rotateS;
    private final BooleanSetting onlyNetherEndS;
    private final ModeSetting targetModeS;
    private final BooleanSetting renderS;
    private final BooleanSetting instantBreakS;
    
    private int placeDelay = 0;
    private BlockPos targetPos = null;
    private Direction targetDirection = null;
    private int originalSlot = -1;
    
    public BedAura() {
        super("BedAura", "Automatically places and detonates beds", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        // Add settings
        rangeS = new NumberSetting("Range", "Maximum range for bed placement", this, 4.5, 1.0, 6.0, 0.1);
        delayS = new NumberSetting("Delay", "Delay between bed placements (ticks)", this, 10.0, 1.0, 40.0, 1.0);
        autoSwitchS = new BooleanSetting("Auto Switch", "Automatically switch to beds", this, true);
        rotateS = new BooleanSetting("Rotate", "Rotate to face bed placement", this, false);
        onlyNetherEndS = new BooleanSetting("Only Nether/End", "Only work in Nether and End", this, true);
        targetModeS = new ModeSetting("Target Mode", "How to select targets", this, "Closest", "Closest", "Health", "Damage");
        renderS = new BooleanSetting("Render", "Render bed placement positions", this, true);
        instantBreakS = new BooleanSetting("Instant Break", "Instantly break beds after placing", this, true);
        
        addSetting(rangeS);
        addSetting(delayS);
        addSetting(autoSwitchS);
        addSetting(rotateS);
        addSetting(onlyNetherEndS);
        addSetting(targetModeS);
        addSetting(renderS);
        addSetting(instantBreakS);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        placeDelay = 0;
        targetPos = null;
        targetDirection = null;
        originalSlot = -1;
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        // Switch back to original slot
        if (originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = originalSlot;
            originalSlot = -1;
        }
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        // Check if in correct dimension
        if (onlyNetherEndS.getValue()) {
            boolean isNether = mc.world.getRegistryKey().getValue().getPath().contains("nether");
            boolean isEnd = mc.world.getRegistryKey().getValue().getPath().contains("end");
            if (!isNether && !isEnd) return;
        }
        
        // Handle delay
        if (placeDelay > 0) {
            placeDelay--;
            return;
        }
        
        // Find target player
        PlayerEntity target = findTargetPlayer();
        
        if (target != null) {
            // Find placement position
            findBedPlacement(target);
            
            if (targetPos != null && targetDirection != null) {
                // Place and detonate bed
                placeAndDetonateBed();
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
        
        // Sort players based on target mode
        switch (targetModeS.getValue()) {
            case "Closest":
                players.sort(Comparator.comparingDouble(mc.player::distanceTo));
                break;
            case "Health":
                players.sort(Comparator.comparingDouble(PlayerEntity::getHealth));
                break;
            case "Damage":
                // Simplified damage calculation - would be more complex in a real implementation
                players.sort(Comparator.comparingDouble(mc.player::distanceTo));
                break;
        }
        
        return players.get(0);
    }
    
    private void findBedPlacement(PlayerEntity target) {
        targetPos = null;
        targetDirection = null;
        
        BlockPos playerPos = target.getBlockPos();
        List<BlockPos> possiblePositions = new ArrayList<>();
        
        // Check positions around the player
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos headPos = playerPos.up();
            BlockPos bedPos = headPos.offset(dir.getOpposite());
            
            // Ensure both blocks are replaceable
            if (mc.world.getBlockState(headPos).isReplaceable() && 
                mc.world.getBlockState(bedPos).isReplaceable()) {
                
                double distance = mc.player.getPos().distanceTo(new Vec3d(bedPos.getX() + 0.5, bedPos.getY() + 0.5, bedPos.getZ() + 0.5));
                if (distance <= rangeS.getValue()) {
                    possiblePositions.add(bedPos);
                }
            }
        }
        
        if (possiblePositions.isEmpty()) return;
        
        // Sort positions by distance to target
        possiblePositions.sort(Comparator.comparingDouble(pos -> 
            target.getPos().distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5))));
        
        // Get the closest valid position
        for (BlockPos pos : possiblePositions) {
            // Determine the direction to place the bed
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos headPos = pos.offset(dir);
                
                // Check if the head position is where the target is
                if (headPos.equals(target.getBlockPos().up())) {
                    targetPos = pos;
                    targetDirection = dir;
                    return;
                }
            }
        }
        
        // If no optimal position, just take the first one
        if (!possiblePositions.isEmpty()) {
            targetPos = possiblePositions.get(0);
            // Default to north if we can't determine the direction
            targetDirection = Direction.NORTH;
            
            // Try to find a valid direction
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos headPos = targetPos.offset(dir);
                if (mc.world.getBlockState(headPos).isReplaceable()) {
                    targetDirection = dir;
                    break;
                }
            }
        }
    }
    
    private void placeAndDetonateBed() {
        // Find bed in hotbar
        int bedSlot = findBedSlot();
        
        if (bedSlot == -1) return; // No beds available
        
        // Switch to bed
        if (autoSwitchS.getValue()) {
            originalSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = bedSlot;
        } else if (!(mc.player.getMainHandStack().getItem() instanceof BedItem)) {
            return; // Not holding bed and auto switch is disabled
        }
        
        // Rotate to position if enabled
        if (rotateS.getValue()) {
            rotateToPos(targetPos, targetDirection);
        }
        
        // Place bed
        if (placeBed(targetPos, targetDirection)) {
            // Detonate bed if instant break is enabled
            if (instantBreakS.getValue()) {
                // The bed head position will be at targetPos offset by targetDirection
                BlockPos bedHeadPos = targetPos.offset(targetDirection);
                
                // Interact with the bed to detonate it
                interactWithBed(bedHeadPos);
            }
            
            // Switch back to original slot
            if (autoSwitchS.getValue() && originalSlot != -1) {
                mc.player.getInventory().selectedSlot = originalSlot;
                originalSlot = -1;
            }
            
            // Reset target and set delay
            targetPos = null;
            targetDirection = null;
            placeDelay = delayS.getValue().intValue();
        }
    }
    
    private int findBedSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof BedItem) {
                return i;
            }
        }
        return -1;
    }
    
    private boolean placeBed(BlockPos pos, Direction direction) {
        // Calculate hit vector
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        
        // Create hit result with the bed facing direction
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        
        // Place bed
        boolean success = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult) != null;
        if (success) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        
        return success;
    }
    
    private void interactWithBed(BlockPos pos) {
        // Calculate hit vector
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        
        // Create hit result
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        
        // Interact with bed
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
    
    private void rotateToPos(BlockPos pos, Direction direction) {
        // Calculate the position to face (middle of the block)
        double x = pos.getX() + 0.5 + direction.getOffsetX() * 0.5;
        double z = pos.getZ() + 0.5 + direction.getOffsetZ() * 0.5;
        double y = pos.getY() + 0.5;
        
        // Calculate pitch and yaw
        double deltaX = x - mc.player.getX();
        double deltaY = y - mc.player.getEyeY();
        double deltaZ = z - mc.player.getZ();
        
        double dist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, dist));
        
        // Set player rotation
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }
    
    @Override
    public void onRender(float tickDelta) {
        if (!renderS.getValue() || mc.player == null || mc.world == null || targetPos == null) return;
        
        // Render bed placement position
        // This would be implemented with proper rendering code
    }
}

