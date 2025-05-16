package org.kurva.werlii.client.module.combat;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.kurva.werlii.client.util.CrystalUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AutoHitCrystal extends Module {
  private final NumberSetting rangeS;
  private final NumberSetting delayS;
  private final BooleanSetting onlyVisibleS;
  private final BooleanSetting rotateS;
  private final BooleanSetting onlyDangerousS;
  private final BooleanSetting prioritizeNearPlayerS;
  private final NumberSetting maxTargetsPerTickS;
  private final BooleanSetting autoCrystalS;
  private final NumberSetting placementRangeS;
  private final BooleanSetting autoSwitchS;
  private final NumberSetting maxSelfDamageS;
  private final BooleanSetting antiSuicideS;
  private final NumberSetting criticalDelayS;
  private final BooleanSetting smartCritsS;
  private final NumberSetting minDamageToEnemyS;
  private final BooleanSetting preventFriendDamageS;
  
  private int hitDelay = 0;
  private PlayerEntity targetPlayer = null;
  private int crystalPlaceDelay = 0;
  private boolean isAttacking = false;
  private int originalSlot = -1;
  private int criticalHitDelay = 0;
  private long lastCrystalBreakTime = 0;
  private EndCrystalEntity lastBrokenCrystal = null;
  
  public AutoHitCrystal() {
      super("AutoHitCrystal", "Automatically breaks nearby end crystals", Category.COMBAT);
      this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
      this.registerKeybinding("Werlii Combat");
      
      rangeS = new NumberSetting("Range", "Maximum range to hit crystals", this, 4.5, 1.0, 6.0, 0.1);
      delayS = new NumberSetting("Delay", "Delay between hits (ticks)", this, 2.0, 0.0, 20.0, 1.0);
      onlyVisibleS = new BooleanSetting("Only Visible", "Only hit visible crystals", this, true);
      rotateS = new BooleanSetting("Rotate", "Rotate to face crystals", this, false);
      onlyDangerousS = new BooleanSetting("Only Dangerous", "Only hit crystals near players", this, false);
      prioritizeNearPlayerS = new BooleanSetting("Prioritize Near Player", "Prioritize crystals near players", this, true);
      maxTargetsPerTickS = new NumberSetting("Max Targets", "Maximum crystals to hit per tick", this, 1.0, 1.0, 5.0, 1.0);
      autoCrystalS = new BooleanSetting("Auto Crystal", "Automatically place and detonate crystals near enemies", this, true);
      placementRangeS = new NumberSetting("Placement Range", "Maximum range to place crystals", this, 4.0, 1.0, 6.0, 0.1);
      autoSwitchS = new BooleanSetting("Auto Switch", "Automatically switch to crystals", this, true);
      maxSelfDamageS = new NumberSetting("Max Self Damage", "Maximum damage to take from crystals", this, 4.0, 0.0, 20.0, 0.5);
      antiSuicideS = new BooleanSetting("Anti Suicide", "Prevent breaking crystals that would kill you", this, true);
      criticalDelayS = new NumberSetting("Critical Delay", "Delay for proper critical hits (ms)", this, 500.0, 100.0, 1000.0, 50.0);
      smartCritsS = new BooleanSetting("Smart Crits", "Intelligently time attacks for critical hits", this, true);
      minDamageToEnemyS = new NumberSetting("Min Enemy Damage", "Minimum damage to deal to enemies", this, 4.0, 0.0, 20.0, 0.5);
      preventFriendDamageS = new BooleanSetting("Prevent Friend Damage", "Don't break crystals near friends", this, true);
      
      addSetting(rangeS);
      addSetting(delayS);
      addSetting(onlyVisibleS);
      addSetting(rotateS);
      addSetting(onlyDangerousS);
      addSetting(prioritizeNearPlayerS);
      addSetting(maxTargetsPerTickS);
      addSetting(autoCrystalS);
      addSetting(placementRangeS);
      addSetting(autoSwitchS);
      addSetting(maxSelfDamageS);
      addSetting(antiSuicideS);
      addSetting(criticalDelayS);
      addSetting(smartCritsS);
      addSetting(minDamageToEnemyS);
      addSetting(preventFriendDamageS);
  }
  
  @Override
  public void onTick() {
      if (mc.player == null || mc.world == null) return;
      
      if (hitDelay > 0) {
          hitDelay--;
      }
      
      if (crystalPlaceDelay > 0) {
          crystalPlaceDelay--;
      }
      
      if (criticalHitDelay > 0) {
          criticalHitDelay--;
      }
      
      if (mc.options.attackKey.isPressed() && mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult) {
          Entity entity = ((net.minecraft.util.hit.EntityHitResult) mc.crosshairTarget).getEntity();
          if (entity instanceof PlayerEntity && entity != mc.player) {
              targetPlayer = (PlayerEntity) entity;
              isAttacking = true;
              
              if (smartCritsS.getValue() && criticalHitDelay <= 0) {
                  if (mc.player.fallDistance > 0.0 && !mc.player.isOnGround() && !mc.player.isClimbing() && !mc.player.isTouchingWater()) {
                      criticalHitDelay = (int)(criticalDelayS.getValue() / 50);
                  }
              }
          }
      } else {
          if (isAttacking && targetPlayer != null) {
              if (System.currentTimeMillis() - lastCrystalBreakTime > 2000) {
                  isAttacking = false;
                  targetPlayer = null;
              }
          }
      }
      
      if (autoCrystalS.getValue() && isAttacking && targetPlayer != null && targetPlayer.isAlive()) {
          if (crystalPlaceDelay <= 0) {
              placeCrystalNearTarget();
              crystalPlaceDelay = 2;
          }
      }
      
      if (hitDelay <= 0) {
          List<EndCrystalEntity> crystals = findCrystalsToHit();
          
          if (!crystals.isEmpty()) {
              int maxTargets = maxTargetsPerTickS.getValue().intValue();
              int targetsHit = 0;
              
              for (EndCrystalEntity crystal : crystals) {
                  if (targetsHit >= maxTargets) break;
                  
                  double selfDamage = calculateSelfDamage(crystal);
                  if (selfDamage > maxSelfDamageS.getValue()) {
                      continue;
                  }
                  
                  if (antiSuicideS.getValue() && selfDamage >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
                      continue;
                  }
                  
                  if (preventFriendDamageS.getValue() && wouldDamageFriends(crystal)) {
                      continue;
                  }
                  
                  if (rotateS.getValue()) {
                      rotateToEntity(crystal);
                  }
                  
                  mc.interactionManager.attackEntity(mc.player, crystal);
                  mc.player.swingHand(Hand.MAIN_HAND);
                  
                  lastBrokenCrystal = crystal;
                  lastCrystalBreakTime = System.currentTimeMillis();
                  
                  targetsHit++;
              }
              
              if (targetsHit > 0) {
                  hitDelay = delayS.getValue().intValue();
              }
          }
      }
  }
  
  private void placeCrystalNearTarget() {
      BlockPos obsidianPos = findNearestObsidian(targetPlayer);
      
      if (obsidianPos != null) {
          double damageToEnemy = calculateDamageToEntity(obsidianPos, targetPlayer);
          double damageToSelf = calculateSelfDamage(obsidianPos);
          
          if (damageToEnemy < minDamageToEnemyS.getValue()) {
              return;
          }
          
          if (damageToSelf > maxSelfDamageS.getValue()) {
              return;
          }
          
          if (antiSuicideS.getValue() && damageToSelf >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
              return;
          }
          
          if (autoSwitchS.getValue()) {
              originalSlot = mc.player.getInventory().selectedSlot;
              int crystalSlot = findItemInHotbar(Items.END_CRYSTAL);
              
              if (crystalSlot != -1) {
                  mc.player.getInventory().selectedSlot = crystalSlot;
              } else {
                  return;
              }
          } else if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
              return;
          }
          
          if (rotateS.getValue()) {
              rotateToPos(obsidianPos);
          }
          
          placeCrystal(obsidianPos);
          
          if (autoSwitchS.getValue() && originalSlot != -1) {
              mc.player.getInventory().selectedSlot = originalSlot;
              originalSlot = -1;
          }
      }
  }
  
  private BlockPos findNearestObsidian(PlayerEntity target) {
      List<BlockPos> validPositions = new ArrayList<>();
      int range = 3;
      
      BlockPos targetPos = target.getBlockPos();
      
      for (int x = -range; x <= range; x++) {
          for (int y = -range; y <= range; y++) {
              for (int z = -range; z <= range; z++) {
                  BlockPos pos = targetPos.add(x, y, z);
                  
                  if (mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN || 
                      mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK) {
                      if (mc.world.getBlockState(pos.up()).isAir() && 
                          mc.world.getBlockState(pos.up(2)).isAir()) {
                          
                          double distance = mc.player.getPos().distanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                          if (distance <= placementRangeS.getValue()) {
                              boolean crystalExists = false;
                              for (Entity entity : mc.world.getEntities()) {
                                  if (entity instanceof EndCrystalEntity) {
                                      BlockPos crystalPos = entity.getBlockPos();
                                      if (crystalPos.equals(pos.up())) {
                                          crystalExists = true;
                                          break;
                                      }
                                  }
                              }
                              
                              if (!crystalExists) {
                                  validPositions.add(pos);
                              }
                          }
                      }
                  }
              }
          }
      }
      
      if (validPositions.isEmpty()) return null;
      
      validPositions.sort((pos1, pos2) -> {
          double damage1 = calculateDamageToEntity(pos1, target);
          double damage2 = calculateDamageToEntity(pos2, target);
          return Double.compare(damage2, damage1);
      });
      
      return validPositions.get(0);
  }
  
  private double calculateDamageToEntity(BlockPos pos, Entity entity) {
      return CrystalUtil.calculateDamage(mc.world, 
          pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, entity);
  }
  
  private double calculateSelfDamage(BlockPos pos) {
      return CrystalUtil.calculateDamage(mc.world, 
          pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, mc.player);
  }
  
  private double calculateSelfDamage(EndCrystalEntity crystal) {
      return CrystalUtil.calculateDamage(mc.world, 
          crystal.getX(), crystal.getY(), crystal.getZ(), mc.player);
  }
  
  private boolean wouldDamageFriends(EndCrystalEntity crystal) {
      for (PlayerEntity player : mc.world.getPlayers()) {
          if (player != mc.player && isFriend(player)) {
              double damage = CrystalUtil.calculateDamage(mc.world, 
                  crystal.getX(), crystal.getY(), crystal.getZ(), player);
              
              if (damage > 4.0 && player.getHealth() + player.getAbsorptionAmount() <= 10.0) {
                  return true;
              }
          }
      }
      return false;
  }
  
  private boolean isFriend(PlayerEntity player) {
      return false;
  }
  
  private void placeCrystal(BlockPos pos) {
      Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
      
      BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
      
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
      mc.player.swingHand(Hand.MAIN_HAND);
  }
  
  private List<EndCrystalEntity> findCrystalsToHit() {
      List<EndCrystalEntity> crystals = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
              .filter(entity -> entity instanceof EndCrystalEntity)
              .map(entity -> (EndCrystalEntity) entity)
              .filter(crystal -> mc.player.distanceTo(crystal) <= rangeS.getValue())
              .filter(crystal -> !onlyVisibleS.getValue() || mc.player.canSee(crystal))
              .collect(Collectors.toList());
      
      if (crystals.isEmpty()) return crystals;
      
      if (onlyDangerousS.getValue()) {
          crystals = crystals.stream()
                  .filter(this::isCrystalDangerous)
                  .collect(Collectors.toList());
      }
      
      if (prioritizeNearPlayerS.getValue() && targetPlayer != null) {
          crystals.sort(Comparator.comparingDouble(crystal -> crystal.distanceTo(targetPlayer)));
      } else {
          crystals.sort(Comparator.comparingDouble(mc.player::distanceTo));
      }
      
      return crystals;
  }
  
  private boolean isCrystalDangerous(EndCrystalEntity crystal) {
      if (targetPlayer != null && targetPlayer.isAlive()) {
          double distance = targetPlayer.distanceTo(crystal);
          if (distance <= 6.0) {
              return true;
          }
      }
      
      for (PlayerEntity player : mc.world.getPlayers()) {
          if (player != mc.player && player.isAlive() && !player.isSpectator()) {
              double distance = player.distanceTo(crystal);
              if (distance <= 6.0) {
                  return true;
              }
          }
      }
      
      if (mc.player.distanceTo(crystal) <= 6.0 && 
          mc.player.getHealth() + mc.player.getAbsorptionAmount() <= 10.0) {
          return true;
      }
      
      return false;
  }
  
  private int findItemInHotbar(net.minecraft.item.Item item) {
      for (int i = 0; i < 9; i++) {
          if (mc.player.getInventory().getStack(i).getItem() == item) {
              return i;
          }
      }
      return -1;
  }
  
  private void rotateToEntity(Entity entity) {
      double x = entity.getX() - mc.player.getX();
      double z = entity.getZ() - mc.player.getZ();
      double y = entity.getY() + entity.getHeight() / 2.0 - mc.player.getEyeY();
      
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
      if (mc.player == null || mc.world == null) return;
      
  }
}

