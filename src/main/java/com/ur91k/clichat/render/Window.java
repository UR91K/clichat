package com.ur91k.clichat.render;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private long windowHandle;
    private int width, height;
    private String title;
    private boolean resized;
    private ResizeCallback resizeCallback;

    public interface ResizeCallback {
        void onResize(int width, int height);
    }

    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.resized = false;
    }

    public void setResizeCallback(ResizeCallback callback) {
        this.resizeCallback = callback;
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        
        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        
        // Setup resize callback
        glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> {
            width = w;
            height = h;
            resized = true;
            if (resizeCallback != null) {
                resizeCallback.onResize(w, h);
            }
        });
        
        // Center window on screen
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(windowHandle, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            glfwSetWindowPos(
                windowHandle,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        
        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1); // Enable v-sync
        glfwShowWindow(windowHandle);
        
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }
    
    public void update() {
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }
    
    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }
    
    public void cleanup() {
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
    }
    
    public boolean isResized() {
        return resized;
    }
    
    public void setResized(boolean resized) {
        this.resized = resized;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public long getWindowHandle() {
        return windowHandle;
    }
    
    public long getHandle() {
        return windowHandle;
    }
} 