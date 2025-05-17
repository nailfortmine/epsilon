package org.kurva.werlii.client.module.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.util.font.FontManager;
import org.kurva.werlii.client.util.font.FontRenderer;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

public class HUD extends Module {
    private static final int WATERMARK_COLOR = 0x00AAFF;
    private static final int DEFAULT_TEXT_COLOR = 0xFFFFFF;

    private boolean showWatermark = true;
    private boolean showFPS = true;
    private boolean showCoordinates = true;
    private boolean showModuleList = true;
    private boolean showArmor = true;
    private boolean showPing = true;
    private boolean rainbowText = true;
    private boolean showServerInfo = true;

    private String fontName = "montserrat";
    private float fontSize = 18.0f;

    public HUD() {
        super("HUD", "Displays active modules and other information", Category.RENDER);
        setKeyCode(GLFW.GLFW_KEY_P);
        registerKeybinding("Werlii Render");
        setEnabled(true);
    }


    public void render(DrawContext context, float tickDelta) {
        if (!isEnabled() || mc.player == null) return;

        FontRenderer fontRenderer = getFontRenderer();
        boolean useCustomFont = fontRenderer != null;
        boolean useShadow = useCustomFont && isShadowEnabled();

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int y = 2;

        // Render left-side HUD elements
        y = renderLeftHUD(context, fontRenderer, useCustomFont, useShadow, y);

        // Render module list
        if (showModuleList) {
            renderModuleList(context, fontRenderer, useCustomFont, useShadow, screenWidth);
        }

        // Render armor status
        if (showArmor) {
            renderArmorStatus(context, fontRenderer, useCustomFont, useShadow, screenWidth, screenHeight);
        }
    }

    private FontRenderer getFontRenderer() {
        CustomFont customFont = (CustomFont) WerliiClient.getInstance().getModuleManager().getModuleByName("CustomFont");
        if (customFont == null || !customFont.isEnabled() || !customFont.shouldUseCustomFont()) {
            return null;
        }
        FontRenderer font = FontManager.getInstance().getFont(fontName);
        return font != null ? font : FontManager.getInstance().getDefaultFont();
    }

    private boolean isShadowEnabled() {
        CustomFont customFont = (CustomFont) WerliiClient.getInstance().getModuleManager().getModuleByName("CustomFont");
        return customFont != null && customFont.shouldDrawShadow();
    }

    private int renderLeftHUD(DrawContext context, FontRenderer fontRenderer, boolean useCustomFont, boolean useShadow, int y) {
        // Watermark
        if (showWatermark) {
            drawText(context, fontRenderer, useCustomFont, useShadow, "Werlii v1.0", 2, y, WATERMARK_COLOR);
            y += useCustomFont ? fontRenderer.getFontHeight() : 10;
        }

        // FPS
        if (showFPS) {
            drawText(context, fontRenderer, useCustomFont, useShadow, mc.fpsDebugString.split(" ")[0] + " FPS", 2, y, DEFAULT_TEXT_COLOR);
            y += useCustomFont ? fontRenderer.getFontHeight() : 10;
        }

        // Coordinates
        if (showCoordinates) {
            String coords = String.format("XYZ: %.1f / %.1f / %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            drawText(context, fontRenderer, useCustomFont, useShadow, coords, 2, y, DEFAULT_TEXT_COLOR);
            y += useCustomFont ? fontRenderer.getFontHeight() : 10;
        }

        // Ping
        if (showPing && mc.getNetworkHandler() != null) {
            int ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null
                    ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency() : 0;
            drawText(context, fontRenderer, useCustomFont, useShadow, "Ping: " + ping + "ms", 2, y, DEFAULT_TEXT_COLOR);
            y += useCustomFont ? fontRenderer.getFontHeight() : 10;
        }

        // Server Info
        if (showServerInfo && mc.getCurrentServerEntry() != null) {
            drawText(context, fontRenderer, useCustomFont, useShadow, "Server: " + mc.getCurrentServerEntry().address, 2, y, DEFAULT_TEXT_COLOR);
            y += useCustomFont ? fontRenderer.getFontHeight() : 10;
            String gameMode = mc.isInSingleplayer() ? "Singleplayer" : "Multiplayer";
            drawText(context, fontRenderer, useCustomFont, useShadow, gameMode, 2, y, DEFAULT_TEXT_COLOR);
            y += useCustomFont ? fontRenderer.getFontHeight() : 10;
        }

        return y;
    }

    private void drawText(DrawContext context, FontRenderer fontRenderer, boolean useCustomFont, boolean useShadow, String text, int x, int y, int color) {
        if (useCustomFont) {
            fontRenderer.drawString(context, text, x, y, color, useShadow);
        } else {
            context.drawTextWithShadow(mc.textRenderer, text, x, y, color);
        }
    }

    private void renderModuleList(DrawContext context, FontRenderer fontRenderer, boolean useCustomFont, boolean useShadow, int screenWidth) {
        List<Module> enabledModules = WerliiClient.getInstance().getModuleManager().getEnabledModules();
        enabledModules.sort(Comparator.comparingInt(module ->
                useCustomFont ? -fontRenderer.getStringWidth(module.getName()) : -mc.textRenderer.getWidth(Text.of(module.getName()))));

        int moduleY = 2;
        for (Module module : enabledModules) {
            if (module == this || !module.isVisible()) continue;

            String name = module.getName();
            int color = rainbowText ? getRainbowColor(moduleY * 100) : DEFAULT_TEXT_COLOR;
            int width = useCustomFont ? fontRenderer.getStringWidth(name) : mc.textRenderer.getWidth(Text.of(name));

            drawText(context, fontRenderer, useCustomFont, useShadow, name, screenWidth - width - 2, moduleY, color);
            moduleY += useCustomFont ? fontRenderer.getFontHeight() : 10;
        }
    }

    private void renderArmorStatus(DrawContext context, FontRenderer fontRenderer, boolean useCustomFont, boolean useShadow, int screenWidth, int screenHeight) {
        int x = screenWidth / 2 - 90;
        int y = screenHeight - 55;

        for (int i = 0; i < 4; i++) {
            ItemStack stack = mc.player.getInventory().getArmorStack(i);
            if (stack.isEmpty()) continue;

            int durability = stack.getMaxDamage() - stack.getDamage();
            int maxDurability = stack.getMaxDamage();
            float durabilityPercent = maxDurability > 0 ? (float) durability / maxDurability : 1.0f;

            context.drawItem(stack, x + i * 20, y);
            String durText = (int) (durabilityPercent * 100) + "%";
            int color = getDurabilityColor(durabilityPercent);
            int textWidth = useCustomFont ? fontRenderer.getStringWidth(durText) : mc.textRenderer.getWidth(Text.of(durText));

            drawText(context, fontRenderer, useCustomFont, useShadow, durText, x + i * 20 + 8 - textWidth / 2, y + 18, color);
        }
    }

    private int getDurabilityColor(float durabilityPercent) {
        if (durabilityPercent > 0.75f) return 0x00FF00;
        if (durabilityPercent > 0.5f) return 0xFFFF00;
        if (durabilityPercent > 0.25f) return 0xFFA500;
        return 0xFF0000;
    }

    private int getRainbowColor(int offset) {
        return java.awt.Color.HSBtoRGB((System.currentTimeMillis() + offset) % 10000 / 10000f, 1.0f, 1.0f);
    }

    // Getters and setters
    public String getFontName() { return fontName; }
    public void setFontName(String fontName) { this.fontName = fontName; }
    public float getFontSize() { return fontSize; }
    public void setFontSize(float fontSize) { this.fontSize = fontSize; }
}