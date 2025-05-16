package org.kurva.werlii.client.module.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.util.font.FontManager;
import org.kurva.werlii.client.util.font.FontRenderer;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

public class HUD extends Module {
    private boolean showWatermark = true;
    private boolean showFPS = true;
    private boolean showCoordinates = true;
    private boolean showModuleList = true;
    private boolean showArmor = true;
    private boolean showPing = true;
    private boolean rainbowText = true;
    private boolean showServerInfo = true;

    // Font settings
    private String fontName = "montserrat";
    private float fontSize = 18.0f;

    public HUD() {
        super("HUD", "Displays active modules and other information", Category.RENDER);
        this.setKeyCode(GLFW.GLFW_KEY_P);
        this.registerKeybinding("Werlii Render");
        this.setEnabled(true); // Enable by default
    }

    public void render(DrawContext context, float tickDelta) {
        if (!isEnabled() || mc.player == null) return;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        // Get custom font module
        CustomFont customFont = (CustomFont) WerliiClient.getInstance().getModuleManager().getModuleByName("CustomFont");
        boolean useCustomFont = customFont != null && customFont.isEnabled() && customFont.shouldUseCustomFont();
        boolean useShadow = customFont != null && customFont.shouldDrawShadow();

        // Get font renderer
        final FontRenderer fontRenderer = useCustomFont ?
                (FontManager.getInstance().getFont(fontName) != null ?
                        FontManager.getInstance().getFont(fontName) :
                        FontManager.getInstance().getDefaultFont()) :
                null;

        // Render watermark
        if (showWatermark) {
            String watermark = "Werlii v1.0";
            if (useCustomFont) {
                fontRenderer.drawString(context, watermark, 2, 2, 0x00AAFF, useShadow);
            } else {
                context.drawTextWithShadow(mc.textRenderer, watermark, 2, 2, 0x00AAFF);
            }
        }

        int y = 2;

        // Render FPS
        if (showFPS) {
            String fps = mc.fpsDebugString.split(" ")[0] + " FPS";
            if (useCustomFont) {
                y += fontRenderer.getFontHeight();
                fontRenderer.drawString(context, fps, 2, y, 0xFFFFFF, useShadow);
            } else {
                y += 10;
                context.drawTextWithShadow(mc.textRenderer, fps, 2, y, 0xFFFFFF);
            }
        }

        // Render coordinates
        if (showCoordinates) {
            String coords = String.format("XYZ: %.1f / %.1f / %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            if (useCustomFont) {
                y += fontRenderer.getFontHeight();
                fontRenderer.drawString(context, coords, 2, y, 0xFFFFFF, useShadow);
            } else {
                y += 10;
                context.drawTextWithShadow(mc.textRenderer, coords, 2, y, 0xFFFFFF);
            }
        }

        // Render ping if enabled
        if (showPing && mc.getNetworkHandler() != null) {
            int ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null ?
                    mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency() : 0;
            String pingText = "Ping: " + ping + "ms";
            if (useCustomFont) {
                y += fontRenderer.getFontHeight();
                fontRenderer.drawString(context, pingText, 2, y, 0xFFFFFF, useShadow);
            } else {
                y += 10;
                context.drawTextWithShadow(mc.textRenderer, pingText, 2, y, 0xFFFFFF);
            }
        }

        // Render server info
        if (showServerInfo && mc.getCurrentServerEntry() != null) {
            String serverInfo = "Server: " + mc.getCurrentServerEntry().address;
            if (useCustomFont) {
                y += fontRenderer.getFontHeight();
                fontRenderer.drawString(context, serverInfo, 2, y, 0xFFFFFF, useShadow);
            } else {
                y += 10;
                context.drawTextWithShadow(mc.textRenderer, serverInfo, 2, y, 0xFFFFFF);
            }

            String gameMode = mc.isInSingleplayer() ? "Singleplayer" : "Multiplayer";
            if (useCustomFont) {
                y += fontRenderer.getFontHeight();
                fontRenderer.drawString(context, gameMode, 2, y, 0xFFFFFF, useShadow);
            } else {
                y += 10;
                context.drawTextWithShadow(mc.textRenderer, gameMode, 2, y, 0xFFFFFF);
            }
        }

        // Render active modules (ArrayList)
        if (showModuleList) {
            List<Module> enabledModules = WerliiClient.getInstance().getModuleManager().getEnabledModules();

            // Sort modules by name length for alignment
            if (useCustomFont) {
                enabledModules.sort(Comparator.comparing(module ->
                        -fontRenderer.getStringWidth(module.getName())));
            } else {
                // Make TextRenderer final to use in lambda
                final net.minecraft.client.font.TextRenderer textRenderer = mc.textRenderer;
                enabledModules.sort(Comparator.comparing(module ->
                        -textRenderer.getWidth(Text.of(module.getName()))));
            }

            int moduleY = 2;
            for (Module module : enabledModules) {
                if (module == this) continue; // Don't show HUD in the list
                if (!module.isVisible()) continue; // Skip invisible modules

                String name = module.getName();
                int color = rainbowText ? getRainbowColor(moduleY * 100) : 0xFFFFFF;

                if (useCustomFont) {
                    int width = fontRenderer.getStringWidth(name);
                    fontRenderer.drawString(context, name, screenWidth - width - 2, moduleY, color, useShadow);
                    moduleY += fontRenderer.getFontHeight();
                } else {
                    int width = mc.textRenderer.getWidth(Text.of(name));
                    context.drawTextWithShadow(mc.textRenderer, name, screenWidth - width - 2, moduleY, color);
                    moduleY += 10;
                }
            }
        }

        // Render armor if enabled
        if (showArmor) {
            renderArmorStatus(context, screenWidth, screenHeight, useCustomFont, fontRenderer, useShadow);
        }
    }

