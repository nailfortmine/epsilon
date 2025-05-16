package org.kurva.werlii.client.module.render;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class XRay extends Module {
    private final ModeSetting modeS;
    private final NumberSetting opacityS;
    private final BooleanSetting fullbrightS;
    private final BooleanSetting outlineS;
    private final BooleanSetting caveModeS;
    
    // Block settings
    private final BooleanSetting diamondOreS;
    private final BooleanSetting emeraldOreS;
    private final BooleanSetting goldOreS;
    private final BooleanSetting ironOreS;
    private final BooleanSetting coalOreS;
    private final BooleanSetting redstoneOreS;
    private final BooleanSetting lapisOreS;
    private final BooleanSetting quartzOreS;
    private final BooleanSetting ancientDebrisS;
    private final BooleanSetting chestS;
    private final BooleanSetting spawnerS;
    private final BooleanSetting portalS;
    private final BooleanSetting bedrockS;
    private final BooleanSetting commandBlockS;
    
    private final Set<Block> xrayBlocks = new HashSet<>();
    private float originalGamma;
    
    public XRay() {
        super("XRay", "See through blocks to find ores and structures", Category.RENDER);
        this.setKeyCode(GLFW.GLFW_KEY_X);
        this.registerKeybinding("Werlii Render");
        
        // Add settings
        modeS = new ModeSetting("Mode", "XRay rendering method", this, "Normal", "Normal", "Shader", "Ambient Occlusion");
        opacityS = new NumberSetting("Opacity", "Opacity of non-XRay blocks", this, 0.0, 0.0, 1.0, 0.1);
        fullbrightS = new BooleanSetting("Fullbright", "Make everything bright", this, true);
        outlineS = new BooleanSetting("Outline", "Outline visible blocks", this, true);
        caveModeS = new BooleanSetting("Cave Mode", "Only show blocks exposed to air", this, false);
        
        // Block settings
        diamondOreS = new BooleanSetting("Diamond Ore", "Show diamond ore", this, true);
        emeraldOreS = new BooleanSetting("Emerald Ore", "Show emerald ore", this, true);
        goldOreS = new BooleanSetting("Gold Ore", "Show gold ore", this, true);
        ironOreS = new BooleanSetting("Iron Ore", "Show iron ore", this, true);
        coalOreS = new BooleanSetting("Coal Ore", "Show coal ore", this, false);
        redstoneOreS = new BooleanSetting("Redstone Ore", "Show redstone ore", this, true);
        lapisOreS = new BooleanSetting("Lapis Ore", "Show lapis ore", this, true);
        quartzOreS = new BooleanSetting("Quartz Ore", "Show nether quartz ore", this, true);
        ancientDebrisS = new BooleanSetting("Ancient Debris", "Show ancient debris", this, true);
        chestS = new BooleanSetting("Chest", "Show chests", this, true);
        spawnerS = new BooleanSetting("Spawner", "Show mob spawners", this, true);
        portalS = new BooleanSetting("Portal", "Show nether portals", this, true);
        bedrockS = new BooleanSetting("Bedrock", "Show bedrock", this, false);
        commandBlockS = new BooleanSetting("Command Block", "Show command blocks", this, true);
        
        // Add ore settings
        addSetting(modeS);
        addSetting(opacityS);
        addSetting(fullbrightS);
        addSetting(outlineS);
        addSetting(caveModeS);
        addSetting(diamondOreS);
        addSetting(emeraldOreS);
        addSetting(goldOreS);
        addSetting(ironOreS);
        addSetting(coalOreS);
        addSetting(redstoneOreS);
        addSetting(lapisOreS);
        addSetting(quartzOreS);
        addSetting(ancientDebrisS);
        addSetting(chestS);
        addSetting(spawnerS);
        addSetting(portalS);
        addSetting(bedrockS);
        addSetting(commandBlockS);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        
        if (mc.options == null) return;
        
        // Save original gamma
        
        // Update XRay blocks
        updateXRayBlocks();
        
        // Set fullbright if enabled
        if (fullbrightS.getValue()) {
            mc.options.getGamma().setValue(100.0);
        }
        
        // Force reload chunks
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        
        if (mc.options == null) return;
        
        // Restore gamma
        
        // Clean up shader if used

        
        // Force reload chunks
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }
    
    private void updateXRayBlocks() {
        xrayBlocks.clear();
        
        // Add enabled ore blocks
        if (diamondOreS.getValue()) {
            xrayBlocks.add(Blocks.DIAMOND_ORE);
            xrayBlocks.add(Blocks.DEEPSLATE_DIAMOND_ORE);
        }
        
        if (emeraldOreS.getValue()) {
            xrayBlocks.add(Blocks.EMERALD_ORE);
            xrayBlocks.add(Blocks.DEEPSLATE_EMERALD_ORE);
        }
        
        if (goldOreS.getValue()) {
            xrayBlocks.add(Blocks.GOLD_ORE);
            xrayBlocks.add(Blocks.DEEPSLATE_GOLD_ORE);
            xrayBlocks.add(Blocks.NETHER_GOLD_ORE);
        }
        
        if (ironOreS.getValue()) {
            xrayBlocks.add(Blocks.IRON_ORE);
            xrayBlocks.add(Blocks.DEEPSLATE_IRON_ORE);
        }
        
        if (coalOreS.getValue()) {
            xrayBlocks.add(Blocks.COAL_ORE);
            xrayBlocks.add(Blocks.DEEPSLATE_COAL_ORE);
        }
        
        if (redstoneOreS.getValue()) {
            xrayBlocks.add(Blocks.REDSTONE_ORE);
            xrayBlocks.add(Blocks.DEEPSLATE_REDSTONE_ORE);
        }
        
        if (lapisOreS.getValue()) {
            xrayBlocks.add(Blocks.LAPIS_ORE);
            xrayBlocks.add(Blocks.DEEPSLATE_LAPIS_ORE);
        }
        
        if (quartzOreS.getValue()) {
            xrayBlocks.add(Blocks.NETHER_QUARTZ_ORE);
        }
        
        if (ancientDebrisS.getValue()) {
            xrayBlocks.add(Blocks.ANCIENT_DEBRIS);
        }
        
        // Add non-ore blocks
        if (chestS.getValue()) {
            xrayBlocks.add(Blocks.CHEST);
            xrayBlocks.add(Blocks.TRAPPED_CHEST);
            xrayBlocks.add(Blocks.BARREL);
        }
        
        if (spawnerS.getValue()) {
            xrayBlocks.add(Blocks.SPAWNER);
        }
        
        if (portalS.getValue()) {
            xrayBlocks.add(Blocks.NETHER_PORTAL);
            xrayBlocks.add(Blocks.END_PORTAL);
            xrayBlocks.add(Blocks.END_PORTAL_FRAME);
        }
        
        if (bedrockS.getValue()) {
            xrayBlocks.add(Blocks.BEDROCK);
        }
        
        if (commandBlockS.getValue()) {
            xrayBlocks.add(Blocks.COMMAND_BLOCK);
            xrayBlocks.add(Blocks.CHAIN_COMMAND_BLOCK);
            xrayBlocks.add(Blocks.REPEATING_COMMAND_BLOCK);
        }
    }
    
    /**
     * Check if a block should be visible in XRay
     * This would be called from a mixin to control block rendering
     */
    public boolean shouldRenderBlock(Block block) {
        if (!isEnabled()) return true;
        
        return xrayBlocks.contains(block);
    }
    
    /**
     * Check if fullbright is enabled
     * This would be called from a mixin to control brightness
     */
    public boolean isFullbrightEnabled() {
        return isEnabled() && fullbrightS.getValue();
    }
    
    /**
     * Get opacity for non-XRay blocks
     * This would be called from a mixin to control block opacity
     */
    public float getNonXRayOpacity() {
        return isEnabled() ? opacityS.getValue().floatValue() : 1.0f;
    }
    
    /**
     * Check if cave mode is enabled
     * This would be called from a mixin to check if blocks should only be visible when exposed to air
     */
    public boolean isCaveModeEnabled() {
        return isEnabled() && caveModeS.getValue();
    }
    
    /**
     * Update XRay settings
     * This would be called when settings are changed
     */
    public void updateXRay() {
        if (isEnabled()) {
            updateXRayBlocks();
            
            // Force reload chunks
            if (mc.worldRenderer != null) {
                mc.worldRenderer.reload();
            }
        }
    }
}

