package com.ur91k.clichat.terminal;

import com.ur91k.clichat.render.TextRenderer;
import com.ur91k.clichat.util.Logger;
import org.joml.Vector4f;
import static com.ur91k.clichat.render.ColorUtils.rgb;
import java.util.HashMap;
import java.util.Map;

/**
 * Debug terminal implementation with multiple views for debugging and diagnostics.
 */
public class DebugTerminal extends BaseTerminal {
    private static final Logger logger = Logger.getLogger(DebugTerminal.class);
    
    // View constants
    private enum DebugView {
        FONT_DISPLAY,    // Shows all characters in the loaded font
        SYSTEM_INFO,     // Shows system information
        RENDER_STATS     // Shows rendering statistics
    }
    
    // Colors
    private static final Vector4f HEADER_COLOR = rgb(0x71BD42);  // Green
    private static final Vector4f LABEL_COLOR = rgb(0xa09999);   // Grey
    private static final Vector4f VALUE_COLOR = rgb(0xFFFFFF);   // White
    private static final Vector4f GRID_COLOR = rgb(0x444444);    // Dark grey
    
    // Grid characters with fallbacks
    private char VERTICAL_LINE;   // Primary: '│', Fallback: '|'
    private char HORIZONTAL_LINE; // Primary: '─', Fallback: '-'
    private char CROSS;          // Primary: '┼', Fallback: '+'
    
    // Special display characters
    private static final String CTRL_CHAR = "^";  // Show ^ for control characters
    private static final String NO_GLYPH = " ";   // Show blank for missing glyphs
    
    // State
    private DebugView currentView = DebugView.FONT_DISPLAY;
    private int fontDisplayScrollOffset = 0;
    private long lastStatsUpdate = 0;
    private static final long STATS_UPDATE_INTERVAL = 1000; // ms
    
    // Dimension tracking for logging
    private int lastStartY = -1;
    private int lastVisibleRows = -1;
    
    // Text content tracking for logging
    private String lastHeaderText = "";
    private final Map<String, String> lastRenderedText = new HashMap<>();
    
    // Layout constants
    private static final int CHARS_PER_ROW = 16;
    private static final int HEADER_HEIGHT = 1;
    private static final int CELL_WIDTH = 4;  // Width of each character cell in the font display
    
    public DebugTerminal(TextRenderer textRenderer) {
        super(textRenderer);
        initializeGridChars();
        logger.debug("Created DebugTerminal with default width");
    }
    
    public DebugTerminal(TextRenderer textRenderer, int width) {
        super(textRenderer, width);
        initializeGridChars();
        logger.debug("Created DebugTerminal with width: {}", width);
    }
    
    private void initializeGridChars() {
        // Test if font has box drawing characters, use ASCII fallbacks if not
        if (textRenderer.hasGlyph('│')) {
            VERTICAL_LINE = '│';
            HORIZONTAL_LINE = '─';
            CROSS = '┼';
            logger.debug("Using Unicode box drawing characters for grid");
        } else {
            VERTICAL_LINE = '|';
            HORIZONTAL_LINE = '-';
            CROSS = '+';
            logger.debug("Using ASCII characters for grid");
        }
    }
    
    @Override
    public void handleResize(int width, int height) {
        logger.debug("Handling resize: {}x{}", width, height);
        totalHeight = (height - 2 * PADDING) / charHeight;
        messageAreaHeight = totalHeight - HEADER_HEIGHT;
        textRenderer.handleResize(width, height);
        logger.debug("New dimensions - totalHeight: {}, messageAreaHeight: {}", totalHeight, messageAreaHeight);
    }
    
    @Override
    public void render() {
        logger.trace("Rendering view: {}", currentView);
        
        // Clear the entire character grid
        for (int y = 0; y < totalHeight; y++) {
            for (int x = 0; x < width; x++) {
                chars[y][x] = ' ';
                fgColors[y][x] = new Vector4f(DEFAULT_FG);
                bgColors[y][x] = new Vector4f(DEFAULT_BG);
            }
        }
        
        // Render header with current view name and controls
        renderHeader();
        
        // Render current view
        switch (currentView) {
            case FONT_DISPLAY -> renderFontDisplay();
            case SYSTEM_INFO -> renderSystemInfo();
            case RENDER_STATS -> renderRenderStats();
        }
        
        // Actually render the character grid
        logger.trace("Rendering character grid to screen");
        for (int y = 0; y < totalHeight; y++) {
            for (int x = 0; x < width; x++) {
                if (chars[y][x] != ' ') {
                    textRenderer.renderText(
                        String.valueOf(chars[y][x]),
                        PADDING + (x * charWidth),
                        PADDING + (y * charHeight),
                        fgColors[y][x]
                    );
                }
            }
        }
    }
    
    private void renderHeader() {
        // Clear header line
        for (int x = 0; x < width; x++) {
            chars[0][x] = ' ';
            fgColors[0][x] = new Vector4f(HEADER_COLOR);
        }
        
        // Render view name and controls
        String header = String.format("Debug View: %s (1-3 to switch, ESC to exit)", currentView);
        if (!header.equals(lastHeaderText)) {
            logger.debug("Header text changed to: {}", header);
            lastHeaderText = header;
        }
        renderText(0, 0, header, HEADER_COLOR);
    }
    
