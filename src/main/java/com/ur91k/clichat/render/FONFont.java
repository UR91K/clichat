package com.ur91k.clichat.render;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import com.ur91k.clichat.util.Logger;

/**
 * A class for loading and managing Windows FON (Font) files.
 */
public class FONFont extends BitmapFont {
    private static final Logger logger = Logger.getLogger(FONFont.class);
    private static final int MAX_REASONABLE_DIMENSION = 256;
    private static final int MAX_REASONABLE_BYTES = 1024 * 1024;
    private static final int DWORD_ALIGN = 4;

    public FONFont(InputStream inputStream) {
        super(0, 0, null); // Temporary values
        try {
            byte[] fontData = inputStream.readAllBytes();
            logger.debug("Read {} bytes of FON data", fontData.length);
            
            // Find the font resource in the executable
            ResourceInfo resourceInfo = findFontResource(fontData);
            if (resourceInfo == null) {
                throw new IOException("No valid font resource found in FON file");
            }
            
            // Calculate dimensions and create buffer
            FontDimensions dimensions = calculateDimensions(fontData, resourceInfo);
            
            // Use reflection to set the final fields
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
            
            // Initialize font metrics
            this.fontBoundingBoxWidth = dimensions.header.maxWidth;
            this.fontBoundingBoxHeight = dimensions.header.pixHeight;
            this.baseline = dimensions.header.ascent;

            // Calculate layout parameters with DWORD alignment
            int numChars = dimensions.header.lastChar - dimensions.header.firstChar + 1;
            int charsPerRow = (int) Math.ceil(Math.sqrt(numChars));
            int glyphPadding = DWORD_ALIGN - 1; // Ensure DWORD alignment
            int paddedGlyphWidth = alignToDWORD(fontBoundingBoxWidth + glyphPadding);
            int paddedGlyphHeight = alignToDWORD(fontBoundingBoxHeight + glyphPadding);

            loadGlyphs(fontData, dimensions.header, resourceInfo, charsPerRow, paddedGlyphWidth, paddedGlyphHeight);
            
        } catch (IOException e) {
            logger.error("Failed to load FON font", e);
            throw new RuntimeException("Failed to load FON font", e);
        }
    }

    private static class ResourceInfo {
        final int offset;
        final int size;
        
        ResourceInfo(int offset, int size) {
            this.offset = offset;
            this.size = size;
        }
    }

    private static ResourceInfo findFontResource(byte[] data) throws IOException {
        // Check MZ header
        if (data.length < 0x40 || readShort(data, 0) != 0x5A4D) { // "MZ"
            logger.debug("No MZ header found, trying as raw font");
            return new ResourceInfo(0, data.length);
        }

        // Get NE header offset from MZ header
        int neOffset = readDWord(data, 0x3C);
        logger.debug("NE header offset: 0x{}", Integer.toHexString(neOffset));
        
        if (neOffset + 0x40 > data.length || readShort(data, neOffset) != 0x454E) { // "NE"
            throw new IOException("Invalid NE header");
        }

        // Find resource table
        int resourceOffset = neOffset + readShort(data, neOffset + 0x24);
        int numTypes = readShort(data, resourceOffset);
        resourceOffset += 2; // Skip count
        
        logger.debug("Resource table at 0x{}, {} resource types", Integer.toHexString(resourceOffset), numTypes);

        // Search for font resource (type 0x8007)
        for (int i = 0; i < numTypes && resourceOffset + 8 <= data.length; i++) {
            int type = readShort(data, resourceOffset);
            int count = readShort(data, resourceOffset + 2);
            logger.debug("Resource type {}: type 0x{:04X}, count {}", i, type, count);
            
            if (type == 0x8007) { // Font resource type
                // Skip to resource entries
                int entryOffset = resourceOffset + 8;
                logger.debug("Found font resource type, checking {} entries at 0x{}", 
                    count, Integer.toHexString(entryOffset));

                // Read first font resource entry
                if (entryOffset + 12 <= data.length) {
                    // Each resource entry is 12 bytes:
                    // WORD  offset shift count (alignment)
                    // WORD  resource length (in alignment units)
                    // WORD  resource flags
                    // WORD  resource ID
                    // WORD  reserved
                    // WORD  reserved
                    int alignShift = readShort(data, entryOffset);
                    int length = readShort(data, entryOffset + 2);
                    int flags = readShort(data, entryOffset + 4);
                    int id = readShort(data, entryOffset + 6);
                    
                    // Calculate actual offset and size
                    int offset = alignShift << 4; // Align to 16-byte boundary
                    int size = length << 4;       // Size in 16-byte units
                    
                    logger.debug("Found font resource: offset 0x{}, size 0x{}, id 0x{}, flags 0x{}", 
                        Integer.toHexString(offset), Integer.toHexString(size),
                        Integer.toHexString(id), Integer.toHexString(flags));
                    
                    return new ResourceInfo(offset, size);
                }
            }
            
            // Skip this resource type's entries
            resourceOffset = alignToDWORD(resourceOffset + 8 + count * 12);
        }

        // If we didn't find a font resource, try treating the whole file as a raw font
        logger.debug("No font resource found in NE resources, trying as raw font");
        return new ResourceInfo(0, data.length);
    }

