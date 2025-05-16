package org.kurva.werlii.client.module.combat;

import net.minecraft.block.Blocks;
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

public class AnchorMacro extends Module {
    private final BooleanSetting autoSwitchS;
    private final BooleanSetting rotateS;
    private final NumberSetting delayS;
    private final ModeSetting activationS;
    private final BooleanSetting placeFirstS;
    private final BooleanSetting instantDetonateS;
    
    private int actionDelay = 0;
    private int actionStep = 0;
    private BlockPos targetPos = null;
    private int originalSlot = -1;
    
    public AnchorMacro() {
        super("AnchorMacro", "Quickly places and detonates respawn anchors", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Combat");
        
        autoSwitchS = new BooleanSetting("Auto Switch", "Automatically switch to anchors and glowstone", this, true);
        rotateS = new BooleanSetting("Rotate", "Rotate to face placement", this, false);
        delayS = new NumberSetting("Delay", "Delay between actions (ticks)", this, 1.0, 0.0, 10.0, 1.0);
        activationS = new ModeSetting("Activation", "How to activate the macro", this, "Key Press", "Key Press", "Key Hold", "Right Click");
        placeFirstS = new BooleanSetting("Place First", "Place anchor before charging", this, true);
        instantDetonateS = new BooleanSetting("Instant Detonate", "Instantly detonate after charging", this, true);
        
        addSetting(autoSwitchS);
        addSetting(rotateS);
        addSetting(delayS);
        addSetting(activationS);
        addSetting(placeFirstS);
        addSetting(instantDetonateS);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        if (activationS.getValue().equals("Key Press")) {
            startMacro();
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        resetMacro();
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        if (activationS.getValue().equals("Key Hold") && !isEnabled()) {
            return;
        }
        
        if (activationS.getValue().equals("Right Click") && !mc.options.useKey.isPressed()) {
            return;
        }
        
        if (actionDelay > 0) {
            actionDelay--;
            return;
        }
        
        if (targetPos != null) {
            executeMacroStep();
        } else if (activationS.getValue().equals("Key Hold") || activationS.getValue().equals("Right Click")) {
            startMacro();
        }
    }
    
    private void startMacro() {
        if (mc.crosshairTarget instanceof BlockHitResult) {
            BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
            BlockPos hitPos = hitResult.getBlockPos();
            Direction side = hitResult.getSide();
            
            targetPos = hitPos.offset(side);
            
            if (!mc.world.getBlockState(targetPos).isReplaceable()) {
                targetPos = null;
                return;
            }
            
            if (autoSwitchS.getValue()) {
                originalSlot = mc.player.getInventory().selectedSlot;
            }
            
            actionStep = 0;
        }
    }
    
    private void executeMacroStep() {
        if (placeFirstS.getValue()) {
            switch (actionStep) {
                case 0:
                    placeAnchor();
                    break;
                case 1:
                    chargeAnchor();
                    break;
                case 2:
                    detonateAnchor();
                    break;
                default:
                    resetMacro();
                    return;
            }
        } else {
            switch (actionStep) {
                case 0:
                    chargeAnchor();
                    break;
                case 1:
                    detonateAnchor();
                    break;
                default:
                    resetMacro();
                    return;
            }
        }
        
        actionStep++;
        actionDelay = delayS.getValue().intValue();
    }
    
    private void placeAnchor() {
        int anchorSlot = findItemInHotbar(Items.RESPAWN_ANCHOR);
        
        if (anchorSlot == -1) {
            resetMacro();
            return;
        }
        
        if (autoSwitchS.getValue()) {
            mc.player.getInventory().selectedSlot = anchorSlot;
        } else if (mc.player.getMainHandStack().getItem() != Items.RESPAWN_ANCHOR) {
            resetMacro();
            return;
        }
        
        if (rotateS.getValue()) {
            rotateToPos(targetPos);
        }
        
        placeBlock(targetPos);
    }
    
    private void chargeAnchor() {
        int glowstoneSlot = findItemInHotbar(Items.GLOWSTONE);
        
        if (glowstoneSlot == -1) {
            resetMacro();
            return;
        }
        
        if (autoSwitchS.getValue()) {
            mc.player.getInventory().selectedSlot = glowstoneSlot;
        } else if (mc.player.getMainHandStack().getItem() != Items.GLOWSTONE) {
            resetMacro();
            return;
        }
        
        if (rotateS.getValue()) {
            rotateToPos(targetPos);
        }
        
        interactWithBlock(targetPos);
        
        if (instantDetonateS.getValue()) {
            actionDelay = 0;
        }
    }
    
    private void detonateAnchor() {
        int anchorSlot = findItemInHotbar(Items.RESPAWN_ANCHOR);
        
        if (anchorSlot == -1) {
            resetMacro();
            return;
        }
        
        if (autoSwitchS.getValue()) {
            mc.player.getInventory().selectedSlot = anchorSlot;
        } else if (mc.player.getMainHandStack().getItem() != Items.RESPAWN_ANCHOR) {
            resetMacro();
            return;
        }
        
        if (rotateS.getValue()) {
            rotateToPos(targetPos);
        }
        
        interactWithBlock(targetPos);
        
        resetMacro();
    }
    
    private void resetMacro() {
        targetPos = null;
        actionStep = 0;
        
        if (autoSwitchS.getValue() && originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }
        originalSlot = -1;
        
        if (activationS.getValue().equals("Key Press")) {
            setEnabled(false);
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
}

