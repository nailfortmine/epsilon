package org.kurva.werlii.client.module.combat;

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
import org.lwjgl.glfw.GLFW;

public class AirAnchor extends Module {
    private BlockPos targetPos = null;
    private boolean waitForKeyPress = true;
    
    public AirAnchor() {
        super("AirAnchor", "Assists with placing blocks in air for crystal placement", Category.COMBAT);
        this.setKeyCode(GLFW.GLFW_KEY_J);
        this.registerKeybinding("Werlii Combat");
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        ItemStack mainHandStack = mc.player.getMainHandStack();
        if (!(mainHandStack.getItem() instanceof BlockItem)) return;
        
        if (mc.crosshairTarget instanceof BlockHitResult) {
            BlockHitResult hitResult = (BlockHitResult) mc.crosshairTarget;
            BlockPos hitPos = hitResult.getBlockPos();
            Direction side = hitResult.getSide();
            
            targetPos = hitPos.offset(side);
            
            if (mc.world.getBlockState(targetPos).getBlock() == Blocks.AIR) {
                if (mc.options.useKey.isPressed() && !waitForKeyPress) {
                    BlockHitResult placeHitResult = new BlockHitResult(
                        new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5),
                        side.getOpposite(),
                        hitPos,
                        false
                    );
                    
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, placeHitResult);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    
                    waitForKeyPress = true;
                }
                
                if (!mc.options.useKey.isPressed()) {
                    waitForKeyPress = false;
                }
            }
        }
    }
    
    @Override
    public void onRender(float tickDelta) {
    }
}

