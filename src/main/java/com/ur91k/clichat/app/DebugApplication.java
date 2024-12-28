package com.ur91k.clichat.app;

import com.ur91k.clichat.render.TextRenderer;
import com.ur91k.clichat.render.Window;
import com.ur91k.clichat.terminal.DebugTerminal;
import com.ur91k.clichat.util.Logger;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class DebugApplication {
    private static final Logger logger = Logger.getLogger(DebugApplication.class);
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    
    private Window window;
    private DebugTerminal terminal;
    private TextRenderer textRenderer;
    private boolean running = true;
    
    public void run() {
        init();
        loop();
        cleanup();
    }
    
    private void init() {
        // Set up logging
        Logger.setGlobalMinimumLevel(Logger.Level.DEBUG);
        Logger.useColors(true);
        logger.info("Initializing DebugApplication");
        
        // Initialize GLFW
        if (!glfwInit()) {
            logger.error("Failed to initialize GLFW");
            throw new RuntimeException("Failed to initialize GLFW");
        }
        logger.debug("GLFW initialized");
        
        // Create window
        window = new Window("CLIChat Debug", WINDOW_WIDTH, WINDOW_HEIGHT);
        window.init();
        logger.debug("Window created: {}x{}", WINDOW_WIDTH, WINDOW_HEIGHT);
        
        // Initialize OpenGL
        glfwMakeContextCurrent(window.getHandle());
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        logger.debug("OpenGL context initialized");
        
        // Create text renderer and terminal
        textRenderer = new TextRenderer(WINDOW_WIDTH, WINDOW_HEIGHT);
        terminal = new DebugTerminal(textRenderer, WINDOW_WIDTH / 8); // Assuming 8 pixels per character
        terminal.handleResize(WINDOW_WIDTH, WINDOW_HEIGHT);
        logger.debug("Text renderer and terminal initialized");
        
        // Setup resize callback
        window.setResizeCallback((width, height) -> {
            logger.debug("Window resized to: {}x{}", width, height);
            glViewport(0, 0, width, height);
            terminal.handleResize(width, height);
        });
        
        // Setup input callbacks
        glfwSetCharCallback(window.getHandle(), new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                logger.trace("Character input: {} ({})", (char)codepoint, codepoint);
                terminal.handleCharacter((char)codepoint);
            }
        });
        
        glfwSetKeyCallback(window.getHandle(), new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                    if (key == GLFW_KEY_ESCAPE) {
                        logger.debug("ESC pressed, setting running to false");
                        running = false;
                    }
                }
            }
        });
        
        logger.info("Initialization complete");
    }
    
    private void loop() {
        logger.info("Entering main loop");
        while (running && !window.shouldClose()) {
            glClear(GL_COLOR_BUFFER_BIT);
            terminal.render();
            window.update();
        }
        logger.info("Main loop ended");
    }
    
    private void cleanup() {
        logger.info("Cleaning up resources");
        window.cleanup();
        glfwTerminate();
        logger.debug("Cleanup complete");
    }
    
    public static void main(String[] args) {
        new DebugApplication().run();
    }
} 