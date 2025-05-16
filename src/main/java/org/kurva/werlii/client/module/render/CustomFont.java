package org.kurva.werlii.client.module.render;

import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.setting.ModeSetting;
import org.kurva.werlii.client.setting.NumberSetting;
import org.kurva.werlii.client.util.font.FontManager;
import org.lwjgl.glfw.GLFW;
import org.kurva.werlii.client.WerliiClient;

public class CustomFont extends Module {
    private final ModeSetting fontTypeS;
    private final NumberSetting fontSizeS;
    private final BooleanSetting antiAliasS;
    private final BooleanSetting shadowS;
    private final BooleanSetting minecraftFontS;

    // Move these to instance variables instead of static
    private String lastFontType = "";
    private float lastFontSize = 0;
    private boolean lastAntiAlias = true;

    public CustomFont() {
        super("CustomFont", "Use custom fonts for the client", Category.RENDER);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
        this.registerKeybinding("Werlii Render");

        // Add settings
        fontTypeS = new ModeSetting("Font", "Font to use", this, "Montserrat",
                "Montserrat", "Montserrat Bold", "Montserrat Light", "Minecraft");
        fontSizeS = new NumberSetting("Size", "Font size", this, 18.0, 12.0, 24.0, 1.0);
        antiAliasS = new BooleanSetting("Anti-Alias", "Smooth font rendering", this, true);
        shadowS = new BooleanSetting("Shadow", "Draw text with shadow", this, true);
        minecraftFontS = new BooleanSetting("Minecraft Font for Chat", "Use Minecraft font for chat", this, true);

        addSetting(fontTypeS);
        addSetting(fontSizeS);
        addSetting(antiAliasS);
        addSetting(shadowS);
        addSetting(minecraftFontS);

        // Enable by default
        this.setEnabled(true);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        // Don't call updateFont() immediately - let it happen during the next tick
    }

    @Override
    public void onTick() {
        // Only update font if settings have changed
        // This reduces unnecessary texture regeneration
        if (!lastFontType.equals(fontTypeS.getValue()) ||
                lastFontSize != fontSizeS.getValue().floatValue() || // Convert Double to float
                lastAntiAlias != antiAliasS.getValue()) {

            updateFont();

            lastFontType = fontTypeS.getValue();
            lastFontSize = fontSizeS.getValue().floatValue(); // Convert Double to float
            lastAntiAlias = antiAliasS.getValue();
        }
    }

    private void updateFont() {
        try {
            String fontName;

            switch (fontTypeS.getValue()) {
                case "Montserrat Bold":
                    fontName = "montserrat-bold";
                    break;
                case "Montserrat Light":
                    fontName = "montserrat-light";
                    break;
                case "Minecraft":
                    fontName = "minecraft";
                    break;
                default:
                    fontName = "montserrat";
                    break;
            }

            // Set as default font
            if (!fontName.equals("minecraft")) {
                FontManager.getInstance().setDefaultFont(fontName);

                // Update font size and anti-aliasing
                FontManager.getInstance().setFontSize(fontName, fontSizeS.getValue().floatValue()); // Convert Double to float
                FontManager.getInstance().setAntiAlias(fontName, antiAliasS.getValue());

                // Update HUD font if needed
                HUD hud = (HUD) WerliiClient.getInstance().getModuleManager().getModuleByName("HUD");
                if (hud != null) {
                    hud.setFontName(fontName);
                    hud.setFontSize(fontSizeS.getValue().floatValue()); // Convert Double to float
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating font: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean shouldUseCustomFont() {
        return isEnabled() && !fontTypeS.getValue().equals("Minecraft");
    }

    public boolean shouldUseMinecraftFontForChat() {
        return !isEnabled() || minecraftFontS.getValue();
    }

    public boolean shouldDrawShadow() {
        return shadowS.getValue();
    }
}

