package com.ur91k.clichat.terminal;

import com.ur91k.clichat.render.TextRenderer;
import org.joml.Vector4f;

/**
 * Core interface for terminal functionality.
 * Provides basic methods for terminal display and text manipulation.
 */
public interface ITerminal {
    /**
     * Handle window resize events
     */
    void handleResize(int width, int height);
    
    /**
     * Render the terminal contents
     */
    void render();
    
    /**
     * Add a line of text with default color
     */
    void addLine(String text);
    
    /**
     * Add a line of text with specified color
     */
    void addLine(String text, Vector4f color);
    
    /**
     * Clear all lines in the terminal
     */
    void clearLines();
} 