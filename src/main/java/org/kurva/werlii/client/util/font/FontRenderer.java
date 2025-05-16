package org.kurva.werlii.client.util.font;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.io.ByteArrayInputStream;

public class FontRenderer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§[0-9a-fk-or]");

    private Font font;
    private boolean antiAlias;
    private float size;
    private final Map<Character, CharInfo> charMap = new HashMap<>();
    private Identifier fontTexture;
    private int fontHeight = -1;
    private CharInfo missingCharInfo;

    public FontRenderer(String fontName, float size, boolean antiAlias) {
        this.size = size;
        this.antiAlias = antiAlias;

        try {
            // Load font with UTF-8 encoding support
            InputStream is = FontRenderer.class.getResourceAsStream("/assets/werlii/fonts/" + fontName);
            if (is != null) {
                // Create temporary buffer to read font data
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();

                font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(buffer.toByteArray())).deriveFont(size);
                is.close();
            } else {
                System.out.println("Could not load font: " + fontName);
                // Fallback to system font
                font = new Font(fontName, Font.PLAIN, (int) size);
            }

            fontTexture = null;
            fontHeight = (int) size;
        } catch (Exception e) {
            e.printStackTrace();
            font = new Font("Arial", Font.PLAIN, (int) size);
            fontTexture = null;
            fontHeight = (int) size;
        }
    }

    // Add setters for size and antiAlias to FontRenderer
    public void setSize(float size) {
        if (this.size != size) {
            this.size = size;
            try {
                // Regenerate font with new size
                if (font != null) {
                    font = font.deriveFont(size);
                    generateFontTexture();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setAntiAlias(boolean antiAlias) {
        if (this.antiAlias != antiAlias) {
            this.antiAlias = antiAlias;
            generateFontTexture();
        }
    }

    private void generateFontTexture() {
        try {
            // Clear previous texture if it exists
            if (fontTexture != null) {
                // Can't directly delete texture by Identifier, need to get the raw ID
                int textureId = ((AbstractTexture)MinecraftClient.getInstance().getTextureManager()
                        .getTexture(fontTexture)).getGlId();
                RenderSystem.deleteTexture(textureId);
                fontTexture = null;
            }

            // Create temporary image for measuring
            BufferedImage tempImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = (Graphics2D) tempImg.getGraphics();
            if (antiAlias) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            }
            g2d.setFont(font);
            FontRenderContext frc = g2d.getFontRenderContext();

            // Calculate texture size needed for all characters (including Unicode)
            int imageSize = 512; // Reduced from 2048 to prevent memory issues
            int rowHeight = 0;
            int posX = 0;
            int posY = 0;

            // Create the actual texture
            BufferedImage fontImage = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
            g2d = (Graphics2D) fontImage.getGraphics();
            g2d.setColor(new Color(0, 0, 0, 0));
            g2d.fillRect(0, 0, imageSize, imageSize);
            g2d.setColor(Color.WHITE);
            g2d.setFont(font);
            if (antiAlias) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            }

            // Generate textures for basic ASCII characters first
            for (int i = 32; i < 128; i++) { // Only basic ASCII range initially
                char c = (char) i;
                String charStr = String.valueOf(c);

                // Skip if font doesn't support this character
                if (!font.canDisplay(c)) continue;

                Rectangle2D bounds = font.getStringBounds(charStr, frc);
                int charWidth = (int) Math.ceil(bounds.getWidth());
                int charHeight = (int) Math.ceil(bounds.getHeight());

                if (charHeight > rowHeight) {
                    rowHeight = charHeight;
                }

                // Check if we need to move to next row
                if (posX + charWidth >= imageSize) {
                    posX = 0;
                    posY += rowHeight;
                    rowHeight = charHeight;

                    // Check if we've run out of texture space
                    if (posY + rowHeight >= imageSize) break;
                }

                g2d.drawString(charStr, posX, posY + g2d.getFontMetrics().getAscent());

                CharInfo info = new CharInfo(
                        posX / (float) imageSize,
                        posY / (float) imageSize,
                        (posX + charWidth) / (float) imageSize,
                        (posY + charHeight) / (float) imageSize,
                        charWidth,
                        charHeight
                );

                charMap.put(c, info);
                if (c == '?' || missingCharInfo == null) {
                    missingCharInfo = info;
                }

                posX += charWidth;
            }

            fontHeight = rowHeight;

            try {
                // Convert to Minecraft texture
                NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, imageSize, imageSize, true);
                for (int x = 0; x < imageSize; x++) {
                    for (int y = 0; y < imageSize; y++) {
                        int rgb = fontImage.getRGB(x, y);
                        int alpha = (rgb >> 24) & 0xFF;
                        nativeImage.setColor(x, y, alpha << 24 | 0xFFFFFF);
                    }
                }

                // Register the texture
                String texturePath = "font/custom_" + System.currentTimeMillis();
                // Use the static of() method instead of constructor
                fontTexture = Identifier.of("werlii", "fonts/custom_" + System.currentTimeMillis());
                MinecraftClient.getInstance().getTextureManager().registerTexture(
                        fontTexture,
                        new NativeImageBackedTexture(nativeImage)
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            g2d.dispose();
        } catch (Exception e) {
            System.err.println("Failed to generate font texture: " + e.getMessage());
            e.printStackTrace();
            fontTexture = null; // Ensure texture is null on failure
        }
    }

    // Add a method to check if texture is generated and generate it if needed
    private void ensureTextureGenerated() {
        if (fontTexture == null) {
            try {
                generateFontTexture();
            } catch (Exception e) {
                System.err.println("Error generating font texture: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void drawString(DrawContext context, String text, float x, float y, int color) {
        drawString(context, text, x, y, color, false);
    }

    // Modify drawString to ensure texture is generated
    public void drawString(DrawContext context, String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;

        // Ensure texture is generated before drawing
        ensureTextureGenerated();

        if (fontTexture == null) {
            // If texture generation failed, fall back to Minecraft's font renderer
            if (shadow) {
                context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, text, (int)x, (int)y, color);
            } else {
                context.drawText(MinecraftClient.getInstance().textRenderer, text, (int)x, (int)y, color, false);
            }
            return;
        }

        if (shadow) {
            drawText(context, text, x + 1, y + 1, 0xFF000000);
        }
        drawText(context, text, x, y, color);
    }

    private void drawText(DrawContext context, String text, float x, float y, int color) {
        if (fontTexture == null) return;

        // Setup rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, fontTexture);

        // Set color
        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        RenderSystem.setShaderColor(red, green, blue, alpha);

        float startX = x;

        // Draw each character
        for (char c : text.toCharArray()) {
            CharInfo charInfo = charMap.getOrDefault(c, missingCharInfo);
            if (charInfo != null) {
                drawChar(context, charInfo, x, y);
                x += charInfo.width;
            }
        }

        // Reset color
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawChar(DrawContext context, CharInfo charInfo, float x, float y) {
        if (fontTexture == null) return;

        float u1 = charInfo.u1;
        float v1 = charInfo.v1;
        float u2 = charInfo.u2;
        float v2 = charInfo.v2;

        // Update the drawTexture call to use the public method
        context.drawTexture(
                fontTexture,
                (int)x, (int)y,
                charInfo.width, charInfo.height,
                (int)(charInfo.u1 * 512), (int)(charInfo.v1 * 512),
                charInfo.width, charInfo.height,
                512, 512
        );
    }

    // Modify getStringWidth to ensure texture is generated
    public int getStringWidth(String text) {
        if (text == null || text.isEmpty()) return 0;

        // Ensure texture is generated before measuring
        ensureTextureGenerated();

        // If texture generation failed, fall back to Minecraft's font renderer
        if (fontTexture == null) {
            return MinecraftClient.getInstance().textRenderer.getWidth(text);
        }

        int width = 0;
        for (char c : text.toCharArray()) {
            CharInfo info = charMap.getOrDefault(c, missingCharInfo);
            if (info != null) {
                width += info.width;
            }
        }
        return width;
    }

    public int getFontHeight() {
        return fontHeight;
    }

    private static class CharInfo {
        public final float u1, v1, u2, v2;
        public final int width, height;

        public CharInfo(float u1, float v1, float u2, float v2, int width, int height) {
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
            this.width = width;
            this.height = height;
        }
    }
}