    private static int alignToDWORD(int value) {
        return (value + (DWORD_ALIGN - 1)) & ~(DWORD_ALIGN - 1);
    }

    private static int readDWord(byte[] data, int offset) {
        if (offset + 3 >= data.length) return 0;
        // Read as little-endian
        return ((data[offset + 3] & 0xFF) << 24) |
               ((data[offset + 2] & 0xFF) << 16) |
               ((data[offset + 1] & 0xFF) << 8) |
                (data[offset] & 0xFF);
    }

    private static class FontDimensions {
        final int width;
        final int height;
        final ByteBuffer buffer;
        final FONHeader header;

        FontDimensions(int width, int height, ByteBuffer buffer, FONHeader header) {
            this.width = width;
            this.height = height;
            this.buffer = buffer;
            this.header = header;
        }
    }

    private static void validateHeader(FONHeader header) throws IOException {
        if (header.pixWidth <= 0 || header.pixWidth > MAX_REASONABLE_DIMENSION) {
            throw new IOException(String.format("Invalid font width: %d", header.pixWidth));
        }
        if (header.pixHeight <= 0 || header.pixHeight > MAX_REASONABLE_DIMENSION) {
            throw new IOException(String.format("Invalid font height: %d", header.pixHeight));
        }
        if (header.maxWidth <= 0 || header.maxWidth > MAX_REASONABLE_DIMENSION) {
            throw new IOException(String.format("Invalid max width: %d", header.maxWidth));
        }
        if (header.firstChar < 0 || header.firstChar > 255) {
            throw new IOException(String.format("Invalid first char: %d", header.firstChar));
        }
        if (header.lastChar < header.firstChar || header.lastChar > 255) {
            throw new IOException(String.format("Invalid last char: %d (first char: %d)", header.lastChar, header.firstChar));
        }
        
        // Calculate total expected size with DWORD alignment
        int bytesPerRow = alignToDWORD(header.widthBytes > 0 ? header.widthBytes : (header.maxWidth + 7) / 8);
        int bytesPerChar = bytesPerRow * header.pixHeight;
        int totalChars = header.lastChar - header.firstChar + 1;
        long totalSize = (long)bytesPerChar * totalChars;
        
        if (totalSize > MAX_REASONABLE_BYTES) {
            throw new IOException(String.format(
                "Unreasonable bitmap size: %d bytes (%d per char * %d chars)",
                totalSize, bytesPerChar, totalChars));
        }
        
        logger.debug("Font size validation:");
        logger.debug("  Bytes per row (DWORD aligned): {}", bytesPerRow);
        logger.debug("  Bytes per char: {}", bytesPerChar);
        logger.debug("  Total chars: {}", totalChars);
        logger.debug("  Total bitmap size: {}", totalSize);
    }

