package com.ur91k.clichat.render;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.lwjgl.BufferUtils;

/**
 * A class for loading and managing BDF (Bitmap Distribution Format) fonts.
 * This implementation supports ASCII characters from 32 to 126 and creates
 * a texture atlas for efficient rendering.
 */
public class BDFFont {
    /** Maps ASCII characters to their corresponding glyph data */
    private final Map<Character, Glyph> glyphs = new HashMap<>();
    
    /** The texture atlas containing all glyph bitmaps */
    private final ByteBuffer bitmap;
    
    /** Width of the texture atlas in pixels */
    private final int textureWidth;
    
    /** Height of the texture atlas in pixels */
    private final int textureHeight;
    
    /** Width of the font's bounding box */
    private int fontBoundingBoxWidth;
    
    /** Height of the font's bounding box */
    private int fontBoundingBoxHeight;
    
    /** Baseline offset for proper glyph positioning */
    private int baseline;
    
    /**
     * Represents a single character glyph in the font.
     * Contains both geometric and texture coordinate information.
     */
    public static class Glyph {
        /** Width of the glyph in pixels */
        public final int width;
        
        /** Height of the glyph in pixels */
        public final int height;
        
        /** Horizontal offset from the cursor position */
        public final int xOffset;
        
        /** Vertical offset from the baseline */
        public final int yOffset;
        
        /** Horizontal advance after rendering this glyph */
        public final int xAdvance;
        
        /** Texture coordinates for the glyph in the atlas */
        public final float s0, t0, s1, t1;
        
        /** Binary bitmap data for the glyph */
        public final boolean[] bitmap;
        
        /**
         * Creates a new Glyph with the specified parameters.
         *
         * @param width    Width of the glyph
         * @param height   Height of the glyph
         * @param xOffset  Horizontal offset
         * @param yOffset  Vertical offset
         * @param xAdvance Horizontal advance
         * @param s0       Left texture coordinate
         * @param t0       Top texture coordinate
         * @param s1       Right texture coordinate
         * @param t1       Bottom texture coordinate
         * @param bitmap   Binary bitmap data
         */
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
     * Creates a new BDFFont from the specified input stream.
     * Parses the BDF file format and creates a texture atlas containing all glyphs.
     *
     * @param inputStream The input stream containing the BDF font data
     * @throws RuntimeException if the font cannot be loaded
     */
    @SuppressWarnings("unused")
    public BDFFont(InputStream inputStream) {
        try {
            // Store the entire file content first
            String fileContent = new BufferedReader(new InputStreamReader(inputStream))
                .lines()
                .collect(Collectors.joining("\n"));
            
            // First pass: determine texture size needed
            int maxWidth = 0;
            int totalHeight = 0;
            int currentChar = -1;
            
            try (BufferedReader firstPass = new BufferedReader(new StringReader(fileContent))) {
                String line;
                while ((line = firstPass.readLine()) != null) {
                    String[] tokens = line.trim().split("\\s+");
                    
                    switch (tokens[0]) {
                        case "FONTBOUNDINGBOX":
                            fontBoundingBoxWidth = Integer.parseInt(tokens[1]);
                            fontBoundingBoxHeight = Integer.parseInt(tokens[2]);
                            baseline = -Integer.parseInt(tokens[4]);
                            maxWidth = Math.max(maxWidth, fontBoundingBoxWidth);
                            break;
                        case "ENCODING":
                            currentChar = Integer.parseInt(tokens[1]);
                            if (currentChar >= 32 && currentChar <= 126) {
                                totalHeight += fontBoundingBoxHeight;
                            }
                            break;
                    }
                }
            }
            
            // Calculate texture dimensions (power of 2)
            textureWidth = nextPowerOfTwo(maxWidth * 16);
            textureHeight = nextPowerOfTwo(totalHeight);
            bitmap = BufferUtils.createByteBuffer(textureWidth * textureHeight);
            
            // Second pass: read glyphs
            try (BufferedReader secondPass = new BufferedReader(new StringReader(fileContent))) {
                int currentX = 0;
                int currentY = 0;
                currentChar = -1;
                int bitmapWidth = 0;
                int bitmapHeight = 0;
                boolean[] currentBitmap = null;
                int currentRow = 0;
                int xOffset = 0;
                int yOffset = 0;
                
                String line;
                while ((line = secondPass.readLine()) != null) {
                    String[] tokens = line.trim().split("\\s+");
                    
                    switch (tokens[0]) {
                        case "ENCODING":
                            currentChar = Integer.parseInt(tokens[1]);
                            break;
                            
                        case "BBX":
                            bitmapWidth = Integer.parseInt(tokens[1]);
                            bitmapHeight = Integer.parseInt(tokens[2]);
                            xOffset = Integer.parseInt(tokens[3]);
                            yOffset = Integer.parseInt(tokens[4]);
                            currentBitmap = new boolean[bitmapWidth * bitmapHeight];
                            currentRow = 0;
                            break;
                            
                        case "BITMAP":
                            // Read bitmap data
                            for (int i = 0; i < bitmapHeight; i++) {
                                line = secondPass.readLine();
                                int value = Integer.parseInt(line, 16);
                                for (int j = 0; j < bitmapWidth; j++) {
                                    currentBitmap[i * bitmapWidth + j] = 
                                        ((value >> (bitmapWidth - 1 - j)) & 1) == 1;
                                }
                            }
                            break;
                            
                        case "ENDCHAR":
                            if (currentChar >= 32 && currentChar <= 126 && currentBitmap != null) {
                                // Calculate texture coordinates
                                float s0 = (float)currentX / textureWidth;
                                float t0 = (float)currentY / textureHeight;
                                float s1 = (float)(currentX + bitmapWidth) / textureWidth;
                                float t1 = (float)(currentY + bitmapHeight) / textureHeight;
                                
                                // Create glyph
                                Glyph glyph = new Glyph(
                                    bitmapWidth, bitmapHeight,
                                    xOffset, baseline + yOffset,
                                    fontBoundingBoxWidth,
                                    s0, t0, s1, t1,
                                    currentBitmap
                                );
                                glyphs.put((char)currentChar, glyph);
                                
                                // Copy bitmap to texture
                                for (int y = 0; y < bitmapHeight; y++) {
                                    for (int x = 0; x < bitmapWidth; x++) {
                                        int pos = (currentY + y) * textureWidth + currentX + x;
                                        bitmap.put(pos, currentBitmap[y * bitmapWidth + x] ? (byte)0xFF : 0);
                                    }
                                }
                                
                                // Update position
                                currentX += bitmapWidth;
                                if (currentX + bitmapWidth > textureWidth) {
                                    currentX = 0;
                                    currentY += fontBoundingBoxHeight;
                                }
                            }
                            break;
                    }
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load BDF font", e);
        }
    }
    
    /**
     * Returns the next power of two greater than or equal to the input value.
     * Used for calculating texture dimensions.
     *
     * @param n The input value
     * @return The next power of two
     */
    private int nextPowerOfTwo(int n) {
        int value = 1;
        while (value < n) value <<= 1;
        return value;
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
} 