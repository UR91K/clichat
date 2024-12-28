package com.ur91k.clichat.terminal;

import com.ur91k.clichat.render.TextRenderer;
import org.joml.Vector4f;

/**
 * Base terminal implementation with shared functionality.
 * Handles basic terminal operations like text rendering and grid management.
 */
public abstract class BaseTerminal implements ITerminal {
    protected static final int DEFAULT_WIDTH = 80;  // characters
    protected static final Vector4f DEFAULT_FG = new Vector4f(0.8f, 0.8f, 0.8f, 1.0f);
    protected static final Vector4f DEFAULT_BG = new Vector4f(0.1f, 0.1f, 0.1f, 1.0f);
    protected static final int PADDING = 6;  // Minimal padding from window edges
    
    protected final TextRenderer textRenderer;
    protected final int width;
    protected final int charWidth = 8;  // Font dimensions
    protected final int charHeight = 16;
    
    // Grid dimensions
    protected int totalHeight;        // Total visible height in characters
    protected int messageAreaHeight;  // Calculated based on window size
    
    // Grid storage
    protected char[][] chars;
    protected Vector4f[][] fgColors;
    protected Vector4f[][] bgColors;
    protected int currentLine = 0;    // Current line in message history
    
    public BaseTerminal(TextRenderer textRenderer) {
        this(textRenderer, DEFAULT_WIDTH);
    }
    
    public BaseTerminal(TextRenderer textRenderer, int width) {
        this.textRenderer = textRenderer;
        this.width = width;
        
        // Start with a reasonably large buffer
        int initialHeight = 1000;  // Large enough to hold plenty of scrollback
        chars = new char[initialHeight][width];
        fgColors = new Vector4f[initialHeight][width];
        bgColors = new Vector4f[initialHeight][width];
        
        // Initialize with spaces and default colors
        for (int y = 0; y < initialHeight; y++) {
            for (int x = 0; x < width; x++) {
                chars[y][x] = ' ';
                fgColors[y][x] = new Vector4f(DEFAULT_FG);
                bgColors[y][x] = new Vector4f(DEFAULT_BG);
            }
        }
    }
    
    @Override
    public void handleResize(int width, int height) {
        totalHeight = (height - 2 * PADDING) / charHeight;
        messageAreaHeight = totalHeight;  // By default, use full height
        textRenderer.handleResize(width, height);
    }
    
    @Override
    public void addLine(String text) {
        addLine(text, null);
    }
    
    @Override
    public void addLine(String text, Vector4f color) {
        // Move to next line
        currentLine++;
        
        // Clear the new line
        for (int x = 0; x < width; x++) {
            chars[currentLine - 1][x] = ' ';
            fgColors[currentLine - 1][x] = new Vector4f(DEFAULT_FG);
            bgColors[currentLine - 1][x] = new Vector4f(DEFAULT_BG);
        }
        
        // Add text with color
        int x = 0;
        Vector4f textColor = color != null ? color : DEFAULT_FG;
        for (char c : text.toCharArray()) {
            if (x >= width) break;
            chars[currentLine - 1][x] = c;
            fgColors[currentLine - 1][x] = new Vector4f(textColor);
            x++;
        }
    }
    
    @Override
    public void clearLines() {
        // Reset all lines to empty
        for (int y = 0; y < chars.length; y++) {
            for (int x = 0; x < width; x++) {
                chars[y][x] = ' ';
                fgColors[y][x] = new Vector4f(DEFAULT_FG);
                bgColors[y][x] = new Vector4f(DEFAULT_BG);
            }
        }
        currentLine = 0;
    }
    
    protected void renderLine(int bufferLine, int screenY) {
        for (int x = 0; x < width; x++) {
            String charStr = String.valueOf(chars[bufferLine][x]);
            textRenderer.renderText(
                charStr,
                PADDING + (x * charWidth),
                screenY,
                fgColors[bufferLine][x]
            );
        }
    }
    
    protected void renderBlankLine(int y) {
        textRenderer.renderText(
            " ".repeat(width),
            PADDING,
            y,
            DEFAULT_FG
        );
    }
} 