package com.ur91k.clichat.render;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import com.ur91k.clichat.util.Logger;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class TextRenderer {
    private static final Logger logger = Logger.getLogger(TextRenderer.class);
    private final int vao;
    private final int vbo;
    private final ShaderProgram shader;
    private final Matrix4f projection;
    private final BDFFont font;
    private final int textureId;
    
    public TextRenderer(int windowWidth, int windowHeight) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            font = new BDFFont(classLoader.getResourceAsStream("fonts/spleen-8x16.bdf"));
            
            shader = new ShaderProgram(
                classLoader.getResourceAsStream("shaders/text_vertex.glsl"),
                classLoader.getResourceAsStream("shaders/text_fragment.glsl")
            );
            
            // Verify shader program
            int programId = shader.getProgramId();
            glValidateProgram(programId);
            if (glGetProgrami(programId, GL_VALIDATE_STATUS) != GL_TRUE) {
                String log = glGetProgramInfoLog(programId);
                logger.error("Shader program validation failed: {}", log);
            }
            
            projection = new Matrix4f().ortho(
                0, windowWidth,
                windowHeight, 0,
                -1, 1
            );
            
            // Create and setup texture
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            
            if (textureId == 0) {
                throw new RuntimeException("Failed to generate texture");
            }

            // Critical for proper font rendering
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            
            // Set texture parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            
            // Upload font bitmap
            ByteBuffer bitmap = font.getBitmap();
            logger.debug("Font texture size: {}x{}", font.getTextureWidth(), font.getTextureHeight());
            
            glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RED,
                font.getTextureWidth(),
                font.getTextureHeight(),
                0,
                GL_RED,
                GL_UNSIGNED_BYTE,
                bitmap
            );
            
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                throw new RuntimeException("OpenGL error after texture upload: " + error);
            }
            
            // Setup VAO/VBO
            vao = glGenVertexArrays();
            vbo = glGenBuffers();
            
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, 4 * 4 * Float.BYTES, GL_DYNAMIC_DRAW);
            
            // position (x,y) and texture coordinates (s,t)
            glVertexAttribPointer(0, 4, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0);
            
            // Verify VAO setup
            if (glGetVertexAttribi(0, GL_VERTEX_ATTRIB_ARRAY_ENABLED) != GL_TRUE) {
                logger.error("Vertex attribute 0 is not enabled!");
            }
            
            // Enable blending for text
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            
        } catch (Exception e) {
            logger.error("Failed to initialize text renderer", e);
            throw new RuntimeException("Failed to initialize text renderer", e);
        }
    }
    
    public void renderText(String text, float x, float y, Vector4f color) {
        if (text == null || text.isEmpty()) return;
        
        logger.trace("Rendering text: {}", text);
        
        if (shader == null) {
            logger.error("Shader is null");
            return;
        }
        
        // Save current OpenGL state
        int previousProgram = glGetInteger(GL_CURRENT_PROGRAM);
        boolean depthTestEnabled = glGetBoolean(GL_DEPTH_TEST);
        
        // Setup state for text rendering
        if (depthTestEnabled) glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        shader.use();
        shader.setMatrix4f("projection", projection);
        shader.setVector4f("color", color);
        shader.setInt("text", 0);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        
        FloatBuffer vertices = BufferUtils.createFloatBuffer(16); // 4 vertices * 4 components
        float xpos = x;
        
        for (char c : text.toCharArray()) {
            BDFFont.Glyph glyph = font.getGlyph(c);
            if (glyph == null) {
                logger.warn("No glyph found for character: {}", c);
                continue;
            }
            
            float x0 = xpos + glyph.xOffset;
            float y0 = y;
            float x1 = x0 + glyph.width;
            float y1 = y0 + glyph.height;
            
            vertices.clear();
            vertices.put(new float[] {
                x0, y0, glyph.s0, glyph.t0,
                x1, y0, glyph.s1, glyph.t0,
                x1, y1, glyph.s1, glyph.t1,
                x0, y1, glyph.s0, glyph.t1
            });
            vertices.flip();
            
            // Make sure we're not writing beyond buffer bounds
            if (vertices.remaining() * Float.BYTES > 4 * 4 * Float.BYTES) {
                logger.error("Attempting to write beyond buffer bounds");
                continue;
            }
            
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
            
            xpos += glyph.xAdvance;
        }
        
        // Restore previous OpenGL state
        glUseProgram(previousProgram);
        if (depthTestEnabled) glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    public void renderText(String text, Vector2f position, Vector4f color, float scale) {
        if (text == null || text.isEmpty()) return;
        
        shader.use();
        shader.setMatrix4f("projection", projection);
        shader.setVector4f("color", color);
        shader.setInt("text", 0);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        float xpos = position.x;
        float ypos = position.y;
        
        FloatBuffer vertices = BufferUtils.createFloatBuffer(16);
        
        for (char c : text.toCharArray()) {
            BDFFont.Glyph glyph = font.getGlyph(c);
            if (glyph == null) continue;
            
            float x0 = xpos + glyph.xOffset * scale;
            float y0 = ypos;
            float x1 = x0 + glyph.width * scale;
            float y1 = y0 + glyph.height * scale;
            
            vertices.clear();
            vertices.put(new float[] {
                x0, y0, glyph.s0, glyph.t0,
                x1, y0, glyph.s1, glyph.t0,
                x1, y1, glyph.s1, glyph.t1,
                x0, y1, glyph.s0, glyph.t1
            });
            vertices.flip();
            
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
            glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
            
            xpos += glyph.xAdvance * scale;
        }
        
        glDisable(GL_BLEND);
        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void renderScreenText(String text, Vector2f position, Vector4f color, float scale) {
        // Screen space coordinates are already in the correct space
        renderText(text, position, color, scale);
    }
    
    public void handleResize(int width, int height) {
        // Update projection matrix for new window dimensions
        projection.identity().ortho(0, width, height, 0, -1, 1);
        shader.use();
        shader.setMatrix4f("projection", projection);
    }
    
    public void cleanup() {
        shader.cleanup();
        glDeleteTextures(textureId);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
} 