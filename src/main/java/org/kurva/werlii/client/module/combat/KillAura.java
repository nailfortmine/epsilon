package org.kurva.werlii.client.module.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.kurva.werlii.client.util.EntityUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.item.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class KillAura extends Module {
  private int attackDelay = 0;
  
  private final NumberSetting rangeS;
  private final NumberSetting attackDelayS;
  private final BooleanSetting targetPlayersS;
  private final BooleanSetting targetMobsS;
  private final BooleanSetting targetAnimalsS;
  private final BooleanSetting rotateS;
  private final BooleanSetting autoSwitchS;
  private final BooleanSetting onlySwordS;
  private final BooleanSetting criticalS;
  private final BooleanSetting throughWallsS;
  private final BooleanSetting pauseOnEatS;
  private final BooleanSetting randomizeS;
  private final ModeSetting priorityS;
  private final ModeSetting attackTimingS;
  private final NumberSetting fovS;
  private final BooleanSetting swingS;
  
  public KillAura() {
      super("KillAura", "Automatically attacks nearby entities", Category.COMBAT);
      this.setKeyCode(GLFW.GLFW_KEY_K);
      this.registerKeybinding("Werlii Combat");
      
      rangeS = new NumberSetting("Range", "Attack range", this, 4.0, 1.0, 6.0, 0.1);
      attackDelayS = new NumberSetting("Attack Delay", "Delay between attacks", this, 10.0, 0.0, 20.0, 1.0);
      targetPlayersS = new BooleanSetting("Target Players", "Attack players", this, true);
      targetMobsS = new BooleanSetting("Target Mobs", "Attack hostile mobs", this, false);
      targetAnimalsS = new BooleanSetting("Target Animals", "Attack animals", this, false);
      rotateS = new BooleanSetting("Rotate", "Rotate to face target", this, false);
      autoSwitchS = new BooleanSetting("Auto Switch", "Switch to best weapon", this, false);
      onlySwordS = new BooleanSetting("Only Sword", "Only attack with sword", this, false);
      criticalS = new BooleanSetting("Criticals", "Always do critical hits", this, true);
      throughWallsS = new BooleanSetting("Through Walls", "Attack through walls", this, false);
      pauseOnEatS = new BooleanSetting("Pause On Eat", "Don't attack while eating", this, true);
      randomizeS = new BooleanSetting("Randomize", "Add random delay", this, true);
      priorityS = new ModeSetting("Priority", "Target selection priority", this, "Distance", "Distance", "Health", "Armor", "Angle");
      attackTimingS = new ModeSetting("Timing", "When to attack", this, "Pre", "Pre", "Post", "Both");
      fovS = new NumberSetting("FOV", "Field of view for targeting", this, 180.0, 30.0, 180.0, 15.0);
      swingS = new BooleanSetting("Swing", "Swing arm when attacking", this, true);
      
      addSetting(rangeS);
      addSetting(attackDelayS);
      addSetting(targetPlayersS);
      addSetting(targetMobsS);
      addSetting(targetAnimalsS);
      addSetting(rotateS);
      addSetting(autoSwitchS);
      addSetting(onlySwordS);
      addSetting(criticalS);
      addSetting(throughWallsS);
      addSetting(pauseOnEatS);
      addSetting(randomizeS);
      addSetting(priorityS);
      addSetting(attackTimingS);
      addSetting(fovS);
      addSetting(swingS);
  }
  
  @Override
  public void onTick() {
      if (mc.player == null || mc.world == null) return;
      
      if (pauseOnEatS.getValue() && mc.player.isUsingItem()) {
          return;
      }
      
      if (attackDelay > 0) {
          attackDelay--;
          return;
      }
      
      LivingEntity target = findBestTarget();
      
      if (target != null) {
          if (autoSwitchS.getValue()) {
              switchToBestWeapon();
          }
          
          if (onlySwordS.getValue() && !(mc.player.getMainHandStack().getItem() instanceof SwordItem)) {
              return;
          }
          
          if (rotateS.getValue()) {
              rotateToTarget(target);
          }
          
          if (criticalS.getValue() && mc.player.isOnGround() && !mc.player.isSprinting()) {
              mc.player.jump();
          }
          
          mc.interactionManager.attackEntity(mc.player, target);
          
          if (swingS.getValue()) {
              mc.player.swingHand(Hand.MAIN_HAND);
          }
          
          attackDelay = attackDelayS.getValue().intValue();
          
          if (randomizeS.getValue()) {
              attackDelay += Math.random() * 3;
          }
      }
  }
  
  private LivingEntity findBestTarget() {
      List<LivingEntity> targets = StreamSupport.stream(mc.world.getEntities().spliterator(), false)
              .filter(entity -> entity instanceof LivingEntity)
              .map(entity -> (LivingEntity) entity)
              .filter(this::isValidTarget)
              .filter(entity -> mc.player.distanceTo(entity) <= rangeS.getValue())
              .filter(entity -> throughWallsS.getValue() || mc.player.canSee(entity))
              .filter(this::isInFov)
              .collect(Collectors.toList());
      
      if (targets.isEmpty()) {
          return null;
      }
      
      switch (priorityS.getValue()) {
          case "Distance":
              targets.sort(Comparator.comparingDouble(mc.player::distanceTo));
              break;
          case "Health":
              targets.sort(Comparator.comparingDouble(LivingEntity::getHealth));
              break;
          case "Armor":
              targets.sort(Comparator.comparingInt(entity -> entity instanceof PlayerEntity ? 
                      ((PlayerEntity) entity).getInventory().getArmorStack(0).getDamage() +
                      ((PlayerEntity) entity).getInventory().getArmorStack(1).getDamage() +
                      ((PlayerEntity) entity).getInventory().getArmorStack(2).getDamage() +
                      ((PlayerEntity) entity).getInventory().getArmorStack(3).getDamage() : 0));
              break;
          case "Angle":
              targets.sort(Comparator.comparingDouble(this::getAngleToEntity));
              break;
      }
      
      return targets.get(0);
  }
  
  private boolean isValidTarget(LivingEntity entity) {
      if (entity == mc.player) return false;
      
      if (entity instanceof PlayerEntity) {
          return targetPlayersS.getValue() && EntityUtil.isValidTarget((PlayerEntity) entity);
      } else {
          boolean isMob = entity.isAttackable();
          boolean isAnimal = !isMob && entity.isAlive();
          
          return (isMob && targetMobsS.getValue()) || (isAnimal && targetAnimalsS.getValue());
      }
  }
  
  private boolean isInFov(LivingEntity entity) {
      double fov = fovS.getValue();
      if (fov >= 180) return true;
      
      Vec3d playerRotation = mc.player.getRotationVec(1.0f);
      Vec3d entityVec = new Vec3d(
          entity.getX() - mc.player.getX(),
          entity.getEyeY() - mc.player.getEyeY(),
          entity.getZ() - mc.player.getZ()
      ).normalize();
      
      double angle = Math.toDegrees(Math.acos(playerRotation.dotProduct(entityVec)));
      return angle <= fov / 2;
  }
  
  private double getAngleToEntity(Entity entity) {
      Vec3d playerRotation = mc.player.getRotationVec(1.0f);
      Vec3d entityVec = new Vec3d(
          entity.getX() - mc.player.getX(),
          entity.getEyeY() - mc.player.getEyeY(),
          entity.getZ() - mc.player.getZ()
      ).normalize();
      
      return Math.toDegrees(Math.acos(playerRotation.dotProduct(entityVec)));
  }
  
  private void switchToBestWeapon() {
    int bestSlot = -1;
    float bestDamage = -1;
    
    for (int i = 0; i < 9; i++) {
        ItemStack stack = mc.player.getInventory().getStack(i);
        Item item = stack.getItem();
        float damage = 0.0f;
        
        if (item instanceof SwordItem) {
            if (item == Items.NETHERITE_SWORD) {
                damage = 8.0f;
            } else if (item == Items.DIAMOND_SWORD) {
                damage = 7.0f;
            } else if (item == Items.IRON_SWORD) {
                damage = 6.0f;
            } else if (item == Items.STONE_SWORD) {
                damage = 5.0f;
            } else if (item == Items.WOODEN_SWORD || item == Items.GOLDEN_SWORD) {
                damage = 4.0f;
            }
        } else if (item instanceof AxeItem) {
            if (item == Items.NETHERITE_AXE) {
                damage = 10.0f;
            } else if (item == Items.DIAMOND_AXE) {
                damage = 9.0f;
            } else if (item == Items.IRON_AXE) {
                damage = 7.0f;
            } else if (item == Items.STONE_AXE) {
                damage = 6.0f;
            } else if (item == Items.WOODEN_AXE || item == Items.GOLDEN_AXE) {
                damage = 4.0f;
            }
        }
        
        if (damage > bestDamage) {
            bestDamage = damage;
            bestSlot = i;
        }
    }
    
    if (bestSlot != -1) {
        mc.player.getInventory().selectedSlot = bestSlot;
    }
}
  
  private void rotateToTarget(LivingEntity target) {
      double x = target.getX() - mc.player.getX();
      double z = target.getZ() - mc.player.getZ();
      double y = target.getEyeY() - mc.player.getEyeY();
      
      double dist = Math.sqrt(x * x + z * z);
      float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90F;
      float pitch = (float) -Math.toDegrees(Math.atan2(y, dist));
      
      mc.player.setYaw(yaw);
      mc.player.setPitch(pitch);
  }
}

