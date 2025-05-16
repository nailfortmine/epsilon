package org.kurva.werlii.client.util;

import net.minecraft.entity.player.PlayerEntity;

public class EntityUtil {
    
    public static boolean isEnemy(PlayerEntity player) {
        return true;
    }
    
    public static boolean isValidTarget(PlayerEntity player) {
        return player != null && !player.isSpectator() && player.isAlive() && !player.isInvisible();
    }
}

