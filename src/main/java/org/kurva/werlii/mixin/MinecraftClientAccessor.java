package org.kurva.werlii.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
  @Accessor("attackCooldown")
  int getAttackCooldown();
  
  @Accessor("attackCooldown")
  void setAttackCooldown(int cooldown);
  
  @Accessor("itemUseCooldown")
  int getRightClickDelayTimer();
  
  @Accessor("itemUseCooldown")
  void setRightClickDelayTimer(int cooldown);
}

