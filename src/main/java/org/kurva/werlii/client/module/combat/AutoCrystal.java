package org.kurva.werlii.client.module.combat;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.kurva.werlii.client.util.CrystalUtil;
import org.kurva.werlii.client.util.EntityUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AutoCrystal extends Module {
  private int placeDelay = 0;
  private int breakDelay = 0;
  private BlockPos lastPlacePos = null;
  private EndCrystalEntity lastTargetCrystal = null;
  
  private final NumberSetting placeDelayS;
  private final NumberSetting breakDelayS;
  private final NumberSetting placeRangeS;
  private final NumberSetting breakRangeS;
  private final NumberSetting wallsRangeS;
  private final NumberSetting minDamageS;
  private final NumberSetting maxSelfDamageS;
  private final BooleanSetting antiSuicideS;
  private final BooleanSetting autoSwitchS;
  private final BooleanSetting silentSwitchS;
  private final BooleanSetting rotateS;
  private final BooleanSetting bypassModeS;
  private final BooleanSetting pauseWhileEatingS;
  private final BooleanSetting randomDelayS;
  private final BooleanSetting placeS;
  private final BooleanSetting breakS;
  private final BooleanSetting rayTraceS;
  private final ModeSetting targetS;
  private final BooleanSetting predictS;
  private final BooleanSetting multiPlaceS;
  private final NumberSetting facePlaceHealthS;
  private final BooleanSetting renderS;
  
  public AutoCrystal() {
      super("AutoCrystal", "Automatically places and breaks crystals", Category.COMBAT);
      this.setKeyCode(GLFW.GLFW_KEY_G);
      this.registerKeybinding("Werlii Combat");
      
      placeDelayS = new NumberSetting("Place Delay", "Delay between crystal placements", this, 1.0, 0.0, 20.0, 1.0);
      breakDelayS = new NumberSetting("Break Delay", "Delay between crystal breaks", this, 1.0, 0.0, 20.0, 1.0);
      placeRangeS = new NumberSetting("Place Range", "Range for crystal placement", this, 4.5, 1.0, 6.0, 0.1);
      breakRangeS = new NumberSetting("Break Range", "Range for crystal breaking", this, 4.5, 1.0, 6.0, 0.1);
      wallsRangeS = new NumberSetting("Walls Range", "Range for crystal breaking through walls", this, 3.5, 1.0, 6.0, 0.1);
      minDamageS = new NumberSetting("Min Damage", "Minimum damage to deal to enemies", this, 5.0, 1.0, 20.0, 0.5);
      maxSelfDamageS = new NumberSetting("Max Self Damage", "Maximum damage to take from crystals", this, 10.0, 1.0, 20.0, 0.5);
      antiSuicideS = new BooleanSetting("Anti Suicide", "Prevent self-damage that would kill you", this, true);
      autoSwitchS = new BooleanSetting("Auto Switch", "Automatically switch to crystals", this, true);
      silentSwitchS = new BooleanSetting("Silent Switch", "Switch to crystals without animation", this, false);
      rotateS = new BooleanSetting("Rotate", "Rotate to face crystals", this, false);
      bypassModeS = new BooleanSetting("Bypass Mode", "Enable anti-cheat bypass features", this, false);
      pauseWhileEatingS = new BooleanSetting("Pause While Eating", "Pause crystal placement while eating", this, true);
      randomDelayS = new BooleanSetting("Random Delay", "Add random delay to bypass anti-cheats", this, true);
      placeS = new BooleanSetting("Place", "Enable crystal placement", this, true);
      breakS = new BooleanSetting("Break", "Enable crystal breaking", this, true);
      rayTraceS = new BooleanSetting("Ray Trace", "Use ray tracing for placement", this, false);
      targetS = new ModeSetting("Target", "Target selection priority", this, "Closest", "Closest", "Health", "Damage");
      predictS = new BooleanSetting("Predict", "Predict crystal explosions", this, true);
      multiPlaceS = new BooleanSetting("Multi Place", "Place multiple crystals at once", this, false);
      facePlaceHealthS = new NumberSetting("Face Place Health", "Health threshold for face placing", this, 8.0, 0.0, 20.0, 0.5);
      renderS = new BooleanSetting("Render", "Render crystal placements", this, true);
      
      addSetting(placeDelayS);
      addSetting(breakDelayS);
      addSetting(placeRangeS);
      addSetting(breakRangeS);
      addSetting(wallsRangeS);
      addSetting(minDamageS);
      addSetting(maxSelfDamageS);
      addSetting(antiSuicideS);
      addSetting(autoSwitchS);
      addSetting(silentSwitchS);
      addSetting(rotateS);
      addSetting(bypassModeS);
      addSetting(pauseWhileEatingS);
      addSetting(randomDelayS);
      addSetting(placeS);
      addSetting(breakS);
      addSetting(rayTraceS);
      addSetting(targetS);
      addSetting(predictS);
      addSetting(multiPlaceS);
      addSetting(facePlaceHealthS);
      addSetting(renderS);
  }
  
  @Override
  public void onEnable() {
      super.onEnable();
      placeDelay = 0;
      breakDelay = 0;
      lastPlacePos = null;
      lastTargetCrystal = null;
  }
  
  @Override
  public void onTick() {
      if (mc.player == null || mc.world == null) return;
      
      if (pauseWhileEatingS.getValue() && mc.player.isUsingItem()) {
          return;
      }
      
      if (breakS.getValue() && breakDelay <= 0) {
          breakCrystals();
          breakDelay = breakDelayS.getValue().intValue();
          
          if (randomDelayS.getValue()) {
              breakDelay += Math.random() * 2;
          }
      } else {
          breakDelay--;
      }
      
      if (placeS.getValue() && placeDelay <= 0) {
          placeCrystals();
          placeDelay = placeDelayS.getValue().intValue();
          
          if (randomDelayS.getValue()) {
              placeDelay += Math.random() * 2;
          }
      } else {
          placeDelay--;
      }
  }
  
  private void breakCrystals() {
      EndCrystalEntity bestCrystal = findBestCrystalToBreak();
      
      if (bestCrystal != null) {
          if (rotateS.getValue()) {
              rotateToEntity(bestCrystal);
          }
          
          mc.interactionManager.attackEntity(mc.player, bestCrystal);
          mc.player.swingHand(Hand.MAIN_HAND);
          
          lastTargetCrystal = bestCrystal;
      }
  }
  
  private EndCrystalEntity findBestCrystalToBreak() {
      List<EndCrystalEntity> crystals = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
              .filter(entity -> entity instanceof EndCrystalEntity)
              .map(entity -> (EndCrystalEntity) entity)
              .filter(crystal -> mc.player.distanceTo(crystal) <= breakRangeS.getValue())
              .filter(crystal -> mc.player.canSee(crystal) || mc.player.distanceTo(crystal) <= wallsRangeS.getValue())
              .sorted(Comparator.comparingDouble(mc.player::distanceTo))
              .collect(Collectors.toList());
      
      if (!crystals.isEmpty()) {
          return crystals.get(0);
      }
      
      return null;
  }
  
  private void placeCrystals() {
      if (!hasEndCrystals() && !autoSwitchS.getValue()) return;
      
      BlockPos bestPos = findBestPlacePos();
      
      if (bestPos != null) {
          int originalSlot = -1;
          if (autoSwitchS.getValue() && mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
              originalSlot = mc.player.getInventory().selectedSlot;
              int crystalSlot = findCrystalSlot();
              
              if (crystalSlot != -1) {
                  if (silentSwitchS.getValue() && WerliiClient.getInstance().isBypassMode()) {
                  } else {
                      mc.player.getInventory().selectedSlot = crystalSlot;
                  }
              }
          }
          
          if (rotateS.getValue()) {
              rotateToPos(bestPos);
          }
          
          placeCrystal(bestPos);
          lastPlacePos = bestPos;
          
          if (autoSwitchS.getValue() && originalSlot != -1 && !silentSwitchS.getValue()) {
              mc.player.getInventory().selectedSlot = originalSlot;
          }
      }
  }
  
  private boolean hasEndCrystals() {
      return mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL || 
             mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
  }
  
  private int findCrystalSlot() {
      for (int i = 0; i < 9; i++) {
          if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) {
              return i;
          }
      }
      return -1;
  }
  
  private BlockPos findBestPlacePos() {
      List<BlockPos> validPositions = findValidPositions();
      
      if (validPositions.isEmpty()) return null;
      
      BlockPos bestPos = null;
      double bestDamage = 0;
      PlayerEntity target = findBestTarget();
      
      if (target == null) return null;
      
      for (BlockPos pos : validPositions) {
          double damage = calculateDamage(pos, target);
          double selfDamage = calculateSelfDamage(pos);
          
          boolean facePlacing = target.getHealth() <= facePlaceHealthS.getValue();
          
          if ((damage >= minDamageS.getValue() || facePlacing) && 
              selfDamage <= maxSelfDamageS.getValue() && 
              damage > bestDamage) {
              
              if (antiSuicideS.getValue() && selfDamage >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
                  continue;
              }
              
              bestDamage = damage;
              bestPos = pos;
              
              if (!multiPlaceS.getValue()) {
                  break;
              }
          }
      }
      
      return bestPos;
  }
  
  private List<BlockPos> findValidPositions() {
      List<BlockPos> positions = new ArrayList<>();
      int range = (int) Math.ceil(placeRangeS.getValue());
      
      BlockPos playerPos = mc.player.getBlockPos();
      
      for (int x = -range; x <= range; x++) {
          for (int y = -range; y <= range; y++) {
              for (int z = -range; z <= range; z++) {
                  BlockPos pos = playerPos.add(x, y, z);
                  
                  if (isValidPosition(pos)) {
                      positions.add(pos);
                  }
              }
          }
      }
      
      return positions;
  }
  
  private boolean isValidPosition(BlockPos pos) {
      if (mc.world.getBlockState(pos).getBlock() != Blocks.OBSIDIAN && 
          mc.world.getBlockState(pos).getBlock() != Blocks.BEDROCK) {
          return false;
      }
      
      if (mc.world.getBlockState(pos.up()).getBlock() != Blocks.AIR) {
          return false;
      }
      
      if (mc.world.getBlockState(pos.up(2)).getBlock() != Blocks.AIR) {
          return false;
      }
      
      for (Entity entity : mc.world.getEntities()) {
          if (entity instanceof EndCrystalEntity) {
              BlockPos crystalPos = entity.getBlockPos();
              if (crystalPos.equals(pos.up())) {
                  return false;
              }
          }
      }
      
      double distance = mc.player.getPos().distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
      return distance <= placeRangeS.getValue();
  }
  
  private double calculateDamage(BlockPos pos, PlayerEntity target) {
      return CrystalUtil.calculateDamage(mc.world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, target);
  }
  
  private double calculateSelfDamage(BlockPos pos) {
      return CrystalUtil.calculateDamage(mc.world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, mc.player);
  }
  
  private PlayerEntity findBestTarget() {
      List<PlayerEntity> players = mc.world.getPlayers()
          .stream()
          .filter(player -> player != mc.player)
          .filter(EntityUtil::isValidTarget)
          .filter(player -> mc.player.distanceTo(player) <= placeRangeS.getValue() * 2)
          .collect(Collectors.toList());
      
      if (players.isEmpty()) return null;
      
      switch (targetS.getValue()) {
          case "Closest":
              players.sort(Comparator.comparingDouble(mc.player::distanceTo));
              break;
          case "Health":
              players.sort(Comparator.comparingDouble(PlayerEntity::getHealth));
              break;
          case "Damage":
              players.sort(Comparator.comparingDouble(mc.player::distanceTo));
              break;
      }
      
      return players.get(0);
  }
  
  private void placeCrystal(BlockPos pos) {
      Hand hand = mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL ? 
                  Hand.MAIN_HAND : Hand.OFF_HAND;
      
      BlockHitResult hitResult = new BlockHitResult(
          new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5),
          Direction.UP,
          pos,
          false
      );
      
      mc.interactionManager.interactBlock(mc.player, hand, hitResult);
      mc.player.swingHand(hand);
  }
  
  private void rotateToEntity(Entity entity) {
      double x = entity.getX() - mc.player.getX();
      double z = entity.getZ() - mc.player.getZ();
      double y = entity.getY() - mc.player.getY();
      
      double dist = Math.sqrt(x * x + z * z);
      float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90F;
      float pitch = (float) -Math.toDegrees(Math.atan2(y, dist));
      
      mc.player.setYaw(yaw);
      mc.player.setPitch(pitch);
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
      if (!renderS.getValue() || mc.player == null || mc.world == null) return;
  }
}

