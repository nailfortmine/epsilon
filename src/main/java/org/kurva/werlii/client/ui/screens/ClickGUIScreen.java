package org.kurva.werlii.client.ui.screens;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.kurva.werlii.client.WerliiClient;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.*;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGUIScreen extends Screen {
  private final Map<Module.Category, CategoryPanel> panels = new HashMap<>();
  private CategoryPanel draggingPanel = null;
  private int dragX, dragY;
  private Module selectedModule = null;
  private int settingsX, settingsY, settingsWidth, settingsHeight;
  private boolean draggingSettings = false;
  private int settingsDragX, settingsDragY;
  private List<SettingComponent> settingComponents = new ArrayList<>();
  private boolean waitingForKeyPress = false;
  private int settingsScrollOffset = 0;
  private final int maxSettingsHeight = 300;
  private int totalSettingsHeight = 0;
  
  public ClickGUIScreen(Text title) {
      super(title);
      
      // Create panels for each category
      int x = 10;
      for (Module.Category category : Module.Category.values()) {
          CategoryPanel panel = new CategoryPanel(category, x, 10);
          panels.put(category, panel);
          x += 120;
      }
      
      // Initialize settings panel
      settingsX = 400;
      settingsY = 50;
      settingsWidth = 200;
      settingsHeight = 300;
  }
  
  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      // First, render a semi-transparent dark overlay
      context.fill(0, 0, this.width, this.height, 0x80000000);
      
      // Render panels
      for (CategoryPanel panel : panels.values()) {
          panel.render(context, mouseX, mouseY);
      }
      
      // Render settings panel if a module is selected
      if (selectedModule != null) {
          renderSettingsPanel(context, mouseX, mouseY);
      }
      
      // Don't call super.render() as it would add additional blur
  }
  
  private void renderSettingsPanel(DrawContext context, int mouseX, int mouseY) {
      // Draw settings panel background
      context.fill(settingsX, settingsY, settingsX + settingsWidth, settingsY + settingsHeight, 0xE0000000);
      context.drawBorder(settingsX, settingsY, settingsWidth, settingsHeight, 0xFFAAAAAA);
      
      // Draw header
      context.fill(settingsX, settingsY, settingsX + settingsWidth, settingsY + 20, 0xFF3366FF);
      context.drawTextWithShadow(client.textRenderer, selectedModule.getName() + " Settings", settingsX + 5, settingsY + 6, 0xFFFFFF);
      
      // Draw close button
      context.fill(settingsX + settingsWidth - 15, settingsY + 5, settingsX + settingsWidth - 5, settingsY + 15, 0xFFFF0000);
      context.drawTextWithShadow(client.textRenderer, "×", settingsX + settingsWidth - 12, settingsY + 6, 0xFFFFFFFF);
      
      // Setup scissor to clip settings that are outside the panel
      int scissorY = settingsY + 25;
      int scissorHeight = settingsHeight - 30;
      
      // Enable scissor test
      context.enableScissor(settingsX, scissorY, settingsX + settingsWidth, scissorY + scissorHeight);
      
      // Render settings
      int y = settingsY + 25;
      for (SettingComponent component : settingComponents) {
          // Store the actual position for each component (without scroll offset)
          component.setPosition(settingsX + 5, y);
          // When rendering, apply the scroll offset to visually position components
          component.render(context, mouseX, mouseY, settingsScrollOffset);
          y += component.getHeight() + 5;
      }
      
      // Store the total content height for scroll bounds checking
      totalSettingsHeight = y - (settingsY + 25);
      
      // Disable scissor test
      context.disableScissor();
      
      // Draw scroll indicators if needed
      if (totalSettingsHeight > scissorHeight) {
          // Draw up arrow
          if (settingsScrollOffset > 0) {
              context.drawTextWithShadow(client.textRenderer, "▲", settingsX + settingsWidth - 15, settingsY + 25, 0xFFFFFFFF);
          }
          
          // Draw down arrow
          if (settingsScrollOffset < totalSettingsHeight - scissorHeight) {
              context.drawTextWithShadow(client.textRenderer, "▼", settingsX + settingsWidth - 15, settingsY + settingsHeight - 15, 0xFFFFFFFF);
          }
      }
  }
  
  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
      // Check if clicked on settings panel close button
      if (selectedModule != null && 
          mouseX >= settingsX + settingsWidth - 15 && mouseX <= settingsX + settingsWidth - 5 &&
          mouseY >= settingsY + 5 && mouseY <= settingsY + 15) {
          selectedModule = null;
          settingComponents.clear();
          return true;
      }
      
      // Check if clicked on settings panel header (for dragging)
      if (selectedModule != null && 
          mouseX >= settingsX && mouseX <= settingsX + settingsWidth &&
          mouseY >= settingsY && mouseY <= settingsY + 20) {
          draggingSettings = true;
          settingsDragX = (int) mouseX - settingsX;
          settingsDragY = (int) mouseY - settingsY;
          return true;
      }
      
      // Check if clicked on a setting component
      if (selectedModule != null) {
          int scissorY = settingsY + 25;
          int scissorHeight = settingsHeight - 30;
          
          if (mouseX >= settingsX && mouseX <= settingsX + settingsWidth &&
              mouseY >= scissorY && mouseY <= scissorY + scissorHeight) {
              for (SettingComponent component : settingComponents) {
                  // Fix: Adjust component position by scroll offset when checking clicks
                  if (mouseY >= component.y - settingsScrollOffset && 
                      mouseY <= component.y - settingsScrollOffset + component.getHeight() && 
                      mouseX >= component.x && mouseX <= component.x + component.width) {
                      if (component.mouseClicked((int) mouseX, (int) mouseY, button)) {
                          return true;
                      }
                  }
              }
          }
      }
      
      // Check if clicked on panel header
      for (CategoryPanel panel : panels.values()) {
          if (panel.isHeaderHovered((int) mouseX, (int) mouseY)) {
              draggingPanel = panel;
              dragX = (int) mouseX - panel.getX();
              dragY = (int) mouseY - panel.getY();
              return true;
          }
          
          // Check if clicked on module
          Module clickedModule = panel.getClickedModule((int) mouseX, (int) mouseY);
          if (clickedModule != null) {
              if (button == 0) { // Left click
                  clickedModule.toggle();
              } else if (button == 1) { // Right click
                  selectedModule = clickedModule;
                  createSettingComponents();
              } else if (button == 2) { // Middle click (mouse wheel)
                  // Start listening for key press to bind
                  KeybindSetting keybindSetting = clickedModule.getKeybindSetting();
                  if (keybindSetting != null) {
                      keybindSetting.setListening(true);
                      waitingForKeyPress = true;
                      if (client.player != null) {
                          client.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Press a key to bind §b" + clickedModule.getName()), false);
                      }
                  }
              }
              return true;
          }
      }
      
      return super.mouseClicked(mouseX, mouseY, button);
  }
  
  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (selectedModule != null) {
          int scissorY = settingsY + 25;
          int scissorHeight = settingsHeight - 30;
          
          if (mouseX >= settingsX && mouseX <= settingsX + settingsWidth &&
              mouseY >= scissorY && mouseY <= scissorY + scissorHeight) {
            
            // Adjust scroll offset
            settingsScrollOffset -= verticalAmount * 15;
            
            // Clamp scroll offset
            settingsScrollOffset = Math.max(0, Math.min(totalSettingsHeight - scissorHeight, settingsScrollOffset));
            
            return true;
        }
    }
    
    return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
  }
  
  private void createSettingComponents() {
      settingComponents.clear();
      settingsScrollOffset = 0;
      
      for (Setting<?> setting : selectedModule.getSettings()) {
          if (setting instanceof BooleanSetting) {
              settingComponents.add(new BooleanSettingComponent((BooleanSetting) setting));
          } else if (setting instanceof NumberSetting) {
              settingComponents.add(new NumberSettingComponent((NumberSetting) setting));
          } else if (setting instanceof ModeSetting) {
              settingComponents.add(new ModeSettingComponent((ModeSetting) setting));
          } else if (setting instanceof KeybindSetting) {
              settingComponents.add(new KeybindSettingComponent((KeybindSetting) setting));
          }
      }
  }
  
  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
      draggingPanel = null;
      draggingSettings = false;
      return super.mouseReleased(mouseX, mouseY, button);
  }
  
  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (draggingPanel != null) {
          draggingPanel.setPosition((int) mouseX - dragX, (int) mouseY - dragY);
          return true;
      }
      
      if (draggingSettings) {
          settingsX = (int) mouseX - settingsDragX;
          settingsY = (int) mouseY - settingsDragY;
          return true;
      }
      
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
  }
  
  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (waitingForKeyPress) {
          for (SettingComponent component : settingComponents) {
              if (component instanceof KeybindSettingComponent) {
                  KeybindSettingComponent keybindComponent = (KeybindSettingComponent) component;
                  if (keybindComponent.isListening()) {
                      keybindComponent.setKey(keyCode);
                      waitingForKeyPress = false;
                      return true;
                  }
              }
          }
      }
      
      if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
          if (selectedModule != null) {
              selectedModule = null;
              settingComponents.clear();
              return true;
          }
      }
      
      return super.keyPressed(keyCode, scanCode, modifiers);
  }
  
  @Override
  public boolean shouldPause() {
      return false;
  }
  
  // Update the abstract SettingComponent class to handle scrolling properly
  // Add a new render method that takes scrollOffset as a parameter
  private abstract class SettingComponent {
      protected final Setting<?> setting;
      protected int x, y;
      protected int width = 190;
      protected int height = 20;
      
      public SettingComponent(Setting<?> setting) {
          this.setting = setting;
      }
      
      // The original render method is kept for backward compatibility
      public abstract void render(DrawContext context, int mouseX, int mouseY);
      
      // Add a new render method that takes scrollOffset
      public void render(DrawContext context, int mouseX, int mouseY, int scrollOffset) {
          // By default, apply the scroll offset to y position when rendering
          int adjustedY = y - scrollOffset;
          // Move rendering logic to this temporary method
          renderAtPosition(context, mouseX, mouseY, x, adjustedY);
      }
      
      // Add a new method to handle actual rendering at a given position
      protected abstract void renderAtPosition(DrawContext context, int mouseX, int mouseY, int renderX, int renderY);
      
      public abstract boolean mouseClicked(int mouseX, int mouseY, int button);
      
      public void setPosition(int x, int y) {
          this.x = x;
          this.y = y;
      }
      
      public int getHeight() {
          return height;
      }
      
      protected boolean isHovered(int mouseX, int mouseY) {
          return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
      }
      
      public boolean isVisible(int scrollOffset) {
          int adjustedY = y - scrollOffset;
          return adjustedY + height > settingsY + 25 && adjustedY < settingsY + settingsHeight;
      }
  }
  
  // Now update the BooleanSettingComponent to use the new render method
  private class BooleanSettingComponent extends SettingComponent {
      private final BooleanSetting booleanSetting;
      
      public BooleanSettingComponent(BooleanSetting setting) {
          super(setting);
          this.booleanSetting = setting;
      }
      
      @Override
      public void render(DrawContext context, int mouseX, int mouseY) {
          // Call the new render method with 0 scroll offset for backward compatibility
          render(context, mouseX, mouseY, 0);
      }
      
      @Override
      protected void renderAtPosition(DrawContext context, int mouseX, int mouseY, int renderX, int renderY) {
          // Use renderX and renderY instead of x and y
          context.drawTextWithShadow(client.textRenderer, setting.getName(), renderX, renderY + 6, 0xFFFFFF);
          
          // Draw toggle button
          int toggleX = renderX + width - 30;
          int toggleY = renderY + 2;
          int toggleWidth = 25;
          int toggleHeight = 15;
          
          int bgColor = booleanSetting.getValue() ? 0xFF00AA00 : 0xFF555555;
          if (mouseX >= toggleX && mouseX <= toggleX + toggleWidth && mouseY >= toggleY && mouseY <= toggleY + toggleHeight) {
              bgColor = booleanSetting.getValue() ? 0xFF00FF00 : 0xFF777777;
          }
          
          context.fill(toggleX, toggleY, toggleX + toggleWidth, toggleY + toggleHeight, bgColor);
          context.drawTextWithShadow(client.textRenderer, booleanSetting.getValue() ? "ON" : "OFF", 
              toggleX + (booleanSetting.getValue() ? 5 : 3), toggleY + 4, 0xFFFFFF);
      }
      
      @Override
      public boolean mouseClicked(int mouseX, int mouseY, int button) {
          // Apply scroll offset to check
          int effectiveY = y - settingsScrollOffset;
          int toggleX = x + width - 30;
          int toggleY = effectiveY + 2;
          int toggleWidth = 25;
          int toggleHeight = 15;
          
          if (mouseX >= toggleX && mouseX <= toggleX + toggleWidth && 
              mouseY >= toggleY && mouseY <= toggleY + toggleHeight) {
              booleanSetting.toggle();
              return true;
          }
          
          return false;
      }
  }
  
  private class NumberSettingComponent extends SettingComponent {
    private final NumberSetting numberSetting;
    
    public NumberSettingComponent(NumberSetting setting) {
        super(setting);
        this.numberSetting = setting;
        this.height = 30;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        render(context, mouseX, mouseY, 0);
    }
    
    @Override
    protected void renderAtPosition(DrawContext context, int mouseX, int mouseY, int renderX, int renderY) {
        // Draw setting name
        context.drawTextWithShadow(client.textRenderer, setting.getName(), renderX, renderY, 0xFFFFFF);
        
        // Draw value
        String value = String.format("%.2f", numberSetting.getValue());
        context.drawTextWithShadow(client.textRenderer, value, renderX + width - client.textRenderer.getWidth(value), renderY, 0xFFFFFF);
        
        // Draw slider
        int sliderX = renderX;
        int sliderY = renderY + 15;
        int sliderWidth = width;
        int sliderHeight = 8;
        
        // Draw slider background
        context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 0xFF555555);
        
        // Draw slider value
        double percentage = (numberSetting.getValue() - numberSetting.getMinValue()) / 
                           (numberSetting.getMaxValue() - numberSetting.getMinValue());
        int valueWidth = (int) (sliderWidth * percentage);
        context.fill(sliderX, sliderY, sliderX + valueWidth, sliderY + sliderHeight, 0xFF3366FF);
        
        // Draw slider knob
        context.fill(sliderX + valueWidth - 2, sliderY - 2, sliderX + valueWidth + 2, sliderY + sliderHeight + 2, 0xFFFFFFFF);
    }
    
    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        int effectiveY = y - settingsScrollOffset;
        int sliderX = x;
        int sliderY = effectiveY + 15;
        int sliderWidth = width;
        int sliderHeight = 8;
        
        if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth && 
            mouseY >= sliderY - 2 && mouseY <= sliderY + sliderHeight + 2) {
            updateSlider(mouseX);
            return true;
        }
        
        return false;
    }
    
    private void updateSlider(int mouseX) {
        double percentage = (double) (mouseX - x) / width;
        double value = numberSetting.getMinValue() + (numberSetting.getMaxValue() - numberSetting.getMinValue()) * percentage;
        numberSetting.setValue(value);
    }
}
  
  private class ModeSettingComponent extends SettingComponent {
    private final ModeSetting modeSetting;
    
    public ModeSettingComponent(ModeSetting setting) {
        super(setting);
        this.modeSetting = setting;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        render(context, mouseX, mouseY, 0);
    }
    
    @Override
    protected void renderAtPosition(DrawContext context, int mouseX, int mouseY, int renderX, int renderY) {
        // Draw setting name
        context.drawTextWithShadow(client.textRenderer, setting.getName(), renderX, renderY + 6, 0xFFFFFF);
        
        // Draw current mode
        String mode = modeSetting.getValue();
        int modeX = renderX + width - client.textRenderer.getWidth(mode) - 15;
        context.drawTextWithShadow(client.textRenderer, mode, modeX, renderY + 6, 0xFF3366FF);
        
        // Draw cycle button
        int buttonX = renderX + width - 10;
        context.drawTextWithShadow(client.textRenderer, ">", buttonX, renderY + 6, 0xFFFFFF);
    }
    
    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        int effectiveY = y - settingsScrollOffset;
        
        if (mouseX >= x && mouseX <= x + width && 
            mouseY >= effectiveY && mouseY <= effectiveY + height) {
            modeSetting.cycle();
            return true;
        }
        
        return false;
    }
}
  
  private class KeybindSettingComponent extends SettingComponent {
    private final KeybindSetting keybindSetting;
    
    public KeybindSettingComponent(KeybindSetting setting) {
        super(setting);
        this.keybindSetting = setting;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        render(context, mouseX, mouseY, 0);
    }
    
    @Override
    protected void renderAtPosition(DrawContext context, int mouseX, int mouseY, int renderX, int renderY) {
        // Draw setting name
        context.drawTextWithShadow(client.textRenderer, setting.getName(), renderX, renderY + 6, 0xFFFFFF);
        
        // Draw key button
        int buttonX = renderX + width - 70;
        int buttonY = renderY;
        int buttonWidth = 65;
        int buttonHeight = 20;
        
        int bgColor = isListening() ? 0xFF00AA00 : 0xFF555555;
        if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
            bgColor = isListening() ? 0xFF00FF00 : 0xFF777777;
        }
        
        context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, bgColor);
        
        String text = isListening() ? "Press a key..." : KeybindSetting.getKeyName(keybindSetting.getValue());
        int textX = buttonX + (buttonWidth - client.textRenderer.getWidth(text)) / 2;
        context.drawTextWithShadow(client.textRenderer, text, textX, buttonY + 6, 0xFFFFFF);
    }
    
    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        int effectiveY = y - settingsScrollOffset;
        int buttonX = x + width - 70;
        int buttonY = effectiveY;
        int buttonWidth = 65;
        int buttonHeight = 20;
        
        if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth && 
            mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
            keybindSetting.setListening(!keybindSetting.isListening());
            waitingForKeyPress = keybindSetting.isListening();
            return true;
        }
        
        return false;
    }
    
    public boolean isListening() {
        return keybindSetting.isListening();
    }
    
    public void setKey(int keyCode) {
        keybindSetting.setValue(keyCode);
        keybindSetting.setListening(false);
        
        // Update module key code
        selectedModule.setKeyCode(keyCode);
      
      // Show confirmation message
      if (client.player != null) {
          client.player.sendMessage(Text.literal("§8[§bWerlii§8] §aSuccessfully bound §b" + 
              selectedModule.getName() + " §ato §b" + KeybindSetting.getKeyName(keyCode)), false);
      }
    }
}
  
  private class CategoryPanel {
      private final Module.Category category;
      private int x, y;
      private final int width = 100;
      private final int headerHeight = 20;
      private boolean expanded = true;
      private final List<Module> modules;
      
      public CategoryPanel(Module.Category category, int x, int y) {
          this.category = category;
          this.x = x;
          this.y = y;
          
          // Get modules for this category
          this.modules = new ArrayList<>();
          for (Module module : WerliiClient.getInstance().getModuleManager().getModules()) {
              if (module.getCategory() == category) {
                  modules.add(module);
              }
          }
      }
      
      public void render(DrawContext context, int mouseX, int mouseY) {
          // Render header with dark background
          context.fill(x, y, x + width, y + headerHeight, 0xFF000000);
          context.drawTextWithShadow(client.textRenderer, category.getName(), x + 5, y + 6, 0xFFFFFF);
          
          // Render expand/collapse button
          String expandChar = expanded ? "-" : "+";
          context.drawTextWithShadow(client.textRenderer, expandChar, x + width - 10, y + 6, 0xFFFFFF);
          
          // Render modules if expanded
          if (expanded) {
              int moduleY = y + headerHeight;
              for (Module module : modules) {
                  // Background with transparency
                  int bgColor = module.isEnabled() ? 0xAA3366FF : 0xAA222222;
                  if (isModuleHovered(mouseX, mouseY, moduleY)) {
                      bgColor = module.isEnabled() ? 0xAA4477FF : 0xAA333333;
                  }
                  
                  // Fill with semi-transparent background
                  context.fill(x, moduleY, x + width, moduleY + 15, bgColor);
                  
                  // Module name
                  context.drawTextWithShadow(client.textRenderer, module.getName(), x + 5, moduleY + 3, 0xFFFFFF);
                  
                  // Right-click indicator
                  if (isModuleHovered(mouseX, mouseY, moduleY)) {
                      context.drawTextWithShadow(client.textRenderer, "⚙", x + width - 15, moduleY + 3, 0xFFFFFF);
                  }
                  
                  // Render settings if expanded
                  if (module.isSettingsExpanded()) {
                      renderModuleSettings(context, module, moduleY + 15, mouseX, mouseY);
                      moduleY += getExpandedSettingsHeight(module);
                  }
                  
                  moduleY += 15;
              }
          }
      }
      
      private void renderModuleSettings(DrawContext context, Module module, int startY, int mouseX, int mouseY) {
          int settingY = startY;
          
          for (Setting<?> setting : module.getSettings()) {
              // Skip keybind setting as it's handled separately
              if (setting instanceof KeybindSetting) continue;
              
              // Background
              context.fill(x + 5, settingY, x + width - 5, settingY + 20, 0xAA444444);
              
              // Setting name
              context.drawTextWithShadow(client.textRenderer, setting.getName(), x + 10, settingY + 6, 0xFFFFFF);
              
              // Render setting value based on type
              if (setting instanceof BooleanSetting) {
                  BooleanSetting boolSetting = (BooleanSetting) setting;
                  String value = boolSetting.getValue() ? "ON" : "OFF";
                  int color = boolSetting.getValue() ? 0xFF00AA00 : 0xFFAA0000;
                  context.drawTextWithShadow(client.textRenderer, value, x + width - 25, settingY + 6, color);
              } else if (setting instanceof NumberSetting) {
                  NumberSetting numSetting = (NumberSetting) setting;
                  String value = String.format("%.1f", numSetting.getValue());
                  context.drawTextWithShadow(client.textRenderer, value, x + width - 30, settingY + 6, 0xFFFFFF);
              } else if (setting instanceof ModeSetting) {
                  ModeSetting modeSetting = (ModeSetting) setting;
                  context.drawTextWithShadow(client.textRenderer, modeSetting.getValue(), x + width - 40, settingY + 6, 0xFF3366FF);
              }
              
              settingY += 20;
          }
      }
      
      private int getExpandedSettingsHeight(Module module) {
          int settingsCount = 0;
          for (Setting<?> setting : module.getSettings()) {
              if (!(setting instanceof KeybindSetting)) {
                  settingsCount++;
              }
          }
          return settingsCount * 20;
      }
      
      public boolean isHeaderHovered(int mouseX, int mouseY) {
          return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight;
      }
      
      public boolean isModuleHovered(int mouseX, int mouseY, int moduleY) {
          return mouseX >= x && mouseX <= x + width && mouseY >= moduleY && mouseY <= moduleY + 15;
      }
      
      public Module getClickedModule(int mouseX, int mouseY) {
          if (!expanded) return null;
          
          int moduleY = y + headerHeight;
          for (Module module : modules) {
              if (isModuleHovered(mouseX, mouseY, moduleY)) {
                  return module;
              }
              
              if (module.isSettingsExpanded()) {
                  moduleY += getExpandedSettingsHeight(module);
              }
              
              moduleY += 15;
          }
          
          return null;
      }
      
      public void setPosition(int x, int y) {
          this.x = x;
          this.y = y;
      }
      
      public int getX() {
          return x;
      }
      
      public int getY() {
          return y;
      }
  }
}

