package com.ur91k.clichat.terminal;

import com.ur91k.clichat.render.TextRenderer;
import org.joml.Vector4f;

/**
 * Simplified terminal implementation for debugging purposes.
 * Provides basic text output without chat-specific features.
 */
public class DebugTerminal extends BaseTerminal {
    private final String title;
    private static final Vector4f TITLE_COLOR = new Vector4f(0.6f, 0.8f, 1.0f, 1.0f);
    
    public DebugTerminal(TextRenderer textRenderer, String title) {
        super(textRenderer);
        this.title = title;
    }
    
    public DebugTerminal(TextRenderer textRenderer, int width, String title) {
        super(textRenderer, width);
        this.title = title;
    }
    
    @Override
    public void render() {
        // Render title
        textRenderer.renderText(title, PADDING, PADDING, TITLE_COLOR);
        
        // Calculate visible message range
        int visibleLines = Math.min(messageAreaHeight - 2, currentLine); // -2 for title and spacing
        int startLine = Math.max(0, currentLine - visibleLines);
        
        // Render messages
        int messageStartY = 2 * charHeight + PADDING; // Start after title and blank line
        for (int i = 0; i < visibleLines; i++) {
            int bufferLine = startLine + i;
            int screenY = messageStartY + (i * charHeight);
            renderLine(bufferLine, screenY);
        }
    }
    
    @Override
    public void handleResize(int width, int height) {
        super.handleResize(width, height);
        messageAreaHeight = totalHeight - 2; // Reserve space for title and spacing
    }
} 