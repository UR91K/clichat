package com.ur91k.clichat.render;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;

import com.ur91k.clichat.util.Logger;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class RenderSystem {
    private static final Logger logger = Logger.getLogger(RenderSystem.class);
    private final ShaderProgram shader;
    private final Matrix4f screenProjection;  // For screen-space rendering
    private int windowWidth;
    private int windowHeight;
    
    // Basic rendering resources
    private final int vao;
    private final int vbo;
    
    public RenderSystem(int windowWidth, int windowHeight) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        
        // Screen space projection (top-left origin)
        screenProjection = new Matrix4f().ortho(
            0, windowWidth,
            windowHeight, 0,  // Flip Y coordinates for screen space
            -1, 1
        );

        // Initialize shader
        ClassLoader classLoader = getClass().getClassLoader();
        shader = new ShaderProgram(
            classLoader.getResourceAsStream("shaders/basic_vertex.glsl"),
            classLoader.getResourceAsStream("shaders/basic_fragment.glsl")
        );

        // Create VAO/VBO for basic rendering
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 8 * Float.BYTES, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);

        // Verify VAO/VBO setup
        if (glGetError() != GL_NO_ERROR) {
            logger.error("Error during VAO/VBO setup");
        }
    }

    public void beginFrame() {
        glClear(GL_COLOR_BUFFER_BIT);
    }
    
    public void drawRect(Vector2f position, float width, float height, Vector4f color) {
        shader.use();
        
        // Enable blending
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Set uniforms
        shader.setMatrix4f("projection", screenProjection);
        shader.setMatrix4f("model", new Matrix4f());
        shader.setVector4f("color", color);
        
        // Draw rectangle
        FloatBuffer vertices = BufferUtils.createFloatBuffer(12);  // 6 vertices * 2 coordinates
        float[] rectVertices = new float[] {
            position.x, position.y,                    // Top left
            position.x + width, position.y,            // Top right
            position.x + width, position.y + height,   // Bottom right
            position.x, position.y,                    // Top left
            position.x + width, position.y + height,   // Bottom right
            position.x, position.y + height            // Bottom left
        };
        vertices.put(rectVertices).flip();
        
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        
        // Cleanup state
        glDisable(GL_BLEND);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void handleResize(int newWidth, int newHeight) {
        this.windowWidth = newWidth;
        this.windowHeight = newHeight;
        
        // Update screen space projection (top-left origin)
        screenProjection.identity().ortho(
            0, windowWidth,
            windowHeight, 0,  // Flip Y coordinates
            -1, 1
        );
        
        // Update viewport
        glViewport(0, 0, windowWidth, windowHeight);
    }
    
    public void cleanup() {
        shader.cleanup();
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
    
    public int getWindowWidth() {
        return windowWidth;
    }
    
    public int getWindowHeight() {
        return windowHeight;
    }
} 