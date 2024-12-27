package com.ur91k.clichat.app;

import com.ur91k.clichat.render.RenderSystem;
import com.ur91k.clichat.render.TextRenderer;
import com.ur91k.clichat.render.Window;
import com.ur91k.clichat.util.Logger;
import com.ur91k.clichat.util.Time;

import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;

public class Engine {
    private static final Logger logger = Logger.getLogger(Engine.class);
    private final Window window;
    private final RenderSystem renderSystem;
    private final TextRenderer textRenderer;
    private boolean running;

    // Background color (dark theme)
    private static final Vector4f BACKGROUND_COLOR = new Vector4f(0.1f, 0.1f, 0.1f, 1.0f);

    public Engine(String title, int width, int height) {
        // Set up logging
        Logger.setGlobalMinimumLevel(Logger.Level.DEBUG);
        Logger.useColors(true);
        Logger.showTimestamp(true);

        // Create window and renderers
        window = new Window(title, width, height);
        window.init();
        
        renderSystem = new RenderSystem(width, height);
        textRenderer = new TextRenderer(width, height);

        // Set up window resize handler
        window.setResizeCallback((newWidth, newHeight) -> {
            logger.debug("Window resized to {}x{}", newWidth, newHeight);
            renderSystem.handleResize(newWidth, newHeight);
            textRenderer.handleResize(newWidth, newHeight);
        });
    }
    
    private void init() {
        logger.info("Initializing chat client...");
        
        // Initialize timing system
        Time.init();
        
        // Set up OpenGL state
        glClearColor(
            BACKGROUND_COLOR.x,
            BACKGROUND_COLOR.y,
            BACKGROUND_COLOR.z,
            BACKGROUND_COLOR.w
        );
        
        running = true;
        logger.info("Chat client initialized successfully");
    }
    
    private void mainLoop() {
        while (running && !window.shouldClose()) {
            Time.update();
            
            // Clear frame
            renderSystem.beginFrame();
            
            // TODO: Render chat UI
            // - Message history
            // - Input box
            // - Status bar
            
            // Update window
            window.update();
        }
    }
    
    public void start() {
        if (!running) {
            init();
        }
        mainLoop();
        cleanup();
    }
    
    private void cleanup() {
        logger.info("Cleaning up resources...");
        renderSystem.cleanup();
        textRenderer.cleanup();
        window.cleanup();
        logger.info("Cleanup complete");
    }
    
    public void stop() {
        running = false;
    }

    public RenderSystem getRenderSystem() {
        return renderSystem;
    }

    public TextRenderer getTextRenderer() {
        return textRenderer;
    }

    public Window getWindow() {
        return window;
    }
} 