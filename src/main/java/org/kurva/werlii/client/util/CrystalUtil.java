package org.kurva.werlii.client.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

public class CrystalUtil {
    
    public static float calculateDamage(World world, double posX, double posY, double posZ, Entity entity) {
        float doubleExplosionSize = 12.0F;
        double distancedSize = entity.getPos().distanceTo(new Vec3d(posX, posY, posZ)) / doubleExplosionSize;
        
        if (distancedSize > 1.0) {
            return 0.0F;
        }
        
        double blockDensity = getBlockDensity(world, new Vec3d(posX, posY, posZ), entity.getBoundingBox());
        double v = (1.0 - distancedSize) * blockDensity;
        float damage = (float) ((v * v + v) / 2.0 * 7.0 * doubleExplosionSize + 1.0);
        
        if (entity instanceof LivingEntity) {
            damage = getDamageAfterAbsorb(damage, ((LivingEntity) entity).getArmor(), ((LivingEntity) entity).getAbsorptionAmount());
        }
        
        return damage;
    }
    
    private static float getDamageAfterAbsorb(float damage, int armor, float absorptionAmount) {
        float f = 1.0F - MathHelper.clamp(armor - damage / 2.0F, 0.0F, 20.0F) / 25.0F;
        float f1 = damage * f;
        
        if (absorptionAmount > 0.0F) {
            f1 = Math.max(f1 - absorptionAmount, 0.0F);
        }
        
        return f1;
    }
    
    private static float getBlockDensity(World world, Vec3d explosionPosition, Box boundingBox) {
        double d0 = 1.0 / ((boundingBox.maxX - boundingBox.minX) * 2.0 + 1.0);
        double d1 = 1.0 / ((boundingBox.maxY - boundingBox.minY) * 2.0 + 1.0);
        double d2 = 1.0 / ((boundingBox.maxZ - boundingBox.minZ) * 2.0 + 1.0);
        
        return 0.75F;
    }
}

