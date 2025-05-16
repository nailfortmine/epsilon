package org.kurva.werlii.client.util.font;

import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static FontManager instance;

    private final Map<String, FontRenderer> fonts = new HashMap<>();
    private FontRenderer defaultFont;

    private FontManager() {
        // Don't initialize fonts immediately
        // We'll do it lazily when they're first requested
        defaultFont = null;
    }

    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }

    public FontRenderer getFont(String name) {
        // Lazy initialization of fonts
        if (fonts.isEmpty()) {
            initializeFonts();
        }

        return fonts.getOrDefault(name.toLowerCase(), defaultFont);
    }

    public FontRenderer getDefaultFont() {
        // Lazy initialization of fonts
        if (fonts.isEmpty()) {
            initializeFonts();
        }

        return defaultFont;
    }

    public void registerFont(String name, String fontPath, float size, boolean antiAlias) {
        fonts.put(name.toLowerCase(), new FontRenderer(fontPath, size, antiAlias));
    }

    public void setDefaultFont(String name) {
        FontRenderer font = getFont(name);
        if (font != null) {
            defaultFont = font;
        }
    }

    public void setFontSize(String name, float size) {
        FontRenderer font = getFont(name);
        if (font != null) {
            font.setSize(size);
        }
    }

    public void setAntiAlias(String name, boolean antiAlias) {
        FontRenderer font = getFont(name);
        if (font != null) {
            font.setAntiAlias(antiAlias);
        }
    }

    private void initializeFonts() {
        try {
            // Initialize default fonts with the actual filenames you have
            registerFont("montserrat", "Montserrat-Regular.ttf", 18, true);
            registerFont("montserrat-bold", "Montserrat-Bold.ttf", 18, true);
            registerFont("montserrat-light", "Montserrat-Light.ttf", 18, true);

            // Set default font
            defaultFont = fonts.get("montserrat");

            // Fallback if Montserrat failed to load
            if (defaultFont == null) {
                registerFont("default", "Arial", 18, true);
                defaultFont = fonts.get("default");
            }
        } catch (Exception e) {
            // Log the error but don't crash
            System.err.println("Error initializing fonts: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

