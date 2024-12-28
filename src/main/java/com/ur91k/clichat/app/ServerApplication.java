package com.ur91k.clichat.app;

import com.ur91k.clichat.render.TextRenderer;
import com.ur91k.clichat.render.Window;
import com.ur91k.clichat.terminal.ChatTerminal;
import com.ur91k.clichat.net.ChatServer;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.joml.Vector4f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class ServerApplication {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    
    private final String ip;
    private final int port;
    private Window window;
    private ChatTerminal terminal;
    private TextRenderer textRenderer;
    private ChatServer server;
    private boolean running = true;
    
    public ServerApplication(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }
    
    public void run() {
        init();
        loop();
        cleanup();
    }
    
    private void init() {
        // Initialize GLFW
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        
        // Create window
        window = new Window("CLIChat Server", WINDOW_WIDTH, WINDOW_HEIGHT);
        window.init();
        
        // Initialize OpenGL
        glfwMakeContextCurrent(window.getHandle());
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        // Create text renderer and terminal
        textRenderer = new TextRenderer(WINDOW_WIDTH, WINDOW_HEIGHT);
        terminal = new ChatTerminal(textRenderer);
        terminal.handleResize(WINDOW_WIDTH, WINDOW_HEIGHT);
        terminal.setUsername("SERVER");
        
        // Set server prompt color to red to distinguish it
        terminal.setUsernameColor(new Vector4f(0.8f, 0.2f, 0.2f, 1.0f));
        
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
                        handleCommand();
                    } else if (key == GLFW_KEY_ESCAPE) {
                        running = false;
                    }
                }
            }
        });
        
        // Start the server
        try {
            server = new ChatServer(ip, port);
            server.start();
            terminal.setConnectionInfo("LISTENING", ip + ":" + port);
            terminal.setRoomInfo("main_room", new Vector4f(0.6f, 0.8f, 1.0f, 1.0f));
            
            // Add welcome messages
            terminal.addLine("Server started");
            terminal.addLine("Listening for connections on " + ip + ":" + port);
            terminal.addLine("Type /help for available commands");
        } catch (Exception e) {
            terminal.addLine("Error starting server: " + e.getMessage());
            running = false;
        }
    }
    
    @SuppressWarnings("unused")
    private void handleCommand() {
        String input = terminal.getCurrentInput().trim();
        if (!input.isEmpty()) {
            if (input.startsWith("/")) {
                String[] parts = input.split("\\s+", 2);
                String command = parts[0].toLowerCase();
                String args = parts.length > 1 ? parts[1] : "";
                
                switch (command) {
                    case "/help":
                        terminal.addLine("Available commands:");
                        terminal.addLine("/kick <user> - Kick a user from the server");
                        terminal.addLine("/ban <user> - Ban a user by name");
                        terminal.addLine("/banip <ip> - Ban an IP address");
                        terminal.addLine("/unban <user|ip> - Remove a ban");
                        terminal.addLine("/op <user> - Give operator status");
                        terminal.addLine("/deop <user> - Remove operator status");
                        terminal.addLine("/list - List connected users");
                        terminal.addLine("/stop - Stop the server");
                        break;
                    case "/stop":
                        terminal.addLine("Stopping server...");
                        running = false;
                        break;
                    case "/list":
                        // TODO: Implement user listing
                        terminal.addLine("No users connected");
                        break;
                    default:
                        terminal.addLine("Unknown command. Type /help for available commands");
                        break;
                }
            }
            terminal.clearInput();
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
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        window.cleanup();
        glfwTerminate();
    }
} 