    private void renderArmorStatus(DrawContext context, int screenWidth, int screenHeight, boolean useCustomFont, FontRenderer fontRenderer, boolean useShadow) {
        int x = screenWidth / 2 - 90;
        int y = screenHeight - 55;

        // Draw armor pieces
        for (int i = 0; i < 4; i++) {
            ItemStack stack = mc.player.getInventory().getArmorStack(i);
            if (!stack.isEmpty()) {
                int durability = stack.getMaxDamage() - stack.getDamage();
                int maxDurability = stack.getMaxDamage();
                float durabilityPercent = maxDurability > 0 ? (float) durability / maxDurability : 1.0f;

                // Draw item
                context.drawItem(stack, x + i * 20, y);

                // Draw durability text
                String durText = (int) (durabilityPercent * 100) + "%";
                int color = getDurabilityColor(durabilityPercent);

                if (useCustomFont) {
                    int textWidth = fontRenderer.getStringWidth(durText);
                    fontRenderer.drawString(context, durText, x + i * 20 + 8 - textWidth / 2, y + 18, color, useShadow);
                } else {
                    context.drawTextWithShadow(mc.textRenderer, durText, x + i * 20 + 8 - mc.textRenderer.getWidth(Text.of(durText)) / 2, y + 18, color);
                }
            }
        }
    }

    private int getDurabilityColor(float durabilityPercent) {
        if (durabilityPercent > 0.75f) {
            return 0x00FF00; // Green
        } else if (durabilityPercent > 0.5f) {
            return 0xFFFF00; // Yellow
        } else if (durabilityPercent > 0.25f) {
            return 0xFFA500; // Orange
        } else {
            return 0xFF0000; // Red
        }
    }

    private int getRainbowColor(int offset) {
        float hue = (System.currentTimeMillis() + offset) % 10000 / 10000f;
        int color = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);
        return color;
    }

    // Getter and setter for font name
    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    // Getter and setter for font size
    public float getFontSize() {
        return fontSize;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }
}

