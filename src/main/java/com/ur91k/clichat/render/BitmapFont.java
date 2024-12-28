package com.ur91k.clichat.render;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for bitmap-based fonts.
 * Provides common functionality and structure for different bitmap font formats.
 */
public abstract class BitmapFont {
    /** Maps ASCII characters to their corresponding glyph data */
    protected final Map<Character, Glyph> glyphs = new HashMap<>();
    
    /** The texture atlas containing all glyph bitmaps */
    protected final ByteBuffer bitmap;
    
    /** Width of the texture atlas in pixels */
    protected final int textureWidth;
    
    /** Height of the texture atlas in pixels */
    protected final int textureHeight;
    
    /** Width of the font's bounding box */
    protected int fontBoundingBoxWidth;
    
    /** Height of the font's bounding box */
    protected int fontBoundingBoxHeight;
    
    /** Baseline offset for proper glyph positioning */
    protected int baseline;

    /**
     * Represents a single character glyph in the font.
     */
    public static class Glyph {
        public final int width;
        public final int height;
        public final int xOffset;
        public final int yOffset;
        public final int xAdvance;
        public final float s0, t0, s1, t1;
        public final boolean[] bitmap;

        public Glyph(int width, int height, int xOffset, int yOffset, int xAdvance,
                    float s0, float t0, float s1, float t1, boolean[] bitmap) {
            this.width = width;
            this.height = height;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.xAdvance = xAdvance;
            this.s0 = s0;
            this.t0 = t0;
            this.s1 = s1;
            this.t1 = t1;
            this.bitmap = bitmap;
        }
    }

    /**
     * Protected constructor for initializing a BitmapFont with pre-calculated dimensions.
     */
    protected BitmapFont(int textureWidth, int textureHeight, ByteBuffer bitmap) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.bitmap = bitmap;
    }

    /**
     * Factory method to create a bitmap font from an input stream.
     * Subclasses should implement this to handle their specific font format.
     */
    public static BitmapFont create(InputStream inputStream, String format) {
        switch (format.toLowerCase()) {
            case "bdf":
                return new BDFFont(inputStream);
            case "fon":
                return new FONFont(inputStream);
            default:
                throw new IllegalArgumentException("Unsupported font format: " + format);
        }
    }

    /**
     * Retrieves the glyph data for a specific character.
     *
     * @param c The character to look up
     * @return The Glyph object containing the character's rendering data, or null if not found
     */
    public Glyph getGlyph(char c) {
        return glyphs.get(c);
    }

    /**
     * Returns the texture atlas containing all glyph bitmaps.
     *
     * @return ByteBuffer containing the texture atlas data
     */
    public ByteBuffer getBitmap() {
        return bitmap;
    }

    /**
     * Returns the width of the texture atlas.
     *
     * @return Width in pixels
     */
    public int getTextureWidth() {
        return textureWidth;
    }

    /**
     * Returns the height of the texture atlas.
     *
     * @return Height in pixels
     */
    public int getTextureHeight() {
        return textureHeight;
    }

    /**
     * Returns the width of the font's bounding box.
     *
     * @return Width in pixels
     */
    public int getFontBoundingBoxWidth() {
        return fontBoundingBoxWidth;
    }

    /**
     * Returns the height of the font's bounding box.
     *
     * @return Height in pixels
     */
    public int getFontBoundingBoxHeight() {
        return fontBoundingBoxHeight;
    }

    /**
     * Returns the baseline offset.
     *
     * @return Baseline offset in pixels
     */
    public int getBaseline() {
        return baseline;
    }

    /**
     * Helper method to calculate the next power of two.
     */
    protected static int nextPowerOfTwo(int n) {
        int value = 1;
        while (value < n) value <<= 1;
        return value;
    }

    /**
     * Checks if a character has a glyph in this font.
     * @param c The character to check
     * @return true if the character has a glyph, false otherwise
     */
    public boolean hasGlyph(char c) {
        return glyphs.containsKey(c);
    }
} 