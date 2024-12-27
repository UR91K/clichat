package com.ur91k.clichat.terminal;

import com.ur91k.clichat.render.TextRenderer;
import com.ur91k.clichat.render.ColorUtils;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * Manages a terminal-like display with distinct zones for status, messages, and input.
 */
public class Terminal {
    private static final int DEFAULT_WIDTH = 80;  // characters
    private static final Vector4f DEFAULT_FG = new Vector4f(0.8f, 0.8f, 0.8f, 1.0f);
    private static final Vector4f DEFAULT_BG = new Vector4f(0.1f, 0.1f, 0.1f, 1.0f);
    private static final Vector4f CONNECTED_COLOR = new Vector4f(0.2f, 0.8f, 0.2f, 1.0f);  // Green
    private static final Vector4f DISCONNECTED_COLOR = new Vector4f(0.8f, 0.2f, 0.2f, 1.0f);  // Red
    private static final Vector4f IP_COLOR = new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);  // Grey
    private static final Vector4f TIMESTAMP_COLOR = IP_COLOR;  // Use same grey for timestamps
    private static final int PADDING = 6;  // Minimal padding from window edges
    
    // Layout constants
    private static final int STATUS_HEIGHT = 1;  // Status bar height
    private static final int BLANK_LINES = 1;    // Spacing between zones
    
    private final TextRenderer textRenderer;
    private final int width;
    private final int charWidth = 8;  // Spleen font is 8x16
    private final int charHeight = 16;
    
    // Zone dimensions
    private int messageAreaHeight;  // Calculated based on window size
    private int totalHeight;        // Total visible height in characters
    
    // Grid storage
    private char[][] chars;
    private Vector4f[][] fgColors;
    private Vector4f[][] bgColors;
    private int currentLine = 0;    // Current line in message history
    
    // Status information
    private String statusText = "DISCONNECTED";
    private String ipAddress = "";
    private String roomName = "";
    private Vector4f roomNameColor = new Vector4f(0.6f, 0.8f, 1.0f, 1.0f);  // Default room color
    
    // Input state
    private StringBuilder inputBuffer;
    private int cursorX;
    private boolean inputActive;
    
    private String username = "";
    private Vector4f usernameColor = new Vector4f(DEFAULT_FG);
    private static final String PROMPT_SUFFIX = " >> ";
    
    public Terminal(TextRenderer textRenderer) {
        this(textRenderer, DEFAULT_WIDTH);
    }
    
    public Terminal(TextRenderer textRenderer, int width) {
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
        
        inputBuffer = new StringBuilder();
        cursorX = 0;
        inputActive = true;
    }
    
    public void handleResize(int width, int height) {
        // Calculate dimensions
        totalHeight = (height - 2 * PADDING) / charHeight;
        messageAreaHeight = totalHeight - (2 * BLANK_LINES) - STATUS_HEIGHT - 1; // -1 for input line
        textRenderer.handleResize(width, height);
    }
    
    public void render() {
        // Render status bar
        renderStatusBar();
        
        // Render blank line after status
        int blankLine1Y = (STATUS_HEIGHT) * charHeight + PADDING;
        renderBlankLine(blankLine1Y);
        
        // Calculate visible message range
        int visibleLines = Math.min(messageAreaHeight, currentLine);
        int startLine = Math.max(0, currentLine - visibleLines);
        int endLine = currentLine;
        
        // Render messages
        int messageStartY = (STATUS_HEIGHT + BLANK_LINES) * charHeight + PADDING;
        for (int i = 0; i < visibleLines; i++) {
            int bufferLine = startLine + i;
            int screenY = messageStartY + (i * charHeight);
            renderLine(bufferLine, screenY);
        }
        
        // Render blank line before input
        int blankLine2Y = (totalHeight - 2) * charHeight + PADDING;
        renderBlankLine(blankLine2Y);
        
        // Render input line
        int inputY = (totalHeight - 1) * charHeight + PADDING;
        renderInputLine(inputY);
    }
    
    private void renderStatusBar() {
        int x = PADDING;
        
        // Render connection status
        Vector4f statusColor = statusText.equals("CONNECTED") ? CONNECTED_COLOR : DISCONNECTED_COLOR;
        textRenderer.renderText(statusText, x, PADDING, statusColor);
        x += statusText.length() * charWidth;
        
        // Render IP if connected
        if (!ipAddress.isEmpty()) {
            textRenderer.renderText(": ", x, PADDING, DEFAULT_FG);
            x += 2 * charWidth;
            textRenderer.renderText(ipAddress, x, PADDING, IP_COLOR);
            x += ipAddress.length() * charWidth;
        }
        
        // Render room name if set
        if (!roomName.isEmpty()) {
            textRenderer.renderText(" ", x, PADDING, DEFAULT_FG);
            x += charWidth;
            textRenderer.renderText(roomName, x, PADDING, roomNameColor);
        }
    }
    
    private void renderBlankLine(int y) {
        textRenderer.renderText(
            " ".repeat(width),
            PADDING,
            y,
            DEFAULT_FG
        );
    }
    
    private void renderLine(int bufferLine, int screenY) {
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
    
    private void renderInputLine(int y) {
        String prompt = username + PROMPT_SUFFIX;
        textRenderer.renderText(
            prompt + inputBuffer.toString(),
            PADDING,
            y,
            DEFAULT_FG
        );
        
        // Update cursor position to account for prompt
        if (inputActive) {
            int cursorScreenX = PADDING + ((prompt.length() + cursorX) * charWidth);
            textRenderer.renderText(
                "_",
                cursorScreenX,
                y,
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f)
            );
        }
    }
    
    public void setStatus(String status) {
        this.statusText = status;
    }
    
    public void setRoomName(String name) {
        this.roomName = name;
    }
    
    public void handleCharacter(char c) {
        if (!inputActive) return;
        
        if (c == '\b') {  // Backspace
            if (cursorX > 0) {
                inputBuffer.deleteCharAt(cursorX - 1);
                cursorX--;
                updateInputLine();
            }
        } else if (c == '\n' || c == '\r') {  // Enter
            String input = inputBuffer.toString().trim();
            if (!input.isEmpty()) {
                addLine(input);  // Echo input to terminal
                inputBuffer.setLength(0);
                cursorX = 0;
                updateInputLine();
            }
        } else if (Character.isISOControl(c)) {
            // Ignore other control characters
            return;
        } else if (cursorX < width - 1) {  // Regular character
            inputBuffer.insert(cursorX, c);
            cursorX++;
            updateInputLine();
        }
    }
    
    private void updateInputLine() {
        // Clear input line
        for (int x = 0; x < width; x++) {
            chars[currentLine][x] = ' ';
        }
        
        // Update with current input
        String input = inputBuffer.toString();
        for (int i = 0; i < input.length() && i < width; i++) {
            chars[currentLine][i] = input.charAt(i);
        }
    }
    
    public void addLine(String text) {
        // Move to next line
        currentLine++;
        
        // Clear the new line
        for (int x = 0; x < width; x++) {
            chars[currentLine - 1][x] = ' ';
            fgColors[currentLine - 1][x] = new Vector4f(DEFAULT_FG);
            bgColors[currentLine - 1][x] = new Vector4f(DEFAULT_BG);
        }
        
        // Parse and add text with colors
        if (text.startsWith("[")) {
            // Message with timestamp
            int timestampEnd = text.indexOf("]") + 1;
            if (timestampEnd > 0) {
                // Add timestamp in grey
                int x = 0;
                for (int i = 0; i < timestampEnd; i++) {
                    chars[currentLine - 1][x] = text.charAt(i);
                    fgColors[currentLine - 1][x] = new Vector4f(TIMESTAMP_COLOR);
                    x++;
                }
                
                // Add space
                chars[currentLine - 1][x] = ' ';
                fgColors[currentLine - 1][x] = new Vector4f(DEFAULT_FG);
                x++;
                
                // Check for system message
                String remaining = text.substring(timestampEnd + 1);
                if (remaining.startsWith("*")) {
                    // System message in default color
                    for (char c : remaining.toCharArray()) {
                        if (x >= width) break;
                        chars[currentLine - 1][x] = c;
                        fgColors[currentLine - 1][x] = new Vector4f(DEFAULT_FG);
                        x++;
                    }
                } else {
                    // User message, color the username
                    int colonPos = remaining.indexOf(":");
                    if (colonPos > 0) {
                        // Username part
                        String username = remaining.substring(0, colonPos);
                        for (char c : username.toCharArray()) {
                            if (x >= width) break;
                            chars[currentLine - 1][x] = c;
                            fgColors[currentLine - 1][x] = new Vector4f(usernameColor);
                            x++;
                        }
                        
                        // Colon and rest of message
                        String message = remaining.substring(colonPos);
                        for (char c : message.toCharArray()) {
                            if (x >= width) break;
                            chars[currentLine - 1][x] = c;
                            fgColors[currentLine - 1][x] = new Vector4f(DEFAULT_FG);
                            x++;
                        }
                    }
                }
            }
        } else {
            // Plain text without timestamp
            int x = 0;
            for (char c : text.toCharArray()) {
                if (x >= width) break;
                chars[currentLine - 1][x] = c;
                x++;
            }
        }
    }
    
    public void setInputActive(boolean active) {
        this.inputActive = active;
    }
    
    public boolean isInputActive() {
        return inputActive;
    }
    
    public String getCurrentInput() {
        return inputBuffer.toString();
    }
    
    public void clearInput() {
        inputBuffer.setLength(0);
        cursorX = 0;
        updateInputLine();
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setUsernameColor(Vector4f color) {
        this.usernameColor = new Vector4f(color);
    }
    
    public void setConnectionInfo(String status, String ip) {
        this.statusText = status;
        this.ipAddress = ip;
    }
    
    public void setRoomInfo(String name, Vector4f color) {
        this.roomName = name;
        this.roomNameColor = color;
    }
    
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
} 