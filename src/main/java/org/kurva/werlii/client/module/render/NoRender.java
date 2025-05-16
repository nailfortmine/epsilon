package org.kurva.werlii.client.module.render;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class NoRender extends Module {
    // Screen overlays
    private final BooleanSetting fireS;
    private final BooleanSetting waterS;
    private final BooleanSetting portalS;
    private final BooleanSetting pumpkinS;
    private final BooleanSetting blindnessS;
    private final BooleanSetting nauseaS;
    
    // World effects
    private final BooleanSetting weatherS;
    private final BooleanSetting fogS;
    private final BooleanSetting explosionsS;
    private final BooleanSetting totemAnimationS;
    private final BooleanSetting fireworksS;
    
    // Entity effects
    private final BooleanSetting potionParticlesS;
    private final BooleanSetting armorS;
    private final BooleanSetting enchantmentGlintS;
    private final BooleanSetting damageS;
    
    // HUD elements
    private final BooleanSetting bossBarS;
    private final BooleanSetting scoreboardS;
    private final BooleanSetting crosshairS;
    private final BooleanSetting potionIconsS;
    
    // Block effects
    private final BooleanSetting signTextS;
    private final BooleanSetting blockBreakS;
    private final BooleanSetting mapS;
    
    // Potion effects to hide
    private final Set<StatusEffect> hiddenEffects = new HashSet<>();
    
    public NoRender() {
        super("NoRender", "Prevents rendering of various visual effects", Category.RENDER);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Render");
        
        // Screen overlays
        fireS = new BooleanSetting("Fire", "Prevents fire overlay when burning", this, true);
        waterS = new BooleanSetting("Water", "Prevents water overlay when underwater", this, false);
        portalS = new BooleanSetting("Portal", "Prevents nether portal overlay", this, true);
        pumpkinS = new BooleanSetting("Pumpkin", "Prevents pumpkin overlay when wearing", this, true);
        blindnessS = new BooleanSetting("Blindness", "Prevents blindness effect", this, true);
        nauseaS = new BooleanSetting("Nausea", "Prevents nausea effect", this, true);
        
        // World effects
        weatherS = new BooleanSetting("Weather", "Prevents weather effects (rain, snow)", this, false);
        fogS = new BooleanSetting("Fog", "Prevents fog rendering", this, false);
        explosionsS = new BooleanSetting("Explosions", "Prevents explosion particles", this, false);
        totemAnimationS = new BooleanSetting("Totem Animation", "Prevents totem animation", this, false);
        fireworksS = new BooleanSetting("Fireworks", "Prevents firework particles", this, false);
        
        // Entity effects
        potionParticlesS = new BooleanSetting("Potion Particles", "Prevents potion effect particles", this, true);
        armorS = new BooleanSetting("Armor", "Prevents armor rendering on entities", this, false);
        enchantmentGlintS = new BooleanSetting("Enchantment Glint", "Prevents enchantment glint", this, false);
        damageS = new BooleanSetting("Damage", "Prevents damage indicators", this, false);
        
        // HUD elements
        bossBarS = new BooleanSetting("Boss Bar", "Prevents boss bar rendering", this, false);
        scoreboardS = new BooleanSetting("Scoreboard", "Prevents scoreboard rendering", this, false);
        crosshairS = new BooleanSetting("Crosshair", "Prevents crosshair rendering", this, false);
        potionIconsS = new BooleanSetting("Potion Icons", "Prevents potion effect icons", this, true);
        
        // Block effects
        signTextS = new BooleanSetting("Sign Text", "Prevents sign text rendering", this, false);
        blockBreakS = new BooleanSetting("Block Break", "Prevents block breaking particles", this, false);
        mapS = new BooleanSetting("Map", "Prevents map rendering in hand", this, false);
        
        // Add settings - Screen overlays
        addSetting(fireS);
        addSetting(waterS);
        addSetting(portalS);
        addSetting(pumpkinS);
        addSetting(blindnessS);
        addSetting(nauseaS);
        
        // Add settings - World effects
        addSetting(weatherS);
        addSetting(fogS);
        addSetting(explosionsS);
        addSetting(totemAnimationS);
        addSetting(fireworksS);
        
        // Add settings - Entity effects
        addSetting(potionParticlesS);
        addSetting(armorS);
        addSetting(enchantmentGlintS);
        addSetting(damageS);
        
        // Add settings - HUD elements
        addSetting(bossBarS);
        addSetting(scoreboardS);
        addSetting(crosshairS);
        addSetting(potionIconsS);
        
        // Add settings - Block effects
        addSetting(signTextS);
        addSetting(blockBreakS);
        addSetting(mapS);
        
        // Initialize hidden effects
        updateHiddenEffects();
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        updateHiddenEffects();
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        // Remove visual effects from player if enabled
        if (blindnessS.getValue()) {
            mc.player.removeStatusEffect(StatusEffects.BLINDNESS);
        }
        
        if (nauseaS.getValue()) {
            mc.player.removeStatusEffect(StatusEffects.NAUSEA);
        }
    }
    
    private void updateHiddenEffects() {
        hiddenEffects.clear();
        
        if (blindnessS.getValue()) {

        }

        if (nauseaS.getValue()) {
        }
    }
    
    /**
     * Check if fire overlay should be rendered
     * This would be called from a mixin to prevent fire overlay rendering
     */
    public boolean shouldRenderFire() {
        return !isEnabled() || !fireS.getValue();
    }
    
    /**
     * Check if water overlay should be rendered
     * This would be called from a mixin to prevent water overlay rendering
     */
    public boolean shouldRenderWater() {
        return !isEnabled() || !waterS.getValue();
    }
    
    /**
     * Check if portal overlay should be rendered
     * This would be called from a mixin to prevent portal overlay rendering
     */
    public boolean shouldRenderPortal() {
        return !isEnabled() || !portalS.getValue();
    }
    
    /**
     * Check if pumpkin overlay should be rendered
     * This would be called from a mixin to prevent pumpkin overlay rendering
     */
    public boolean shouldRenderPumpkin() {
        return !isEnabled() || !pumpkinS.getValue();
    }
    
    /**
     * Check if weather effects should be rendered
     * This would be called from a mixin to prevent weather rendering
     */
    public boolean shouldRenderWeather() {
        return !isEnabled() || !weatherS.getValue();
    }
    
    /**
     * Check if fog should be rendered
     * This would be called from a mixin to prevent fog rendering
     */
    public boolean shouldRenderFog() {
        return !isEnabled() || !fogS.getValue();
    }
    
    /**
     * Check if explosions should be rendered
     * This would be called from a mixin to prevent explosion particles
     */
    public boolean shouldRenderExplosions() {
        return !isEnabled() || !explosionsS.getValue();
    }
    
    /**
     * Check if totem animation should be rendered
     * This would be called from a mixin to prevent totem animation
     */
    public boolean shouldRenderTotemAnimation() {
        return !isEnabled() || !totemAnimationS.getValue();
    }
    
    /**
     * Check if fireworks should be rendered
     * This would be called from a mixin to prevent firework particles
     */
    public boolean shouldRenderFireworks() {
        return !isEnabled() || !fireworksS.getValue();
    }
    
    /**
     * Check if potion particles should be rendered
     * This would be called from a mixin to prevent potion particles
     */
    public boolean shouldRenderPotionParticles() {
        return !isEnabled() || !potionParticlesS.getValue();
    }
    
    /**
     * Check if armor should be rendered
     * This would be called from a mixin to prevent armor rendering
     */
    public boolean shouldRenderArmor() {
        return !isEnabled() || !armorS.getValue();
    }
    
    /**
     * Check if enchantment glint should be rendered
     * This would be called from a mixin to prevent enchantment glint
     */
    public boolean shouldRenderEnchantmentGlint() {
        return !isEnabled() || !enchantmentGlintS.getValue();
    }
    
    /**
     * Check if damage indicators should be rendered
     * This would be called from a mixin to prevent damage indicators
     */
    public boolean shouldRenderDamage() {
        return !isEnabled() || !damageS.getValue();
    }
    
    /**
     * Check if boss bar should be rendered
     * This would be called from a mixin to prevent boss bar rendering
     */
    public boolean shouldRenderBossBar() {
        return !isEnabled() || !bossBarS.getValue();
    }
    
    /**
     * Check if scoreboard should be rendered
     * This would be called from a mixin to prevent scoreboard rendering
     */
    public boolean shouldRenderScoreboard() {
        return !isEnabled() || !scoreboardS.getValue();
    }
    
    /**
     * Check if crosshair should be rendered
     * This would be called from a mixin to prevent crosshair rendering
     */
    public boolean shouldRenderCrosshair() {
        return !isEnabled() || !crosshairS.getValue();
    }
    
    /**
     * Check if potion icons should be rendered
     * This would be called from a mixin to prevent potion icons
     */
    public boolean shouldRenderPotionIcons() {
        return !isEnabled() || !potionIconsS.getValue();
    }
    
    /**
     * Check if sign text should be rendered
     * This would be called from a mixin to prevent sign text rendering
     */
    public boolean shouldRenderSignText() {
        return !isEnabled() || !signTextS.getValue();
    }
    
    /**
     * Check if block break particles should be rendered
     * This would be called from a mixin to prevent block break particles
     */
    public boolean shouldRenderBlockBreak() {
        return !isEnabled() || !blockBreakS.getValue();
    }
    
    /**
     * Check if maps should be rendered
     * This would be called from a mixin to prevent map rendering
     */
    public boolean shouldRenderMap() {
        return !isEnabled() || !mapS.getValue();
    }
    
    /**
     * Check if a specific status effect should be rendered
     * This would be called from a mixin to prevent status effects
     */
    public boolean shouldRenderStatusEffect(StatusEffect effect) {
        return !isEnabled() || !hiddenEffects.contains(effect);
    }
    
    /**
     * This would be implemented via a mixin to prevent fire overlay rendering
     */
    public static class InGameOverlayRendererMixin {
        /**
         * Example of how the mixin would be implemented
         */
        public static boolean shouldRenderFireOverlay() {
            NoRender noRender = (NoRender) WerliiClient.getInstance().getModuleManager().getModuleByName("NoRender");
            return noRender == null || noRender.shouldRenderFire();
        }
        
        /**
         * Example of how the mixin would be implemented
         */
        public static boolean shouldRenderWaterOverlay() {
            NoRender noRender = (NoRender) WerliiClient.getInstance().getModuleManager().getModuleByName("NoRender");
            return noRender == null || noRender.shouldRenderWater();
        }
    }
}

