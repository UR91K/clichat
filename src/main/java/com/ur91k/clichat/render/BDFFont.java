package com.ur91k.clichat.render;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.stream.Collectors;
import org.lwjgl.BufferUtils;
import com.ur91k.clichat.util.Logger;

/**
 * A class for loading and managing BDF (Bitmap Distribution Format) fonts.
 * This implementation supports ASCII characters from 32 to 126 and creates
 * a texture atlas for efficient rendering.
 */
public class BDFFont extends BitmapFont {
    private static final Logger logger = Logger.getLogger(BDFFont.class);

    /**
     * Creates a new BDFFont from the specified input stream.
     * Parses the BDF file format and creates a texture atlas containing all glyphs.
     *
     * @param inputStream The input stream containing the BDF font data
     * @throws RuntimeException if the font cannot be loaded
     */
    public BDFFont(InputStream inputStream) {
        super(0, 0, null); // Temporary values
        try {
            // Store the entire file content first since we need multiple passes
            String fileContent = new BufferedReader(new InputStreamReader(inputStream))
                .lines()
                .collect(Collectors.joining("\n"));

            // Calculate dimensions and create buffer
            FontDimensions dimensions = calculateDimensions(fileContent);
            
            // Use reflection to set the final fields since we couldn't in super()
            try {
                java.lang.reflect.Field textureWidthField = BitmapFont.class.getDeclaredField("textureWidth");
                java.lang.reflect.Field textureHeightField = BitmapFont.class.getDeclaredField("textureHeight");
                java.lang.reflect.Field bitmapField = BitmapFont.class.getDeclaredField("bitmap");
                
                textureWidthField.setAccessible(true);
                textureHeightField.setAccessible(true);
                bitmapField.setAccessible(true);
                
                textureWidthField.set(this, dimensions.width);
                textureHeightField.set(this, dimensions.height);
                bitmapField.set(this, dimensions.buffer);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize font dimensions", e);
            }

            // Load the glyphs
            loadGlyphs(fileContent);
            
        } catch (Exception e) {
            logger.error("Failed to load BDF font", e);
            throw new RuntimeException("Failed to load BDF font", e);
        }
    }

    @SuppressWarnings("unused")
    private static class FontDimensions {
        final int width;
        final int height;
        final ByteBuffer buffer;
        final int boundingBoxWidth;
        final int boundingBoxHeight;
        final int baseline;

        FontDimensions(int width, int height, ByteBuffer buffer, 
                      int boundingBoxWidth, int boundingBoxHeight, int baseline) {
            this.width = width;
            this.height = height;
            this.buffer = buffer;
            this.boundingBoxWidth = boundingBoxWidth;
            this.boundingBoxHeight = boundingBoxHeight;
            this.baseline = baseline;
        }
    }

    private static FontDimensions calculateDimensions(String fileContent) throws Exception {
        int maxWidth = 0;
        int totalHeight = 0;
        int boundingBoxWidth = 0;
        int boundingBoxHeight = 0;
        int baseline = 0;
        int currentChar = -1;
        
        try (BufferedReader reader = new BufferedReader(new StringReader(fileContent))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.trim().split("\\s+");
                
                switch (tokens[0]) {
                    case "FONTBOUNDINGBOX":
                        boundingBoxWidth = Integer.parseInt(tokens[1]);
                        boundingBoxHeight = Integer.parseInt(tokens[2]);
                        baseline = -Integer.parseInt(tokens[4]);
                        maxWidth = Math.max(maxWidth, boundingBoxWidth);
                        break;
                    case "ENCODING":
                        currentChar = Integer.parseInt(tokens[1]);
                        if (currentChar >= 32 && currentChar <= 126) {
                            totalHeight += boundingBoxHeight;
                        }
                        break;
                }
            }
        }
        
        // Calculate texture dimensions (power of 2)
        int calculatedWidth = nextPowerOfTwo(maxWidth * 16);
        int calculatedHeight = nextPowerOfTwo(totalHeight);
        
        logger.debug("Creating texture atlas of size {}x{} for font {}x{}", 
            calculatedWidth, calculatedHeight, boundingBoxWidth, boundingBoxHeight);
        
        // Create and initialize the bitmap buffer
        ByteBuffer buffer = BufferUtils.createByteBuffer(calculatedWidth * calculatedHeight);
        for (int i = 0; i < calculatedWidth * calculatedHeight; i++) {
            buffer.put(i, (byte)0);
        }
        
        return new FontDimensions(calculatedWidth, calculatedHeight, buffer,
                                boundingBoxWidth, boundingBoxHeight, baseline);
    }

    private void loadGlyphs(String fileContent) throws Exception {
        try (BufferedReader reader = new BufferedReader(new StringReader(fileContent))) {
            int currentX = 0;
            int currentY = 0;
            int currentChar = -1;
            int bitmapWidth = 0;
            int bitmapHeight = 0;
            boolean[] currentBitmap = null;
            int xOffset = 0;
            int yOffset = 0;
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.trim().split("\\s+");
                
                switch (tokens[0]) {
                    case "FONTBOUNDINGBOX":
                        this.fontBoundingBoxWidth = Integer.parseInt(tokens[1]);
                        this.fontBoundingBoxHeight = Integer.parseInt(tokens[2]);
                        this.baseline = -Integer.parseInt(tokens[4]);
                        break;
                        
                    case "ENCODING":
                        currentChar = Integer.parseInt(tokens[1]);
                        break;
                        
                    case "BBX":
                        bitmapWidth = Integer.parseInt(tokens[1]);
                        bitmapHeight = Integer.parseInt(tokens[2]);
                        xOffset = Integer.parseInt(tokens[3]);
                        yOffset = Integer.parseInt(tokens[4]);
                        currentBitmap = new boolean[bitmapWidth * bitmapHeight];
                        break;
                        
                    case "BITMAP":
                        // Read bitmap data
                        for (int i = 0; i < bitmapHeight; i++) {
                            line = reader.readLine();
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
                            copyGlyphToAtlas(currentBitmap, currentX, currentY, bitmapWidth, bitmapHeight);
                            
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
    }

    private void copyGlyphToAtlas(boolean[] glyphBitmap, int xOffset, int yOffset, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int atlasX = xOffset + x;
                int atlasY = yOffset + y;
                
                // Skip if we're outside the texture bounds
                if (atlasX >= textureWidth || atlasY >= textureHeight) {
                    logger.warn("Attempted to write outside texture bounds at ({}, {})", atlasX, atlasY);
                    continue;
                }
                
                int atlasPos = atlasY * textureWidth + atlasX;
                if (atlasPos >= 0 && atlasPos < bitmap.capacity()) {
                    bitmap.put(atlasPos, glyphBitmap[y * width + x] ? (byte)0xFF : 0);
                } else {
                    logger.warn("Buffer position {} out of bounds (capacity: {})", atlasPos, bitmap.capacity());
                }
            }
        }
    }
} 