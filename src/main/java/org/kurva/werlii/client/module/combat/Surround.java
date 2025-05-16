package org.kurva.werlii.client.module.combat;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
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

public class Surround extends Module {
  private final List<BlockPos> surroundPositions = new ArrayList<>();
  private final BooleanSetting centerPlayer;
  private final BooleanSetting autoDisable;
  private final BooleanSetting autoSwitch;
  private final BooleanSetting fullSurround;
  private final BooleanSetting dynamicSurround;
  private final BooleanSetting antiCrystal;
  private final BooleanSetting antiCity;
  private final BooleanSetting renderSurround;
  private final NumberSetting placeDelay;
  private final NumberSetting blocksPerTick;
  private final ModeSetting blockType;
  private int originalSlot = -1;
  private int tickDelay = 0;
  
  public Surround() {
      super("Surround", "Surrounds you with obsidian", Category.COMBAT);
      this.setKeyCode(GLFW.GLFW_KEY_C);
      this.registerKeybinding("Werlii Combat");
      
      centerPlayer = new BooleanSetting("Center Player", "Centers player on block", this, true);
      autoDisable = new BooleanSetting("Auto Disable", "Disables after surrounding", this, false);
      autoSwitch = new BooleanSetting("Auto Switch", "Automatically switches to obsidian", this, true);
      fullSurround = new BooleanSetting("Full Surround", "Surrounds all sides including diagonals", this, false);
      dynamicSurround = new BooleanSetting("Dynamic", "Updates surround when moving", this, true);
      antiCrystal = new BooleanSetting("Anti Crystal", "Prevents crystal placement", this, true);
      antiCity = new BooleanSetting("Anti City", "Prevents city breaks", this, false);
      renderSurround = new BooleanSetting("Render", "Shows surround blocks", this, true);
      placeDelay = new NumberSetting("Place Delay", "Ticks between placements", this, 1.0, 0.0, 10.0, 1.0);
      blocksPerTick = new NumberSetting("Blocks Per Tick", "Blocks to place each tick", this, 1.0, 1.0, 4.0, 1.0);
      blockType = new ModeSetting("Block Type", "Type of block to use", this, "Obsidian", "Obsidian", "Ender Chest", "Crying Obsidian", "Netherite Block");
      
      addSetting(centerPlayer);
      addSetting(autoDisable);
      addSetting(autoSwitch);
      addSetting(fullSurround);
      addSetting(dynamicSurround);
      addSetting(antiCrystal);
      addSetting(antiCity);
      addSetting(renderSurround);
      addSetting(placeDelay);
      addSetting(blocksPerTick);
      addSetting(blockType);
  }
  
  @Override
  public void onEnable() {
      super.onEnable();
      
      if (mc.player == null || mc.world == null) return;
      
      if (centerPlayer.getValue()) {
          centerPlayer();
      }
      
      calculateSurroundPositions();
      
      originalSlot = mc.player.getInventory().selectedSlot;
      
      tickDelay = 0;
  }
  
  @Override
  public void onDisable() {
      super.onDisable();
      
      if (originalSlot != -1 && mc.player != null) {
          mc.player.getInventory().selectedSlot = originalSlot;
      }
      
      originalSlot = -1;
      surroundPositions.clear();
  }
  
  @Override
  public void onTick() {
      if (mc.player == null || mc.world == null) return;
      
      if (dynamicSurround.getValue()) {
          BlockPos playerPos = mc.player.getBlockPos();
          if (!surroundPositions.isEmpty() && !surroundPositions.get(0).equals(playerPos.north())) {
              calculateSurroundPositions();
          }
      }
      
      if (tickDelay > 0) {
          tickDelay--;
          return;
      }
      
      boolean allPlaced = true;
      int blocksPlaced = 0;
      int maxBlocksPerTick = blocksPerTick.getValue().intValue();
      
      for (BlockPos pos : surroundPositions) {
          if (mc.world.getBlockState(pos).getBlock() == Blocks.AIR) {
              allPlaced = false;
              
              int blockSlot = findBlockSlot();
              if (blockSlot == -1) {
                  if (autoDisable.getValue()) {
                      setEnabled(false);
                  }
                  return;
              }
              
              if (autoSwitch.getValue()) {
                  mc.player.getInventory().selectedSlot = blockSlot;
              }
              
              placeBlock(pos);
              
              blocksPlaced++;
              if (blocksPlaced >= maxBlocksPerTick) {
                  tickDelay = placeDelay.getValue().intValue();
                  break;
              }
          }
      }
      
      if (allPlaced && autoDisable.getValue()) {
          setEnabled(false);
      }
  }
  
  private void calculateSurroundPositions() {
      surroundPositions.clear();
      
      BlockPos playerPos = mc.player.getBlockPos();
      
      surroundPositions.add(playerPos.north());
      surroundPositions.add(playerPos.east());
      surroundPositions.add(playerPos.south());
      surroundPositions.add(playerPos.west());
      
      if (fullSurround.getValue()) {
          surroundPositions.add(playerPos.north().east());
          surroundPositions.add(playerPos.east().south());
          surroundPositions.add(playerPos.south().west());
          surroundPositions.add(playerPos.west().north());
      }
      
      if (antiCrystal.getValue()) {
          surroundPositions.add(playerPos.up(2));
          
          if (fullSurround.getValue()) {
              surroundPositions.add(playerPos.north().up());
              surroundPositions.add(playerPos.east().up());
              surroundPositions.add(playerPos.south().up());
              surroundPositions.add(playerPos.west().up());
          }
      }
      
      if (antiCity.getValue()) {
          surroundPositions.add(playerPos.north().north());
          surroundPositions.add(playerPos.east().east());
          surroundPositions.add(playerPos.south().south());
          surroundPositions.add(playerPos.west().west());
      }
  }
  
  private void centerPlayer() {
      BlockPos playerPos = mc.player.getBlockPos();
      double x = playerPos.getX() + 0.5;
      double z = playerPos.getZ() + 0.5;
      
      mc.player.setPosition(x, mc.player.getY(), z);
      mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
  }
  
  private int findBlockSlot() {
      Block targetBlock;
      
      switch (blockType.getValue()) {
          case "Obsidian":
              targetBlock = Blocks.OBSIDIAN;
              break;
          case "Ender Chest":
              targetBlock = Blocks.ENDER_CHEST;
              break;
          case "Crying Obsidian":
              targetBlock = Blocks.CRYING_OBSIDIAN;
              break;
          case "Netherite Block":
              targetBlock = Blocks.NETHERITE_BLOCK;
              break;
          default:
              targetBlock = Blocks.OBSIDIAN;
      }
      
      for (int i = 0; i < 9; i++) {
          ItemStack stack = mc.player.getInventory().getStack(i);
          if (stack.getItem() instanceof BlockItem) {
              Block block = ((BlockItem) stack.getItem()).getBlock();
              if (block == targetBlock) {
                  return i;
              }
          }
      }
      
      if (targetBlock != Blocks.OBSIDIAN) {
          for (int i = 0; i < 9; i++) {
              ItemStack stack = mc.player.getInventory().getStack(i);
              if (stack.getItem() instanceof BlockItem) {
                  Block block = ((BlockItem) stack.getItem()).getBlock();
                  if (block == Blocks.OBSIDIAN || block == Blocks.ENDER_CHEST || 
                      block == Blocks.CRYING_OBSIDIAN || block == Blocks.NETHERITE_BLOCK) {
                      return i;
                  }
              }
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
  
  @Override
  public void onRender(float tickDelta) {
      if (!renderSurround.getValue() || mc.player == null || mc.world == null) return;
      
  }
}

