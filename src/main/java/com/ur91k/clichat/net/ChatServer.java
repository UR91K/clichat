package com.ur91k.clichat.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ur91k.clichat.util.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.joml.Vector4f;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer extends WebSocketServer {
    private static final Logger logger = Logger.getLogger(ChatServer.class);
    private final Map<WebSocket, ClientInfo> clients = new ConcurrentHashMap<>();
    private final Gson gson;
    private String roomName = "main_room";
    private Vector4f roomColor = new Vector4f(0.6f, 0.8f, 1.0f, 1.0f);
    
    private static class ClientInfo {
        String id;
        String name;
        Vector4f color;
        boolean isOp;
        
        ClientInfo(String id, String name, Vector4f color) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.isOp = false;
        }
    }
    
    public ChatServer(String ip, int port) {
        super(new InetSocketAddress(ip, port));
        // Configure Gson with our custom adapters
        this.gson = new GsonBuilder()
            .registerTypeAdapter(Vector4f.class, new Vector4fAdapter())
            .registerTypeAdapter(Message.class, new MessageAdapter())
            .create();
        logger.info("Server created on {}:{}", ip, port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String id = conn.getRemoteSocketAddress().toString();
        clients.put(conn, new ClientInfo(id, "", new Vector4f(1.0f)));
        logger.info("New connection from: {}", id);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ClientInfo client = clients.remove(conn);
        if (client != null && !client.name.isEmpty()) {
            broadcast(gson.toJson(Message.leave(client.id, client.name, client.color)));
        }
        logger.info("Connection closed: {} ({})", client != null ? client.name : "unknown", conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String messageJson) {
        try {
            Message message = gson.fromJson(messageJson, Message.class);
            ClientInfo client = clients.get(conn);
            
            if (client == null) {
                logger.error("Message from unknown client: {}", conn.getRemoteSocketAddress());
                return;
            }
            
            switch (message.getType()) {
                case JOIN:
                    handleJoinMessage(conn, client, message);
                    break;
                case CHAT:
                    handleChatMessage(conn, client, message);
                    break;
                case NICK_CHANGE:
                    handleNickChange(conn, client, message);
                    break;
                case COLOR_CHANGE:
                    handleColorChange(conn, client, message);
                    break;
                case COMMAND:
                    handleCommand(conn, client, message);
                    break;
                default:
                    logger.warn("Unhandled message type: {}", message.getType());
            }
        } catch (Exception e) {
            logger.error("Error handling message: {}", e.getMessage());
        }
    }
    
    private void handleJoinMessage(WebSocket conn, ClientInfo client, Message message) {
        // Update client info
        client.name = message.getSenderName();
        client.color = message.getSenderColor();
        
        // Send room info
        conn.send(gson.toJson(Message.roomUpdate(roomName, roomColor)));
        
        // Broadcast join message
        broadcast(gson.toJson(Message.join(client.id, client.name, client.color)));
        
        logger.info("Client joined: {} ({})", client.name, client.id);
    }
    
    private void handleChatMessage(WebSocket conn, ClientInfo client, Message message) {
        if (!client.name.equals(message.getSenderName())) {
            logger.warn("Message sender mismatch: {} != {}", client.name, message.getSenderName());
            return;
        }
        broadcast(gson.toJson(message));
        logger.debug("Message from {}: {}", client.name, message.getContent());
    }
    
    private void handleNickChange(WebSocket conn, ClientInfo client, Message message) {
        String oldName = client.name;
        client.name = message.getNewValue();
        broadcast(gson.toJson(Message.nickChange(client.id, oldName, client.name, client.color)));
        logger.info("Nickname change: {} -> {}", oldName, client.name);
    }
    
    private void handleColorChange(WebSocket conn, ClientInfo client, Message message) {
        Vector4f oldColor = new Vector4f(client.color);
        client.color = message.getSenderColor();
        broadcast(gson.toJson(Message.colorChange(client.id, client.name, oldColor, client.color)));
        logger.info("Color change for {}", client.name);
    }
    
    private void handleCommand(WebSocket conn, ClientInfo client, Message message) {
        if (!client.isOp) {
            conn.send(gson.toJson(Message.system("You don't have permission to use this command")));
            return;
        }
        
        String[] parts = message.getContent().split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        switch (command) {
            case "/kick":
                handleKickCommand(client, args);
                break;
            case "/ban":
                handleBanCommand(client, args);
                break;
            case "/op":
                handleOpCommand(client, args);
                break;
            default:
                conn.send(gson.toJson(Message.system("Unknown command: " + command)));
        }
    }
    
    private void handleKickCommand(ClientInfo sender, String targetName) {
        for (Map.Entry<WebSocket, ClientInfo> entry : clients.entrySet()) {
            if (entry.getValue().name.equals(targetName)) {
                entry.getKey().close();
                broadcast(gson.toJson(Message.system(targetName + " was kicked by " + sender.name)));
                return;
            }
        }
        // Target not found
        broadcast(gson.toJson(Message.system("User not found: " + targetName)));
    }
    
    private void handleBanCommand(ClientInfo sender, String targetName) {
        // TODO: Implement ban system
        broadcast(gson.toJson(Message.system("Ban system not implemented yet")));
    }
    
    private void handleOpCommand(ClientInfo sender, String targetName) {
        for (ClientInfo client : clients.values()) {
            if (client.name.equals(targetName)) {
                client.isOp = true;
                broadcast(gson.toJson(Message.system(targetName + " is now an operator")));
                return;
            }
        }
        // Target not found
        broadcast(gson.toJson(Message.system("User not found: " + targetName)));
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            clients.remove(conn);
        }
        logger.error("Server error: {}", ex.getMessage());
    }

    @Override
    public void onStart() {
        logger.info("Server started successfully");
    }
} 