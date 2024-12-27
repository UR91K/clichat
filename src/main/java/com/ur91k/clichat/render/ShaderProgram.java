package com.ur91k.clichat.render;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    private final int programId;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public ShaderProgram(InputStream vertexStream, InputStream fragmentStream) {
        if (vertexStream == null || fragmentStream == null) {
            throw new RuntimeException("Could not find shader files");
        }

        // Load and compile vertex shader
        String vertexSource = new BufferedReader(new InputStreamReader(vertexStream))
            .lines().collect(Collectors.joining("\n"));
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);
        checkShaderError(vertexShader, "vertex");

        // Load and compile fragment shader
        String fragmentSource = new BufferedReader(new InputStreamReader(fragmentStream))
            .lines().collect(Collectors.joining("\n"));
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);
        checkShaderError(fragmentShader, "fragment");

        // Create and link program
        programId = glCreateProgram();
        glAttachShader(programId, vertexShader);
        glAttachShader(programId, fragmentShader);
        glLinkProgram(programId);
        checkProgramError();

        // Clean up shaders
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    private void checkShaderError(int shaderId, String type) {
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            String log = glGetShaderInfoLog(shaderId);
            throw new RuntimeException("Failed to compile " + type + " shader: " + log);
        }
    }

    private void checkProgramError() {
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            String log = glGetProgramInfoLog(programId);
            throw new RuntimeException("Failed to link shader program: " + log);
        }
    }

    public void use() {
        glUseProgram(programId);
    }

    public void setMatrix4f(String name, Matrix4f matrix) {
        int location = glGetUniformLocation(programId, name);
        matrix.get(matrixBuffer);
        glUniformMatrix4fv(location, false, matrixBuffer);
    }

    public void setVector4f(String name, Vector4f vector) {
        int location = glGetUniformLocation(programId, name);
        glUniform4f(location, vector.x, vector.y, vector.z, vector.w);
    }

    public void setInt(String name, int value) {
        int location = glGetUniformLocation(programId, name);
        glUniform1i(location, value);
    }

    public void cleanup() {
        glDeleteProgram(programId);
    }

    public int getProgramId() {
        return programId;
    }
} 