    private void renderFontDisplay() {
        logger.trace("Rendering font display");
        int startY = HEADER_HEIGHT;
        int visibleRows = messageAreaHeight;
        
        if (lastStartY != startY || lastVisibleRows != visibleRows) {
            logger.debug("Font display dimensions - startY: {}, visibleRows: {}", startY, visibleRows);
            lastStartY = startY;
            lastVisibleRows = visibleRows;
        }
        
        // Render column headers
        String colHeaders = "   ";  // Offset for row headers
        for (int x = 0; x < CHARS_PER_ROW; x++) {
            colHeaders += String.format(" %02X ", x);
        }
        renderText(startY, colHeaders, LABEL_COLOR);
        startY++;
        
        // Render grid
        for (int row = 0; row < visibleRows - 1 && row < 16; row++) {
            // Row header
            String rowHeader = String.format("%02X:", row * CHARS_PER_ROW);
            renderText(startY + row, rowHeader, LABEL_COLOR);
            
            // Characters
            for (int col = 0; col < CHARS_PER_ROW; col++) {
                int charCode = row * CHARS_PER_ROW + col;
                int x = (col * CELL_WIDTH) + 3;  // +3 for row header width
                
                // Draw character
                char c = (char)charCode;
                String display;
                if (charCode < 32) {
                    display = CTRL_CHAR;  // Control character
                } else if (!textRenderer.hasGlyph(c)) {
                    display = NO_GLYPH;   // Missing glyph
                } else {
                    display = String.valueOf(c);  // Normal character
                }
                
                // Center the character or placeholder in its cell
                int cellStart = (col * CELL_WIDTH) + 3;  // +3 for row header width
                int displayOffset = (CELL_WIDTH - display.length()) / 2;
                
                // Draw the character or placeholder
                for (int i = 0; i < display.length(); i++) {
                    chars[startY + row][cellStart + displayOffset + i] = display.charAt(i);
                    fgColors[startY + row][cellStart + displayOffset + i] = new Vector4f(VALUE_COLOR);
                }
                
                // Draw cell borders
                if (col < CHARS_PER_ROW - 1) {
                    chars[startY + row][cellStart + CELL_WIDTH - 1] = VERTICAL_LINE;
                    fgColors[startY + row][cellStart + CELL_WIDTH - 1] = new Vector4f(GRID_COLOR);
                }
            }
            
            // Draw horizontal grid line
            if (row < visibleRows - 2) {
                for (int x = 3; x < width - 1; x++) {
                    chars[startY + row + 1][x] = (x % CELL_WIDTH == CELL_WIDTH - 1) ? CROSS : HORIZONTAL_LINE;
                    fgColors[startY + row + 1][x] = new Vector4f(GRID_COLOR);
                }
                startY++;  // Skip the grid line in next iteration
            }
        }
        logger.trace("Font display grid rendered");
    }
    
    private void renderSystemInfo() {
        // TODO: Implement system info view
        renderText(HEADER_HEIGHT, "System Info View - Not implemented yet", VALUE_COLOR);
    }
    
    private void renderRenderStats() {
        // TODO: Implement render stats view
        renderText(HEADER_HEIGHT, "Render Stats View - Not implemented yet", VALUE_COLOR);
    }
    
    private void renderText(int y, String text, Vector4f color) {
        String key = String.format("y=%d", y);
        if (!text.equals(lastRenderedText.get(key))) {
            logger.trace("Setting new text at y={}: {}", y, text);
            lastRenderedText.put(key, text);
        }
        
        int x = 0;
        for (char c : text.toCharArray()) {
            if (x >= width) {
                logger.debug("Text truncated at x={}, width={}", x, width);
                break;
            }
            chars[y][x] = c;
            fgColors[y][x] = new Vector4f(color);
            x++;
        }
    }
    
    private void renderText(int y, int x, String text, Vector4f color) {
        String key = String.format("x=%d,y=%d", x, y);
        if (!text.equals(lastRenderedText.get(key))) {
            logger.trace("Setting new text at x={}, y={}: {}", x, y, text);
            lastRenderedText.put(key, text);
        }
        
        int count = 0;
        for (int i = 0; i < text.length() && x + i < width; i++) {
            chars[y][x + i] = text.charAt(i);
            fgColors[y][x + i] = new Vector4f(color);
            count++;
        }
    }
    
    @Override
    public void handleCharacter(char c) {
        if (c >= '1' && c <= '3') {
            DebugView oldView = currentView;
            currentView = DebugView.values()[c - '1'];
            logger.debug("View switched from {} to {}", oldView, currentView);
        }
    }
    
    @Override
    public void addLine(String text) {
        // Debug terminal doesn't support adding lines
    }
    
    @Override
    public void addLine(String text, Vector4f color) {
        // Debug terminal doesn't support adding lines
    }
} 