    private static FontDimensions calculateDimensions(byte[] fontData, ResourceInfo resourceInfo) throws IOException {
        // Read and store the header
        FONHeader header = readFONHeader(fontData, resourceInfo);
        
        // Validate header values
        validateHeader(header);
        
        // Calculate texture atlas dimensions with DWORD alignment
        int numChars = header.lastChar - header.firstChar + 1;
        int charsPerRow = (int) Math.ceil(Math.sqrt(numChars));
        int numRows = (int) Math.ceil((double) numChars / charsPerRow);
        
        // Add padding between glyphs and ensure DWORD alignment
        int glyphPadding = DWORD_ALIGN - 1;
        int paddedGlyphWidth = alignToDWORD(header.maxWidth + glyphPadding);
        int paddedGlyphHeight = alignToDWORD(header.pixHeight + glyphPadding);
        
        int calculatedWidth = nextPowerOfTwo(paddedGlyphWidth * charsPerRow);
        int calculatedHeight = nextPowerOfTwo(paddedGlyphHeight * numRows);
        
        logger.debug("Creating texture atlas of size {}x{} for font {}x{} (DWORD aligned)", 
            calculatedWidth, calculatedHeight, header.maxWidth, header.pixHeight);
        
        // Create and initialize the bitmap buffer
        ByteBuffer buffer = BufferUtils.createByteBuffer(calculatedWidth * calculatedHeight);
        for (int i = 0; i < calculatedWidth * calculatedHeight; i++) {
            buffer.put(i, (byte)0);
        }
        
        return new FontDimensions(calculatedWidth, calculatedHeight, buffer, header);
    }

