package com.ur91k.clichat.debug;

import com.ur91k.clichat.render.TextRenderer;
import com.ur91k.clichat.terminal.DebugTerminal;
import com.ur91k.clichat.util.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.joml.Vector4f;

public class DebugApp {
    private static final Logger logger = Logger.getLogger(DebugApp.class);
    private long window;
    private DebugTerminal terminal;
    private TextRenderer textRenderer;
    private boolean running = true;
    
    private enum View {
        MENU,
        FONT_GRID,
        COLOR_TEST,
        FONT_ATLAS
    }
    private View currentView = View.MENU;

    public static void main(String[] args) {
        new DebugApp().run();
    }

    private void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        // Initialize GLFW
        if (!GLFW.glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }

        // Configure window
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        // Create window
        window = GLFW.glfwCreateWindow(800, 600, "Debug Tools", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create window");
        }

        // Center window
        var vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        GLFW.glfwSetWindowPos(window,
            (vidMode.width() - 800) / 2,
            (vidMode.height() - 600) / 2);

        // Make OpenGL context current
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);

        // Initialize OpenGL
        GL.createCapabilities();
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Create text renderer with window dimensions
        textRenderer = new TextRenderer(800, 600);
        terminal = new DebugTerminal(textRenderer, 80, "Debug Tools");
        
        // Set up input handling
        GLFW.glfwSetKeyCallback(window, new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action == GLFW.GLFW_PRESS) {
                    handleKeyPress(key);
                }
            }
        });

        // Set up window resize callback
        GLFW.glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
            GL11.glViewport(0, 0, width, height);
            terminal.handleResize(width, height);
        });
        
        showMenu();
    }
    
    private void handleKeyPress(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (currentView == View.MENU) {
                running = false;
            } else {
                currentView = View.MENU;
                showMenu();
            }
            return;
        }
        
        switch (currentView) {
            case MENU -> handleMenuInput(key);
            case FONT_GRID -> {} // No special input needed
            case COLOR_TEST -> {} // No special input needed
            case FONT_ATLAS -> {} // No special input needed
        }
    }
    
    private void handleMenuInput(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_1 -> {
                currentView = View.FONT_GRID;
                showFontGrid();
            }
            case GLFW.GLFW_KEY_2 -> {
                currentView = View.COLOR_TEST;
                showColorTest();
            }
            case GLFW.GLFW_KEY_3 -> {
                currentView = View.FONT_ATLAS;
                showFontAtlas();
            }
        }
    }
    
    private void showMenu() {
        terminal.clearLines();
        terminal.addLine("Debug Tools Menu");
        terminal.addLine("");
        terminal.addLine("1. Font Character Grid");
        terminal.addLine("2. Color Test");
        terminal.addLine("3. Font Atlas");
        terminal.addLine("");
        terminal.addLine("ESC - Exit");
    }
    
    private void showFontGrid() {
        terminal.clearLines();
        terminal.addLine("Font Character Grid (ASCII 32-126)");
        terminal.addLine("");

        // Header row
        StringBuilder header = new StringBuilder("   ");
        for (int x = 0; x < 16; x++) {
            header.append(String.format(" %X", x));
        }
        terminal.addLine(header.toString());

        // Character grid
        for (int row = 2; row < 8; row++) {
            StringBuilder line = new StringBuilder();
            line.append(String.format("%X0 ", row));
            
            for (int col = 0; col < 16; col++) {
                int charCode = row * 16 + col;
                if (charCode >= 32 && charCode <= 126) {
                    line.append(" ").append((char)charCode);
                } else {
                    line.append("  ");
                }
            }
            terminal.addLine(line.toString());
        }

        terminal.addLine("");
        terminal.addLine("ESC - Return to Menu");
    }
    
    private void showColorTest() {
        terminal.clearLines();
        terminal.addLine("Color Test");
        terminal.addLine("");
        
        // Basic colors
        terminal.addLine("Basic Colors:", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
        terminal.addLine("Red", new Vector4f(1.0f, 0.0f, 0.0f, 1.0f));
        terminal.addLine("Green", new Vector4f(0.0f, 1.0f, 0.0f, 1.0f));
        terminal.addLine("Blue", new Vector4f(0.0f, 0.0f, 1.0f, 1.0f));
        terminal.addLine("");
        
        // Color gradients
        terminal.addLine("Red Gradient:", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
        StringBuilder redGradient = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            float intensity = i / 9.0f;
            redGradient.append("█");
            terminal.addLine(redGradient.toString(), new Vector4f(intensity, 0.0f, 0.0f, 1.0f));
        }
        terminal.addLine("");
        
        terminal.addLine("Green Gradient:", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
        StringBuilder greenGradient = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            float intensity = i / 9.0f;
            greenGradient.append("█");
            terminal.addLine(greenGradient.toString(), new Vector4f(0.0f, intensity, 0.0f, 1.0f));
        }
        terminal.addLine("");
        
        terminal.addLine("Blue Gradient:", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
        StringBuilder blueGradient = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            float intensity = i / 9.0f;
            blueGradient.append("█");
            terminal.addLine(blueGradient.toString(), new Vector4f(0.0f, 0.0f, intensity, 1.0f));
        }
        
        terminal.addLine("");
        terminal.addLine("ESC - Return to Menu");
    }
    
    private void showFontAtlas() {
        terminal.clearLines();
        terminal.addLine("Font Atlas Test");
        terminal.addLine("");
        
        // Test various text styles and combinations
        terminal.addLine("Normal Text");
        terminal.addLine("Colored Text", new Vector4f(0.5f, 0.8f, 1.0f, 1.0f));
        terminal.addLine("");
        
        // Box drawing characters
        terminal.addLine("Box Drawing:");
        terminal.addLine("┌─────────┐");
        terminal.addLine("│ Testing │");
        terminal.addLine("└─────────┘");
        terminal.addLine("");
        
        // Special characters
        terminal.addLine("Special Characters:");
        terminal.addLine("Arrows: ← → ↑ ↓");
        terminal.addLine("Blocks: █ ▀ ▄ ▌ ▐");
        terminal.addLine("Misc: ° ± × ÷ ≠ ≤ ≥");
        
        terminal.addLine("");
        terminal.addLine("ESC - Return to Menu");
    }

    private void loop() {
        while (running && !GLFW.glfwWindowShouldClose(window)) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            terminal.render();
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void cleanup() {
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
} 