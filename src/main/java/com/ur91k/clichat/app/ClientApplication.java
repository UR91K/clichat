package com.ur91k.clichat.app;

import com.ur91k.clichat.render.TextRenderer;
import com.ur91k.clichat.render.Window;
import com.ur91k.clichat.terminal.ChatTerminal;
import com.ur91k.clichat.util.Logger;
import com.ur91k.clichat.net.ChatClient;
import com.ur91k.clichat.net.Message;
import com.ur91k.clichat.config.ServerConfig;
import com.ur91k.clichat.profile.UserProfile;
import com.ur91k.clichat.profile.UserProfileManager;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.joml.Vector4f;

import java.net.URI;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class ClientApplication {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final Random random = new Random();
    
    private Window window;
    private ChatTerminal terminal;
    private TextRenderer textRenderer;
    private ChatClient client;
    private ServerConfig serverConfig;
    private boolean running = true;
    private String username;
    private Vector4f userColor;
    
    private enum State {
        PROFILE_SELECT,
        PROFILE_CREATE,
        PROFILE_LOGIN,
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
    private State currentState = State.PROFILE_SELECT;
    private UserProfileManager profileManager;
    
    public void run() {
        init();
        loop();
        cleanup();
    }
    
    private void init() {
        // Set up logging
        Logger.setGlobalMinimumLevel(Logger.Level.DEBUG);
        Logger.useColors(true);
        
        // Initialize GLFW
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        
        // Create window
        window = new Window("CLIChat Client", WINDOW_WIDTH, WINDOW_HEIGHT);
        window.init();
        
        // Initialize OpenGL
        glfwMakeContextCurrent(window.getHandle());
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        // Create text renderer and terminal
        textRenderer = new TextRenderer(WINDOW_WIDTH, WINDOW_HEIGHT);
        terminal = new ChatTerminal(textRenderer);
        terminal.handleResize(WINDOW_WIDTH, WINDOW_HEIGHT);
        
        // Generate random username and color
        username = "user" + (1000 + random.nextInt(9000));
        userColor = new Vector4f(
            0.3f + random.nextFloat() * 0.7f,
            0.3f + random.nextFloat() * 0.7f,
            0.3f + random.nextFloat() * 0.7f,
            1.0f
        );
        terminal.setUsername(username);
        terminal.setUsernameColor(userColor);
        
        // Load server config
        serverConfig = new ServerConfig();
        serverConfig.load();
        
        // Setup resize callback
        window.setResizeCallback((width, height) -> {
            glViewport(0, 0, width, height);
            terminal.handleResize(width, height);
        });
        
        // Setup input callbacks
        glfwSetCharCallback(window.getHandle(), new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                terminal.handleCharacter((char)codepoint);
            }
        });
        
        glfwSetKeyCallback(window.getHandle(), new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                    if (key == GLFW_KEY_BACKSPACE) {
                        terminal.handleCharacter('\b');
                    } else if (key == GLFW_KEY_ENTER) {
                        handleInput();
                    } else if (key == GLFW_KEY_ESCAPE) {
                        running = false;
                    }
                }
            }
        });
        
        // Initialize profile manager
        profileManager = new UserProfileManager();
        
        // Show profile selection instead of server selection
        showProfileSelection();
    }
    
    private void showProfileSelection() {
        terminal.clearLines();
        terminal.setConnectionInfo("DISCONNECTED", "");
        terminal.setRoomInfo("", null);
        
        terminal.addLine("Welcome to CLIChat!");
        terminal.addLine("");
        
        List<UserProfile> profiles = profileManager.getProfiles();
        if (!profiles.isEmpty()) {
            terminal.addLine("Select a profile:");
            terminal.addLine("-".repeat(40));
            for (int i = 0; i < profiles.size(); i++) {
                terminal.addLine((i + 1) + ". " + profiles.get(i).toString());
            }
            terminal.addLine("-".repeat(40));
            terminal.addLine("");
        }
        
        terminal.addLine("Commands:");
        terminal.addLine("* Enter a number to select a profile");
        terminal.addLine("* /new - Create a new profile");
        terminal.addLine("* /quit - Exit application");
    }
    
    private void showProfileCreate() {
        currentState = State.PROFILE_CREATE;
        terminal.clearLines();
        terminal.addLine("Create New Profile");
        terminal.addLine("-".repeat(40));
        terminal.addLine("Enter username and password in format: username password");
        terminal.addLine("Example: alice mypassword123");
        terminal.addLine("");
        terminal.addLine("/back - Return to profile selection");
    }
    
    private void showProfileLogin(UserProfile profile) {
        currentState = State.PROFILE_LOGIN;
        terminal.clearLines();
        terminal.addLine("Login to Profile: " + profile.getUsername());
        terminal.addLine("-".repeat(40));
        terminal.addLine("Enter password:");
        terminal.addLine("");
        terminal.addLine("/back - Return to profile selection");
    }
    
    private void handleInput() {
        String input = terminal.getCurrentInput().trim();
        if (!input.isEmpty()) {
            switch (currentState) {
                case PROFILE_SELECT -> handleProfileSelect(input);
                case PROFILE_CREATE -> handleProfileCreate(input);
                case PROFILE_LOGIN -> handleProfileLogin(input);
                case DISCONNECTED -> handleDisconnectedInput(input);
                case CONNECTING -> terminal.addLine("* Please wait while connecting...");
                case CONNECTED -> {
                    if (input.startsWith("/")) {
                        handleCommand(input);
                    } else if (client != null && client.isConnected()) {
                        client.sendMessage(input);
                    } else {
                        terminal.addLine("* Not connected to server");
                    }
                }
            }
            terminal.clearInput();
        }
    }
    
    private void handleProfileSelect(String input) {
        if (input.equals("/new")) {
            showProfileCreate();
            return;
        }
        
        if (input.equals("/quit")) {
            running = false;
            return;
        }
        
        try {
            int index = Integer.parseInt(input) - 1;
            List<UserProfile> profiles = profileManager.getProfiles();
            if (index >= 0 && index < profiles.size()) {
                UserProfile selected = profiles.get(index);
                showProfileLogin(selected);
                return;
            }
        } catch (NumberFormatException ignored) {}
        
        terminal.addLine("* Invalid input. Enter a number or /new");
    }
    
    private void handleProfileCreate(String input) {
        if (input.equals("/back")) {
            currentState = State.PROFILE_SELECT;
            showProfileSelection();
            return;
        }
        
        String[] parts = input.split("\\s+", 2);
        if (parts.length != 2) {
            terminal.addLine("* Invalid format. Use: username password");
            return;
        }
        
        String username = parts[0];
        String password = parts[1];
        
        // Generate random color for new profile
        Vector4f color = new Vector4f(
            0.3f + random.nextFloat() * 0.7f,
            0.3f + random.nextFloat() * 0.7f,
            0.3f + random.nextFloat() * 0.7f,
            1.0f
        );
        
        if (profileManager.createProfile(username, password, color)) {
            terminal.addLine("* Profile created successfully!");
            if (profileManager.login(username, password)) {
                loadProfile(profileManager.getCurrentProfile());
                currentState = State.DISCONNECTED;
                showServerSelection();
            }
        } else {
            terminal.addLine("* Username already exists");
        }
    }
    
    private void handleProfileLogin(String input) {
        if (input.equals("/back")) {
            currentState = State.PROFILE_SELECT;
            showProfileSelection();
            return;
        }
        
        List<UserProfile> profiles = profileManager.getProfiles();
        UserProfile selected = profiles.get(0);  // The one we're trying to log into
        
        if (profileManager.login(selected.getUsername(), input)) {
            loadProfile(profileManager.getCurrentProfile());
            currentState = State.DISCONNECTED;
            showServerSelection();
        } else {
            terminal.addLine("* Incorrect password");
        }
    }
    
    private void loadProfile(UserProfile profile) {
        username = profile.getUsername();
        userColor = profile.getColor();
        terminal.setUsername(username);
        terminal.setUsernameColor(userColor);
    }
    
    private void handleDisconnectedInput(String input) {
        if (input.startsWith("/")) {
            handleCommand(input);
            return;
        }
        
        // Try to parse as a number for server selection
        try {
            int index = Integer.parseInt(input) - 1;
            List<ServerConfig.ServerEntry> servers = serverConfig.getRecentServers();
            if (index >= 0 && index < servers.size()) {
                ServerConfig.ServerEntry server = servers.get(index);
                connectToServer(server.getIp(), server.getPort());
                return;
            }
        } catch (NumberFormatException ignored) {}
        
        // Try to parse as IP:PORT
        String[] parts = input.split(":");
        if (parts.length == 2) {
            try {
                int port = Integer.parseInt(parts[1]);
                connectToServer(parts[0], port);
                serverConfig.addServer("Custom Server", parts[0], port);
                return;
            } catch (NumberFormatException ignored) {}
        }
        
        terminal.addLine("* Invalid input. Enter a room number or IP:PORT");
    }
    
    private void handleCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        switch (command) {
            case "/help":
                if (currentState == State.DISCONNECTED) {
                    terminal.addLine("Available commands:");
                    terminal.addLine("/connect <ip:port> - Connect to a server");
                    terminal.addLine("/nick <name> - Change your nickname");
                    terminal.addLine("/color - Change your color randomly");
                    terminal.addLine("/quit - Exit the application");
                } else {
                    terminal.addLine("Available commands:");
                    terminal.addLine("/nick <name> - Change your nickname");
                    terminal.addLine("/color - Change your color randomly");
                    terminal.addLine("/disconnect - Disconnect from server");
                    terminal.addLine("/quit - Exit the application");
                }
                terminal.addLine("/color [hex] - Change your color (e.g. /color #ff0000 for red)");
                break;
                
            case "/nick":
                if (!args.isEmpty()) {
                    username = args;
                    terminal.setUsername(username);
                    if (client != null && client.isConnected()) {
                        client.changeNickname(args);
                    } else {
                        terminal.addLine("* Nickname changed to: " + username);
                        showServerSelection();
                    }
                } else {
                    terminal.addLine("* Usage: /nick <name>");
                }
                break;
                
            case "/color":
                Vector4f newColor;
                if (args.isEmpty()) {
                    // Random color if no argument provided
                    newColor = new Vector4f(
                        0.3f + random.nextFloat() * 0.7f,
                        0.3f + random.nextFloat() * 0.7f,
                        0.3f + random.nextFloat() * 0.7f,
                        1.0f
                    );
                } else {
                    // Parse hex color
                    try {
                        String hex = args.startsWith("#") ? args.substring(1) : args;
                        if (hex.length() != 6) {
                            terminal.addLine("* Invalid hex color. Use format: #RRGGBB");
                            break;
                        }
                        int r = Integer.parseInt(hex.substring(0, 2), 16);
                        int g = Integer.parseInt(hex.substring(2, 4), 16);
                        int b = Integer.parseInt(hex.substring(4, 6), 16);
                        newColor = new Vector4f(r / 255f, g / 255f, b / 255f, 1.0f);
                    } catch (NumberFormatException e) {
                        terminal.addLine("* Invalid hex color. Use format: #RRGGBB");
                        break;
                    }
                }
                userColor = newColor;
                terminal.setUsernameColor(newColor);
                
                // Save to profile
                if (profileManager.getCurrentProfile() != null) {
                    profileManager.getCurrentProfile().setColor(newColor);
                    profileManager.updateCurrentProfile();
                }
                
                // Update client if connected
                if (client != null && client.isConnected()) {
                    client.changeColor(newColor);
                } else {
                    terminal.addLine("* Color changed");
                    showServerSelection();
                }
                break;
                
            case "/connect":
                if (args.isEmpty()) {
                    terminal.addLine("* Usage: /connect <ip:port>");
                    break;
                }
                String[] connectParts = args.split(":");
                if (connectParts.length != 2) {
                    terminal.addLine("* Invalid format. Use: ip:port");
                    break;
                }
                try {
                    int port = Integer.parseInt(connectParts[1]);
                    connectToServer(connectParts[0], port);
                    serverConfig.addServer("Custom Server", connectParts[0], port);
                } catch (NumberFormatException e) {
                    terminal.addLine("* Invalid port number");
                }
                break;
                
            case "/disconnect":
                if (client != null) {
                    client.close();
                    client = null;
                }
                currentState = State.DISCONNECTED;
                showServerSelection();
                break;
                
            case "/quit":
                running = false;
                break;
                
            default:
                if (client != null && client.isConnected()) {
                    client.sendCommand(input);
                } else {
                    terminal.addLine("* Unknown command. Type /help for available commands");
                }
                break;
        }
    }
    
    private void loop() {
        while (running && !window.shouldClose()) {
            glClear(GL_COLOR_BUFFER_BIT);
            terminal.render();
            window.update();
        }
    }
    
    private void cleanup() {
        if (client != null) {
            client.close();
        }
        window.cleanup();
        glfwTerminate();
    }
    
    private void showServerSelection() {
        terminal.clearLines();
        terminal.setConnectionInfo("DISCONNECTED", "");
        terminal.setRoomInfo("", null);
        
        // User preview section
        terminal.addLine("You appear as:");
        terminal.addLine("[00:00:00] " + username + ": Hello, world!", userColor);
        terminal.addLine("");
        
        // Available servers section
        terminal.addLine("Available rooms:");
        terminal.addLine("-".repeat(40));  // Separator line
        List<ServerConfig.ServerEntry> servers = serverConfig.getRecentServers();
        if (servers.isEmpty()) {
            terminal.addLine("No saved rooms. Connect to a server to add it to this list.");
        } else {
            for (int i = 0; i < servers.size(); i++) {
                ServerConfig.ServerEntry server = servers.get(i);
                terminal.addLine(String.format("%d. %-20s (%s:%d)", 
                    i + 1, 
                    server.getName(), 
                    server.getIp(), 
                    server.getPort()
                ));
            }
        }
        terminal.addLine("-".repeat(40));  // Separator line
        terminal.addLine("");
        
        // Connection options
        terminal.addLine("Connect by:");
        terminal.addLine("* Enter a number to join a room");
        terminal.addLine("* Type an address (e.g. localhost:8887)");
        terminal.addLine("* Use /connect <ip:port>");
        terminal.addLine("");
        
        // Help hint
        terminal.addLine("Type /help for more commands");
    }
    
    private void connectToServer(String ip, int port) {
        try {
            URI serverUri = new URI("ws://" + ip + ":" + port);
            client = new ChatClient(serverUri, username, userColor,
                this::handleMessage,
                this::handleConnectionStatus);
            client.connect();
            currentState = State.CONNECTING;
            terminal.setConnectionInfo("CONNECTING", ip + ":" + port);
            terminal.addLine("Connecting to server...");
        } catch (Exception e) {
            terminal.addLine("Error connecting to server: " + e.getMessage());
            currentState = State.DISCONNECTED;
            showServerSelection();
        }
    }
    
    private void handleMessage(Message message) {
        terminal.addLine(message.format(), message.getDisplayColor());
        
        // Update room info if it's a room update
        if (message.getType() == Message.Type.ROOM_UPDATE) {
            String roomName = message.getNewValue().split(";")[0];
            terminal.setRoomInfo(roomName, client.getRoomColor());
        }
    }
    
    private void handleConnectionStatus(String status) {
        if (status.equals("CONNECTED")) {
            currentState = State.CONNECTED;
        } else if (status.equals("DISCONNECTED")) {
            currentState = State.DISCONNECTED;
            showServerSelection();
        }
        terminal.setConnectionInfo(status, client != null && client.isConnected() ? 
            client.getServerAddress() : "");
    }
    
    public static void main(String[] args) {
        new ClientApplication().run();
    }
} 