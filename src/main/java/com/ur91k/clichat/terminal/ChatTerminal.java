package com.ur91k.clichat.terminal;

import com.ur91k.clichat.render.TextRenderer;
import org.joml.Vector4f;
import static com.ur91k.clichat.render.ColorUtils.rgb;
/**
 * Specialized terminal implementation for chat functionality with status bar and input line.
 */
public class ChatTerminal extends BaseTerminal {
    // Chat-specific constants
    private static final Vector4f CONNECTED_COLOR = rgb(0x71BD42);  // Green
    private static final Vector4f DISCONNECTED_COLOR = rgb(0xC43B4B);  // Red
    private static final Vector4f IP_COLOR = rgb(0xa09999);  // Grey
    private static final Vector4f TIMESTAMP_COLOR = IP_COLOR;  // Use same grey for timestamps
    
    // Layout constants
    private static final int STATUS_HEIGHT = 1;  // Status bar height
    private static final int BLANK_LINES = 1;    // Spacing between zones
    
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
    
    @SuppressWarnings("unused")
    private Vector4f usernameColor = new Vector4f(DEFAULT_FG);
    private static final String PROMPT_SUFFIX = " >> ";
    
    public ChatTerminal(TextRenderer textRenderer) {
        super(textRenderer);
        initializeChatState();
    }
    
    public ChatTerminal(TextRenderer textRenderer, int width) {
        super(textRenderer, width);
        initializeChatState();
    }
    
    private void initializeChatState() {
        inputBuffer = new StringBuilder();
        cursorX = 0;
        inputActive = true;
    }
    
    @Override
    public void handleResize(int width, int height) {
        totalHeight = (height - 2 * PADDING) / charHeight;
        messageAreaHeight = totalHeight - (2 * BLANK_LINES) - STATUS_HEIGHT - 1; // -1 for input line
        textRenderer.handleResize(width, height);
    }
    
    @Override
    public void render() {
        // Render status bar
        renderStatusBar();
        
        // Render blank line after status
        int blankLine1Y = (STATUS_HEIGHT) * charHeight + PADDING;
        renderBlankLine(blankLine1Y);
        
        // Calculate visible message range
        int visibleLines = Math.min(messageAreaHeight, currentLine);
        int startLine = Math.max(0, currentLine - visibleLines);
        
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
    
    @Override
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
    
    @Override
    public void addLine(String text) {
        addLine(text, null);
    }
    
    @Override
    public void addLine(String text, Vector4f color) {
        if (text.startsWith("[")) {
            // Message with timestamp
            int timestampEnd = text.indexOf("]") + 1;
            if (timestampEnd > 0) {
                // Move to next line
                currentLine++;
                
                // Clear the new line
                for (int x = 0; x < width; x++) {
                    chars[currentLine - 1][x] = ' ';
                    fgColors[currentLine - 1][x] = new Vector4f(DEFAULT_FG);
                    bgColors[currentLine - 1][x] = new Vector4f(DEFAULT_BG);
                }
                
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
                        // Username part with provided color
                        String username = remaining.substring(0, colonPos);
                        for (char c : username.toCharArray()) {
                            if (x >= width) break;
                            chars[currentLine - 1][x] = c;
                            fgColors[currentLine - 1][x] = color != null ? 
                                new Vector4f(color) : new Vector4f(DEFAULT_FG);
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
            super.addLine(text, color);
        }
    }
    
    // Chat-specific methods
    public void setStatus(String status) {
        this.statusText = status;
    }
    
    public void setRoomName(String name) {
        this.roomName = name;
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
        this.roomNameColor = color != null ? color : new Vector4f(DEFAULT_FG);
    }
} 