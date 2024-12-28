package com.ur91k.clichat.debug;

import com.ur91k.clichat.render.TextRenderer;
import com.ur91k.clichat.terminal.DebugTerminal;
import com.ur91k.clichat.util.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class FontDebugApp {
    private static final Logger logger = Logger.getLogger(FontDebugApp.class);
    private long window;
    private DebugTerminal terminal;
    private TextRenderer textRenderer;

    public static void main(String[] args) {
        new FontDebugApp().run();
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
        window = GLFW.glfwCreateWindow(800, 600, "Font Debug", 0, 0);
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
        terminal = new DebugTerminal(textRenderer, 80, "Font Character Grid");
        
        // Add character grid to terminal
        displayCharacterGrid();

        // Set up window resize callback
        GLFW.glfwSetFramebufferSizeCallback(window, (window, width, height) -> {
            GL11.glViewport(0, 0, width, height);
            terminal.handleResize(width, height);
        });
    }

    private void displayCharacterGrid() {
        terminal.addLine("ASCII Characters (32-126):");
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
        terminal.addLine("Press ESC to exit");
    }

    private void loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            
            terminal.render();
            
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();

            // Handle ESC key
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
                GLFW.glfwSetWindowShouldClose(window, true);
            }
        }
    }

    private void cleanup() {
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
} 