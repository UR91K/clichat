package com.ur91k.clichat.render;

import org.joml.Vector4f;

/**
 * Utility class for color-related operations.
 */
public final class ColorUtils {
    private ColorUtils() {} // Prevent instantiation
    
    /**
     * Creates a Vector4f color from RGB components (0-255).
     */
    public static Vector4f rgb(int r, int g, int b) {
        return new Vector4f(r / 255.0f, g / 255.0f, b / 255.0f, 1.0f);
    }
    
    /**
     * Creates a Vector4f color from a 24-bit RGB hex value.
     * Format: 0xRRGGBB
     */
    public static Vector4f rgb(int hex) {
        int r = (hex >> 16) & 0xFF;
        int g = (hex >> 8) & 0xFF;
        int b = hex & 0xFF;
        
        return new Vector4f(
            r / 255.0f,
            g / 255.0f,
            b / 255.0f,
            1.0f
        );
    }
    
    /**
     * Creates a Vector4f color from a 32-bit RGBA hex value.
     * Format: 0xRRGGBBAA
     */
    public static Vector4f rgba(int hex) {
        int r = (hex >> 24) & 0xFF;
        int g = (hex >> 16) & 0xFF;
        int b = (hex >> 8) & 0xFF;
        int a = hex & 0xFF;
        
        return new Vector4f(
            r / 255.0f,
            g / 255.0f,
            b / 255.0f,
            a / 255.0f
        );
    }
} 