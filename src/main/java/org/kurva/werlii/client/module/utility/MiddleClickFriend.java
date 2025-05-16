package org.kurva.werlii.client.module.utility;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.setting.BooleanSetting;
import org.kurva.werlii.client.util.FriendManager;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class MiddleClickFriend extends Module {
    private final BooleanSetting notificationsS;
    private final BooleanSetting chatCommandS;
    private final BooleanSetting preferMouseS;
    
    private boolean wasMiddleClickDown = false;
    
    public MiddleClickFriend() {
        super("MiddleClickFriend", "Add/remove friends by middle-clicking them", Category.UTILITY);
        this.setKeyCode(GLFW.GLFW_KEY_UNKNOWN); // No keybind, activated by mouse
        this.registerKeybinding("Werlii Utility");
        
        // Add settings
        notificationsS = new BooleanSetting("Notifications", "Show notifications when adding/removing friends", this, true);
        chatCommandS = new BooleanSetting("Chat Commands", "Allow using chat commands to manage friends", this, true);
        preferMouseS = new BooleanSetting("Prefer Mouse", "Prefer using mouse for friend management", this, true);
        
        addSetting(notificationsS);
        addSetting(chatCommandS);
        addSetting(preferMouseS);
        
        // Enable by default
        this.setEnabled(true);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        boolean middleClick = preferMouseS.getValue() ? 
                isMiddleClickPressed() : 
                MinecraftClient.getInstance().options.pickItemKey.isPressed();
        
        // Check if middle mouse was just pressed
        if (middleClick && !wasMiddleClickDown) {
            // Check what the player is looking at
            if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                EntityHitResult hitResult = (EntityHitResult) mc.crosshairTarget;
                
                // Check if it's a player
                if (hitResult.getEntity() instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) hitResult.getEntity();
                    String playerName = player.getName().getString();
                    
                    // Toggle friend status
                    if (FriendManager.isFriend(playerName)) {
                        FriendManager.removeFriend(playerName);
                        if (notificationsS.getValue()) {
                            mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Removed §c" + playerName + " §7from friends"), false);
                        }
                    } else {
                        FriendManager.addFriend(playerName);
                        if (notificationsS.getValue()) {
                            mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Added §a" + playerName + " §7to friends"), false);
                        }
                    }
                }
            }
        }
        
        // Update middle click state
        wasMiddleClickDown = middleClick;
    }
    
    private boolean isMiddleClickPressed() {
        return GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
    }
    
    /**
     * Handle friend chat commands
     * This would be called from a mixin or event handler for chat messages
     */
    public boolean handleChatCommand(String message) {
        if (!isEnabled() || !chatCommandS.getValue()) return false;
        
        // Skip if not a command
        if (!message.startsWith(".friend") && !message.startsWith(".f")) return false;
        
        String[] parts = message.split(" ");
        if (parts.length < 2) {
            sendHelpMessage();
            return true;
        }
        
        String command = parts[1].toLowerCase();
        
        switch (command) {
            case "add":
                if (parts.length < 3) {
                    mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cUsage: .friend add <name>"), false);
                    return true;
                }
                FriendManager.addFriend(parts[2]);
                mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Added §a" + parts[2] + " §7to friends"), false);
                break;
                
            case "remove":
            case "del":
            case "delete":
                if (parts.length < 3) {
                    mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §cUsage: .friend remove <name>"), false);
                    return true;
                }
                FriendManager.removeFriend(parts[2]);
                mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Removed §c" + parts[2] + " §7from friends"), false);
                break;
                
            case "list":
                List<String> friends = FriendManager.getFriends();
                if (friends.isEmpty()) {
                    mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7You have no friends"), false);
                } else {
                    mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Friends (" + friends.size() + "): §a" + String.join("§7, §a", friends)), false);
                }
                break;
                
            case "clear":
                int count = FriendManager.getFriends().size();
                FriendManager.clearFriends();
                mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Cleared " + count + " friends"), false);
                break;
                
            default:
                sendHelpMessage();
                break;
        }
        
        return true;
    }
    
    private void sendHelpMessage() {
        mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Friend Commands:"), false);
        mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7.friend add <name> - Add a friend"), false);
        mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7.friend remove <name> - Remove a friend"), false);
        mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7.friend list - List all friends"), false);
        mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7.friend clear - Clear all friends"), false);
    }
}

