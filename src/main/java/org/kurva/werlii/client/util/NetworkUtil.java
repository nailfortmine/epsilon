package org.kurva.werlii.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.kurva.werlii.Werlii;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class NetworkUtil {
    
    public static boolean isServerReachable(String address, int timeout) {
        String[] parts = address.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;
        
        try {
            Socket socket = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(host, port);
            
            try {
                socket.connect(socketAddress, timeout);
                return true;
            } catch (IOException e) {
                Werlii.LOGGER.error("Failed to connect to server: " + e.getMessage());
                return false;
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    Werlii.LOGGER.error("Failed to close socket: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Werlii.LOGGER.error("Error connecting to server: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean isConnectedToServer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.getNetworkHandler() != null && mc.getCurrentServerEntry() != null;
    }
    
    public static String getCurrentServerAddress() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getCurrentServerEntry() != null) {
            return mc.getCurrentServerEntry().address;
        }
        return null;
    }
    
    public static void showConnectionInfo() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        
        if (isConnectedToServer()) {
            ServerInfo serverInfo = mc.getCurrentServerEntry();
            String address = serverInfo.address;
            int ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null ? 
                      mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency() : 0;
            
            mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Connected to: §f" + address + " §7(Ping: §f" + ping + "ms§7)"), false);
        } else {
            mc.player.sendMessage(Text.literal("§8[§bWerlii§8] §7Not connected to a server."), false);
        }
    }
    
    public static boolean isVanillaServer(String address) {
        return false;
    }
}

