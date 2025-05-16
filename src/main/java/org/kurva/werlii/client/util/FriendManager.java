package org.kurva.werlii.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FriendManager {
    private static final Set<String> friends = new HashSet<>();
    private static final File friendsFile = new File(MinecraftClient.getInstance().runDirectory, "werlii/friends.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    static {
        loadFriends();
    }
    
    /**
     * Check if a player is a friend
     */
    public static boolean isFriend(String name) {
        return friends.contains(name.toLowerCase());
    }
    
    /**
     * Add a player to friends
     */
    public static void addFriend(String name) {
        friends.add(name.toLowerCase());
        saveFriends();
    }
    
    /**
     * Remove a player from friends
     */
    public static void removeFriend(String name) {
        friends.remove(name.toLowerCase());
        saveFriends();
    }
    
    /**
     * Get all friends
     */
    public static List<String> getFriends() {
        return new ArrayList<>(friends);
    }
    
    /**
     * Clear all friends
     */
    public static void clearFriends() {
        friends.clear();
        saveFriends();
    }
    
    /**
     * Load friends from disk
     */
    public static void loadFriends() {
        friends.clear();
        
        try {
            if (!friendsFile.exists()) {
                File parent = friendsFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                saveFriends(); // Create empty file
                return;
            }
            
            try (FileReader reader = new FileReader(friendsFile)) {
                List<String> loadedFriends = gson.fromJson(reader, new TypeToken<List<String>>(){}.getType());
                if (loadedFriends != null) {
                    friends.addAll(loadedFriends);
                }
            }
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            System.err.println("Failed to load friends: " + e.getMessage());
        }
    }
    
    /**
     * Save friends to disk
     */
    public static void saveFriends() {
        try {
            File parent = friendsFile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            
            try (FileWriter writer = new FileWriter(friendsFile)) {
                gson.toJson(new ArrayList<>(friends), writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save friends: " + e.getMessage());
        }
    }
}

