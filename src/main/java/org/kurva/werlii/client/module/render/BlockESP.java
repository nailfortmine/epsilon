package org.kurva.werlii.client.module.render;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BlockESP extends Module {
    private final ModeSetting renderModeS;
    private final NumberSetting rangeS;
    private final BooleanSetting ironOreS;
    private final BooleanSetting goldOreS;
    private final BooleanSetting diamondOreS;
    private final BooleanSetting emeraldOreS;
    private final BooleanSetting redstoneOreS;
    private final BooleanSetting lapisOreS;
    private final BooleanSetting coalOreS;
    private final BooleanSetting ancientDebrisS;
    private final BooleanSetting netheriteBlockS;
    private final BooleanSetting chestS;
    private final BooleanSetting enderChestS;
    private final BooleanSetting shulkerBoxS;
    private final BooleanSetting spawnerS;
    private final BooleanSetting portalS;
    private final BooleanSetting bedrockS;
    private final BooleanSetting commandBlockS;
    private final BooleanSetting tracerS;
    private final NumberSetting alphaS;
    private final NumberSetting lineWidthS;
    
    private final ConcurrentHashMap<BlockPos, Block> foundBlocks = new ConcurrentHashMap<>();
    private long lastScanTime = 0;
    private Thread scanThread;
    private boolean scanning = false;
    
    public BlockESP() {
        super("BlockESP", "Highlights specific blocks through walls", Category.RENDER);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Render");
        
        // Add settings
        renderModeS = new ModeSetting("Render Mode", "How to render blocks", this, "Outline", "Outline", "Box", "Fill", "Full");
        rangeS = new NumberSetting("Range", "Block scan range", this, 32.0, 4.0, 128.0, 4.0);
        ironOreS = new BooleanSetting("Iron Ore", "Highlight iron ore", this, true);
        goldOreS = new BooleanSetting("Gold Ore", "Highlight gold ore", this, true);
        diamondOreS = new BooleanSetting("Diamond Ore", "Highlight diamond ore", this, true);
        emeraldOreS = new BooleanSetting("Emerald Ore", "Highlight emerald ore", this, true);
        redstoneOreS = new BooleanSetting("Redstone Ore", "Highlight redstone ore", this, true);
        lapisOreS = new BooleanSetting("Lapis Ore", "Highlight lapis ore", this, true);
        coalOreS = new BooleanSetting("Coal Ore", "Highlight coal ore", this, false);
        ancientDebrisS = new BooleanSetting("Ancient Debris", "Highlight ancient debris", this, true);
        netheriteBlockS = new BooleanSetting("Netherite Block", "Highlight netherite blocks", this, true);
        chestS = new BooleanSetting("Chest", "Highlight chests", this, true);
        enderChestS = new BooleanSetting("Ender Chest", "Highlight ender chests", this, true);
        shulkerBoxS = new BooleanSetting("Shulker Box", "Highlight shulker boxes", this, true);
        spawnerS = new BooleanSetting("Spawner", "Highlight mob spawners", this, true);
        portalS = new BooleanSetting("Portal", "Highlight nether portals", this, true);
        bedrockS = new BooleanSetting("Bedrock", "Highlight bedrock", this, false);
        commandBlockS = new BooleanSetting("Command Block", "Highlight command blocks", this, true);
        tracerS = new BooleanSetting("Tracer", "Draw lines to blocks", this, false);
        alphaS = new NumberSetting("Alpha", "Transparency of highlights", this, 0.4, 0.1, 1.0, 0.1);
        lineWidthS = new NumberSetting("Line Width", "Width of outline/tracer lines", this, 2.0, 1.0, 5.0, 0.5);
        
        addSetting(renderModeS);
        addSetting(rangeS);
        addSetting(ironOreS);
        addSetting(goldOreS);
        addSetting(diamondOreS);
        addSetting(emeraldOreS);
        addSetting(redstoneOreS);
        addSetting(lapisOreS);
        addSetting(coalOreS);
        addSetting(ancientDebrisS);
        addSetting(netheriteBlockS);
        addSetting(chestS);
        addSetting(enderChestS);
        addSetting(shulkerBoxS);
        addSetting(spawnerS);
        addSetting(portalS);
        addSetting(bedrockS);
        addSetting(commandBlockS);
        addSetting(tracerS);
        addSetting(alphaS);
        addSetting(lineWidthS);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        foundBlocks.clear();
        lastScanTime = 0;
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        if (scanning && scanThread != null) {
            scanning = false;
            try {
                scanThread.join(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        foundBlocks.clear();
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        long currentTime = System.currentTimeMillis();
{
            lastScanTime = currentTime;
            startScan();
        }
    }
    
    private void startScan() {
        if (scanning) return; // Don't start a new scan if one is already running
        
        scanning = true;
        scanThread = new Thread(() -> {
            try {
                scanBlocks();
            } catch (Exception e) {
                // Error handling
            } finally {
                scanning = false;
            }
        });
        
        scanThread.setName("Werlii-BlockESP-Scanner");
        scanThread.start();
    }
    
    private void scanBlocks() {
        if (mc.player == null || mc.world == null) return;
        
        int range = rangeS.getValue().intValue();
        BlockPos playerPos = mc.player.getBlockPos();
        Set<BlockPos> newlyFound = new HashSet<>();
        
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    if (!scanning) return; // Check if we need to abort
                    
                    BlockPos pos = playerPos.add(x, y, z);
                    
                    // Skip positions outside the world
                    if (pos.getY() < mc.world.getBottomY() || pos.getY() > mc.world.getTopY()) {
                        continue;
                    }
                    
                    // Skip positions too far away to reduce lag
                    if (playerPos.getSquaredDistance(pos) > range * range) {
                        continue;
                    }
                    
                    try {
                        Block block = mc.world.getBlockState(pos).getBlock();
                        
                        if (shouldHighlight(block)) {
                            newlyFound.add(pos);
                            foundBlocks.put(pos, block);
                        }
                    } catch (Exception e) {
                        // Skip this position if there's an error
                    }
                }
            }
        }
        
        // Remove blocks that weren't found in this scan
        foundBlocks.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            return !newlyFound.contains(pos) && 
                   playerPos.getSquaredDistance(pos) <= range * range;
        });
    }
    
    private boolean shouldHighlight(Block block) {
        if (ironOreS.getValue() && (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE)) return true;
        if (goldOreS.getValue() && (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE)) return true;
        if (diamondOreS.getValue() && (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)) return true;
        if (emeraldOreS.getValue() && (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE)) return true;
        if (redstoneOreS.getValue() && (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE)) return true;
        if (lapisOreS.getValue() && (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE)) return true;
        if (coalOreS.getValue() && (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE)) return true;
        if (ancientDebrisS.getValue() && block == Blocks.ANCIENT_DEBRIS) return true;
        if (netheriteBlockS.getValue() && block == Blocks.NETHERITE_BLOCK) return true;
        if (chestS.getValue() && (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST)) return true;
        if (enderChestS.getValue() && block == Blocks.ENDER_CHEST) return true;
        if (shulkerBoxS.getValue() && block.toString().contains("shulker_box")) return true;
        if (spawnerS.getValue() && block == Blocks.SPAWNER) return true;
        if (portalS.getValue() && block == Blocks.NETHER_PORTAL) return true;
        if (bedrockS.getValue() && block == Blocks.BEDROCK) return true;
        if (commandBlockS.getValue() && (block == Blocks.COMMAND_BLOCK || block == Blocks.CHAIN_COMMAND_BLOCK || block == Blocks.REPEATING_COMMAND_BLOCK)) return true;
        
        return false;
    }
    
    private int getBlockColor(Block block) {
        // Iron Ore - Light gray
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return 0xBCBCBC;
        
        // Gold Ore - Gold
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) return 0xFCF147;
        
        // Diamond Ore - Cyan
        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return 0x33EBCB;
        
        // Emerald Ore - Green
        if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return 0x17DD62;
        
        // Redstone Ore - Red
        if (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) return 0xFF0000;
        
        // Lapis Ore - Blue
        if (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE) return 0x0000FF;
        
        // Coal Ore - Dark gray
        if (block == Blocks.COAL_ORE || block == Blocks.DEEPSLATE_COAL_ORE) return 0x3F3F3F;
        
        // Ancient Debris - Brown
        if (block == Blocks.ANCIENT_DEBRIS) return 0x956C5A;
        
        // Netherite Block - Dark gray with purple tint
        if (block == Blocks.NETHERITE_BLOCK) return 0x4F4F5B;
        
        // Chest - Orange
        if (block == Blocks.CHEST) return 0xFFAA00;
        
        // Trapped Chest - Red-orange
        if (block == Blocks.TRAPPED_CHEST) return 0xFF5500;
        
        // Ender Chest - Purple
        if (block == Blocks.ENDER_CHEST) return 0x9A00FF;
        
        // Shulker Box - Magenta
        if (block.toString().contains("shulker_box")) return 0xFF00FF;
        
        // Spawner - Dark blue
        if (block == Blocks.SPAWNER) return 0x0000AA;
        
        // Portal - Purple
        if (block == Blocks.NETHER_PORTAL) return 0xAA00FF;
        
        // Bedrock - Black
        if (block == Blocks.BEDROCK) return 0x000000;
        
        // Command Blocks - Different shades of brown/gold
        if (block == Blocks.COMMAND_BLOCK) return 0xD4A82C;
        if (block == Blocks.CHAIN_COMMAND_BLOCK) return 0x83A696;
        if (block == Blocks.REPEATING_COMMAND_BLOCK) return 0x7C3FB1;
        
        // Default color if no match
        return 0xFFFFFF;
    }
    
    @Override
    public void onRender(float tickDelta) {
        if (mc.player == null || mc.world == null || foundBlocks.isEmpty()) return;
        
        // For rendering blocks
        for (BlockPos pos : foundBlocks.keySet()) {
            Block block = foundBlocks.get(pos);
            int color = getBlockColor(block);
            
            switch (renderModeS.getValue()) {
                case "Outline":
                    drawOutline(pos, color);
                    break;
                case "Box":
                    drawBox(pos, color);
                    break;
                case "Fill":
                    drawFill(pos, color);
                    break;
                case "Full":
                    drawFill(pos, color);
                    drawOutline(pos, color);
                    break;
            }
            
            // Draw tracer if enabled
            if (tracerS.getValue()) {
                drawTracer(pos, color);
            }
        }
    }
    
    private void drawOutline(BlockPos pos, int color) {
        // This would be implemented with proper rendering code in a real client
        // The implementation would draw an outline around the block
    }
    
    private void drawBox(BlockPos pos, int color) {
        // This would be implemented with proper rendering code in a real client
        // The implementation would draw a box around the block
    }
    
    private void drawFill(BlockPos pos, int color) {
        // This would be implemented with proper rendering code in a real client
        // The implementation would fill the block with a colored overlay
    }
    
    private void drawTracer(BlockPos pos, int color) {
        // This would be implemented with proper rendering code in a real client
        // The implementation would draw a line from the player to the block
    }
}

