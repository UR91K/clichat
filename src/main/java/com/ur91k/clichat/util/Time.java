package com.ur91k.clichat.util;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class Time {
    private static double lastFrameTime;
    private static double deltaTime;
    
    private static int frameCount = 0;
    private static double frameTimer = 0;
    private static int fps = 0;
    
    public static void init() {
        lastFrameTime = getCurrentTime();
    }
    
    public static void update() {
        double currentTime = getCurrentTime();
        deltaTime = currentTime - lastFrameTime;
        lastFrameTime = currentTime;
        
        // Update FPS counter
        frameCount++;
        frameTimer += deltaTime;
        if (frameTimer >= 1.0) {
            fps = frameCount;
            frameCount = 0;
            frameTimer -= 1.0;
        }
    }
    
    public static double getDeltaTime() {
        return deltaTime;
    }
    
    public static int getFPS() {
        return fps;
    }
    
    public static double getCurrentTime() {
        return glfwGetTime();
    }
} 