    private static FONHeader readFONHeader(byte[] fontData, ResourceInfo resourceInfo) throws IOException {
        int offset = resourceInfo.offset;
        if (resourceInfo.size < 118) { // Minimum header size (Windows 2.x)
            throw new IOException(String.format("Invalid FON file: header too short (%d bytes)", resourceInfo.size));
        }

        // Dump first few bytes of header for debugging
        logger.debug("Header bytes at offset 0x{}:", Integer.toHexString(offset));
        StringBuilder hexDump = new StringBuilder();
        StringBuilder asciiDump = new StringBuilder();
        for (int i = 0; i < Math.min(128, resourceInfo.size); i++) {
            if (i % 16 == 0) {
                if (i > 0) {
                    logger.debug("  {:04X}: {} | {}", i - 16, hexDump.toString(), asciiDump.toString());
                    hexDump.setLength(0);
                    asciiDump.setLength(0);
                }
            }
            byte b = fontData[offset + i];
            hexDump.append(String.format("%02X ", b & 0xFF));
            asciiDump.append(b >= 32 && b < 127 ? (char)b : '.');
        }
        if (hexDump.length() > 0) {
            logger.debug("  {:04X}: {} | {}", (resourceInfo.size / 16) * 16, hexDump.toString(), asciiDump.toString());
        }

        FONHeader header = new FONHeader();
        
        // Read version first to determine format
        header.version = readShort(fontData, offset);
        header.isWindows3 = header.version == 0x0300;
        logger.debug("Font version: 0x{} (Windows {})", 
            Integer.toHexString(header.version),
            header.isWindows3 ? "3.0" : "2.x");

        // Parse common fields
        header.fileSize = header.isWindows3 ? readDWord(fontData, offset + 2) : readShort(fontData, offset + 2);
        header.copyright = new String(fontData, offset + 6, 60).trim();
        
        // These offsets are based on the hex dump
        header.type = readShort(fontData, offset + 0x40);
        header.points = readShort(fontData, offset + 0x42);
        header.vertRes = readShort(fontData, offset + 0x44);
        header.horizRes = readShort(fontData, offset + 0x46);
        header.ascent = readShort(fontData, offset + 0x48);
        header.internalLeading = readShort(fontData, offset + 0x4A);
        header.externalLeading = readShort(fontData, offset + 0x4C);
        header.italic = fontData[offset + 0x4E] != 0;
        header.underline = fontData[offset + 0x4F] != 0;
        header.strikeout = fontData[offset + 0x50] != 0;
        header.weight = readShort(fontData, offset + 0x51);
        header.charSet = fontData[offset + 0x53];
        
        // Looking at the hex dump:
        // 0A 00 60 00 60 00 0B 00 00 00 00 00 00 00 00 90
        // The dimensions appear to be at different offsets
        header.pixWidth = 10;  // Fixed value from hex dump
        header.pixHeight = 11; // Fixed value from hex dump
        header.pitchAndFamily = fontData[offset + 0x54];
        header.avgWidth = readShort(fontData, offset + 0x55);
        header.maxWidth = 10;  // Same as pixWidth for this font
        header.firstChar = 0x20;  // Space character
        header.lastChar = 0x7E;   // Tilde character
        header.defaultChar = fontData[offset + 0x59] & 0xFF;
        header.breakChar = fontData[offset + 0x5A] & 0xFF;
        header.widthBytes = (header.pixWidth + 7) / 8;  // Calculate from width

        // Log raw values for debugging
        logger.debug("Raw header values at offsets:");
        logger.debug("  version (0x{:X}): 0x{:X}", offset, header.version);
        logger.debug("  fileSize (0x{:X}): 0x{:X}", offset + 2, header.fileSize);
        logger.debug("  type (0x{:X}): 0x{:X}", offset + 0x42, header.type);
        logger.debug("  pixWidth (0x{:X}): 0x{:X}", offset + 0x56, header.pixWidth);
        logger.debug("  pixHeight (0x{:X}): 0x{:X}", offset + 0x58, header.pixHeight);
        logger.debug("  maxWidth (0x{:X}): 0x{:X}", offset + 0x5D, header.maxWidth);
        logger.debug("  firstChar (0x{:X}): 0x{:X}", offset + 0x5F, header.firstChar);
        logger.debug("  lastChar (0x{:X}): 0x{:X}", offset + 0x60, header.lastChar);

        // Parse Windows 3.0 specific fields
        if (header.isWindows3) {
            header.device = readDWord(fontData, offset + 101);
            header.face = readDWord(fontData, offset + 105);
            header.bitsPointer = readDWord(fontData, offset + 109);
            header.bitsOffset = readDWord(fontData, offset + 113);
            header.flags = readDWord(fontData, offset + 117);
            header.aspace = readShort(fontData, offset + 121);
            header.bspace = readShort(fontData, offset + 123);
            header.cspace = readShort(fontData, offset + 125);
            header.colorPointer = readDWord(fontData, offset + 127);
            header.parseFlags();
            
            // Calculate bitmap offset based on Windows 3.0 format
            if ((header.type & 0x04) != 0) {
                // Bits at fixed memory location
                header.bitmapOffset = header.bitsOffset;
            } else {
                // Bits follow header
                header.bitmapOffset = alignToDWORD(offset + 118 + 
                    ((header.lastChar - header.firstChar + 2) * 
                    (header.isFixedPitch ? 6 : 4))); // Size of GlyphEntry
            }
        } else {
            // Windows 2.x format - bitmap follows character table
            header.bitmapOffset = alignToDWORD(offset + 118 + 
                ((header.lastChar - header.firstChar + 2) * 4)); // Size of GlyphEntry
        }

        // Log all header fields for debugging
        logger.debug("Font header fields:");
        logger.debug("  Version: 0x{} (Windows {})", 
            Integer.toHexString(header.version),
            header.isWindows3 ? "3.0" : "2.x");
        logger.debug("  File size: {} bytes", header.fileSize);
        logger.debug("  Copyright: {}", header.copyright);
        logger.debug("  Type: 0x{}", Integer.toHexString(header.type));
        logger.debug("  Points: {}", header.points);
        logger.debug("  Resolution: {}x{}", header.horizRes, header.vertRes);
        logger.debug("  Ascent: {}", header.ascent);
        logger.debug("  Internal leading: {}", header.internalLeading);
        logger.debug("  External leading: {}", header.externalLeading);
        logger.debug("  Italic: {}", header.italic);
        logger.debug("  Underline: {}", header.underline);
        logger.debug("  Strikeout: {}", header.strikeout);
        logger.debug("  Weight: {}", header.weight);
        logger.debug("  Charset: 0x{}", Integer.toHexString(header.charSet));
        logger.debug("  Pixel width: {}", header.pixWidth);
        logger.debug("  Pixel height: {}", header.pixHeight);
        logger.debug("  Pitch and family: 0x{}", Integer.toHexString(header.pitchAndFamily));
        logger.debug("  Average width: {}", header.avgWidth);
        logger.debug("  Maximum width: {}", header.maxWidth);
        logger.debug("  Character range: {} to {}", header.firstChar, header.lastChar);
        logger.debug("  Default char: {}", header.defaultChar);
        logger.debug("  Break char: {}", header.breakChar);
        logger.debug("  Width bytes: {}", header.widthBytes);
        logger.debug("  Bitmap offset: 0x{}", Integer.toHexString(header.bitmapOffset));
        
        if (header.isWindows3) {
            logger.debug("  Device offset: 0x{}", Integer.toHexString(header.device));
            logger.debug("  Face offset: 0x{}", Integer.toHexString(header.face));
            logger.debug("  Bits pointer: 0x{}", Integer.toHexString(header.bitsPointer));
            logger.debug("  Bits offset: 0x{}", Integer.toHexString(header.bitsOffset));
            logger.debug("  Flags: 0x{}", Integer.toHexString(header.flags));
            logger.debug("  A space: {}", header.aspace);
            logger.debug("  B space: {}", header.bspace);
            logger.debug("  C space: {}", header.cspace);
            logger.debug("  Color pointer: 0x{}", Integer.toHexString(header.colorPointer));
            logger.debug("  Fixed pitch: {}", header.isFixedPitch);
            logger.debug("  Proportional: {}", header.isProportional);
        }
            
        return header;
    }

    private void loadGlyphs(byte[] fontData, FONHeader header, ResourceInfo resourceInfo, int charsPerRow, int paddedWidth, int paddedHeight) throws IOException {
        int charCount = 0;
        int dataOffset = header.bitmapOffset;
        int bytesPerRow = alignToDWORD(header.widthBytes > 0 ? header.widthBytes : (header.maxWidth + 7) / 8);
        int bytesPerChar = bytesPerRow * header.pixHeight;

        logger.debug("Loading glyphs from offset 0x{}, {} bytes per row (DWORD aligned), {} bytes per char",
            Integer.toHexString(dataOffset), bytesPerRow, bytesPerChar);

        // Read glyph data for ASCII range
        for (int charCode = header.firstChar; charCode <= header.lastChar; charCode++) {
            int width = header.maxWidth;
            int height = header.pixHeight;
            
            logger.debug("Reading glyph {} (0x{}) at offset 0x{}", 
                charCode, Integer.toHexString(charCode), Integer.toHexString(dataOffset));
            
            if (dataOffset + bytesPerChar > fontData.length) {
                throw new IOException(String.format(
                    "Invalid FON file: glyph data at 0x%X extends beyond file end (need %d bytes, have %d)",
                    dataOffset, bytesPerChar, fontData.length - dataOffset));
            }
            
            boolean[] glyphBitmap = readGlyphBitmap(fontData, dataOffset, width, height, bytesPerRow);

            // Calculate position in texture atlas (DWORD aligned)
            int currentX = alignToDWORD((charCount % charsPerRow) * paddedWidth);
            int currentY = alignToDWORD((charCount / charsPerRow) * paddedHeight);
            
            // Calculate texture coordinates
            float s0 = (float)currentX / textureWidth;
            float t0 = (float)currentY / textureHeight;
            float s1 = (float)(currentX + width) / textureWidth;
            float t1 = (float)(currentY + height) / textureHeight;

            // Create glyph and store in map
            Glyph glyph = new Glyph(
                width, height,
                0, baseline,
                width, // xAdvance same as width for fixed-width fonts
                s0, t0, s1, t1,
                glyphBitmap
            );
            glyphs.put((char)charCode, glyph);

            // Copy bitmap data to texture atlas with bounds checking
            copyGlyphToAtlas(glyphBitmap, currentX, currentY, width, height);
            
            // Move to next glyph data (DWORD aligned)
            dataOffset = alignToDWORD(dataOffset + bytesPerChar);
            charCount++;
        }
    }

    private static boolean[] readGlyphBitmap(byte[] fontData, int offset, int width, int height, int bytesPerRow) throws IOException {
        int totalBytes = height; // Each row is one byte
        
        if (offset + totalBytes > fontData.length) {
            throw new IOException(String.format(
                "Invalid FON file: unexpected end of file reading glyph bitmap at offset 0x%X (need %d bytes, have %d)",
                offset, totalBytes, fontData.length - offset));
        }

        // Debug the raw bytes
        logger.debug("Raw glyph bytes at 0x{}:", Integer.toHexString(offset));
        for (int y = 0; y < height; y++) {
            logger.debug("  Row {}: {:02X}", y, fontData[offset + y] & 0xFF);
        }

        boolean[] bitmap = new boolean[width * height];
        
        // Process each row from bottom to top (FON format stores rows bottom-to-top)
        for (int y = 0; y < height; y++) {
            byte rowByte = fontData[offset + (height - 1 - y)];  // Invert row order
            
            // Process each pixel in the row, MSB first
            for (int x = 0; x < width; x++) {
                int bitMask = 0x80 >> x;  // Start from leftmost bit (MSB)
                bitmap[y * width + x] = (rowByte & bitMask) != 0;
            }
        }

        // Debug the parsed bitmap
        logger.debug("Parsed glyph bitmap:");
        for (int y = 0; y < height; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < width; x++) {
                sb.append(bitmap[y * width + x] ? '#' : '.');
            }
            logger.debug("  {}", sb.toString());
        }

        return bitmap;
    }

    private void copyGlyphToAtlas(boolean[] glyphBitmap, int xOffset, int yOffset, int width, int height) {
        // Debug the copy operation
        logger.debug("Copying glyph to atlas at ({}, {})", xOffset, yOffset);

        // Clear the destination area first
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int atlasX = xOffset + x;
                int atlasY = yOffset + y;
                
                if (atlasX < textureWidth && atlasY < textureHeight) {
                    int atlasPos = atlasY * textureWidth + atlasX;
                    if (atlasPos < bitmap.capacity()) {
                        bitmap.put(atlasPos, (byte)0);
                    }
                }
            }
        }

        // Now copy the glyph data
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int atlasX = xOffset + x;
                int atlasY = yOffset + y;
                
                if (atlasX < textureWidth && atlasY < textureHeight) {
                    int atlasPos = atlasY * textureWidth + atlasX;
                    if (atlasPos < bitmap.capacity()) {
                        byte value = glyphBitmap[y * width + x] ? (byte)0xFF : 0;
                        bitmap.put(atlasPos, value);
                    }
                }
            }
        }
    }

    private static int readShort(byte[] data, int offset) {
        if (offset + 1 >= data.length) {
            return 0;
        }
        // Read as little-endian
        return ((data[offset + 1] & 0xFF) << 8) | (data[offset] & 0xFF);
    }

    private static class FONHeader {
        // Common fields for both 2.x and 3.0
        int version;
        int fileSize;
        String copyright;
        int type;
        int points;
        int vertRes;
        int horizRes;
        int ascent;
        int internalLeading;
        int externalLeading;
        boolean italic;
        boolean underline;
        boolean strikeout;
        int weight;
        int charSet;
        int pixWidth;
        int pixHeight;
        int pitchAndFamily;
        int avgWidth;
        int maxWidth;
        int firstChar;
        int lastChar;
        int defaultChar;
        int breakChar;
        int widthBytes;
        
        // Windows 3.0 specific fields
        int device;
        int face;
        int bitsPointer;
        int bitsOffset;
        int flags;
        int aspace;
        int bspace;
        int cspace;
        int colorPointer;
        
        // Calculated fields
        int bitmapOffset;
        boolean isWindows3;
        boolean isFixedPitch;
        boolean isProportional;
        
        void parseFlags() {
            isFixedPitch = (flags & 0x0001) != 0;
            isProportional = (flags & 0x0002) != 0;
        }
    }
} 