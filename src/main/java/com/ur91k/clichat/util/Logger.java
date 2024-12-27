package com.ur91k.clichat.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * A flexible logging system that supports multiple log levels, colored output,
 * and per-class log level configuration. This logger provides formatted output
 * with timestamps, log levels, and class names.
 */
public class Logger {
    /**
     * Defines the available logging levels with associated priorities and ANSI colors.
     * Levels are ordered from lowest to highest priority:
     * TRACE (0) < DEBUG (1) < INFO (2) < WARN (3) < ERROR (4)
     */
    public enum Level {
        /** Finest level of detail, white color */
        TRACE(0, "TRACE", "\u001B[37m"),
        
        /** Debugging information, cyan color */
        DEBUG(1, "DEBUG", "\u001B[36m"),
        
        /** General information, green color */
        INFO(2, "INFO", "\u001B[32m"),
        
        /** Warning messages, yellow color */
        WARN(3, "WARN", "\u001B[33m"),
        
        /** Error conditions, red color */
        ERROR(4, "ERROR", "\u001B[31m");

        final int priority;
        final String name;
        final String color;

        Level(int priority, String name, String color) {
            this.priority = priority;
            this.name = name;
            this.color = color;
        }
    }

    /** ANSI escape code to reset text formatting */
    private static final String RESET = "\u001B[0m";
    
    /** Format for timestamp in log messages (HH:mm:ss.SSS) */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** Name of the logger, typically the simple class name */
    private final String name;
    
    /** Default minimum logging level for all loggers */
    private static Level globalMinimumLevel = Level.INFO;
    
    /** Map of class-specific logging levels */
    private static final Map<String, Level> classLevels = new HashMap<>();
    
    /** Flag to control colored output */
    private static boolean useColors = true;
    
    /** Flag to control timestamp display */
    private static boolean showTimestamp = true;

    /**
     * Private constructor to enforce usage of factory method.
     *
     * @param name The name of the logger instance
     */
    private Logger(String name) {
        this.name = name;
    }

    /**
     * Creates or retrieves a logger instance for the specified class.
     *
     * @param clazz The class to create the logger for
     * @return A new Logger instance named after the class
     */
    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz.getSimpleName());
    }

    /**
     * Checks if a given log level is enabled for this logger instance.
     *
     * @param level The level to check
     * @return true if the level should be logged, false otherwise
     */
    private boolean isLevelEnabled(Level level) {
        Level classLevel = classLevels.get(name);
        return classLevel != null ? level.priority >= classLevel.priority 
                                : level.priority >= globalMinimumLevel.priority;
    }

    /**
     * Sets a specific logging level for a class, overriding the global minimum level.
     *
     * @param clazz The class to set the level for
     * @param level The logging level to set
     */
    public static void setClassLevel(Class<?> clazz, Level level) {
        classLevels.put(clazz.getSimpleName(), level);
    }

    /**
     * Removes the class-specific logging level, reverting to the global minimum level.
     *
     * @param clazz The class to reset
     */
    public static void resetClassLevel(Class<?> clazz) {
        classLevels.remove(clazz.getSimpleName());
    }

    /**
     * Removes all class-specific logging levels, reverting all loggers to the global minimum level.
     */
    public static void resetAllClassLevels() {
        classLevels.clear();
    }

    /**
     * Logs a message at TRACE level with optional format arguments.
     *
     * @param message The message format string
     * @param args Arguments referenced by format specifiers in the message
     */
    public void trace(String message, Object... args) {
        if (isLevelEnabled(Level.TRACE)) {
            log(Level.TRACE, formatMessage(message, args));
        }
    }

    /**
     * Logs a message at DEBUG level with optional format arguments.
     *
     * @param message The message format string
     * @param args Arguments referenced by format specifiers in the message
     */
    public void debug(String message, Object... args) {
        if (isLevelEnabled(Level.DEBUG)) {
            log(Level.DEBUG, formatMessage(message, args));
        }
    }

    /**
     * Logs a message at INFO level with optional format arguments.
     *
     * @param message The message format string
     * @param args Arguments referenced by format specifiers in the message
     */
    public void info(String message, Object... args) {
        if (isLevelEnabled(Level.INFO)) {
            log(Level.INFO, formatMessage(message, args));
        }
    }

    /**
     * Logs a message at WARN level with optional format arguments.
     *
     * @param message The message format string
     * @param args Arguments referenced by format specifiers in the message
     */
    public void warn(String message, Object... args) {
        if (isLevelEnabled(Level.WARN)) {
            log(Level.WARN, formatMessage(message, args));
        }
    }

    /**
     * Logs a message at ERROR level with optional format arguments.
     *
     * @param message The message format string
     * @param args Arguments referenced by format specifiers in the message
     */
    public void error(String message, Object... args) {
        if (isLevelEnabled(Level.ERROR)) {
            log(Level.ERROR, formatMessage(message, args));
        }
    }

    /**
     * Logs a message and exception at ERROR level.
     *
     * @param message The error message
     * @param t The throwable to log
     */
    public void error(String message, Throwable t) {
        if (isLevelEnabled(Level.ERROR)) {
            log(Level.ERROR, message);
            t.printStackTrace();
        }
    }

    /**
     * Internal method to perform the actual logging.
     * Formats the message with timestamp, level, and logger name.
     *
     * @param level The level to log at
     * @param message The formatted message to log
     */
    private void log(Level level, String message) {
        StringBuilder builder = new StringBuilder();

        if (showTimestamp) {
            builder.append(TIME_FORMATTER.format(LocalDateTime.now()))
                    .append(" ");
        }

        if (useColors) {
            builder.append(level.color);
        }

        builder.append("[").append(level.name).append("]")
                .append("[").append(name).append("] ");

        if (useColors) {
            builder.append(RESET);
        }

        builder.append(message);

        if (level == Level.ERROR) {
            System.err.println(builder);
        } else {
            System.out.println(builder);
        }
    }

    /**
     * Formats a message by replacing {} placeholders with provided arguments.
     *
     * @param message The message template containing {} placeholders
     * @param args The arguments to insert into the template
     * @return The formatted message string
     */
    private String formatMessage(String message, Object... args) {
        StringBuilder builder = new StringBuilder(message);
        for (Object arg : args) {
            int idx = builder.indexOf("{}");
            if (idx != -1) {
                builder.replace(idx, idx + 2, String.valueOf(arg));
            }
        }
        return builder.toString();
    }

    /**
     * Sets the global minimum logging level for all loggers that don't have a class-specific level.
     *
     * @param level The new global minimum level
     */
    public static void setGlobalMinimumLevel(Level level) {
        globalMinimumLevel = level;
    }

    /**
     * Enables or disables colored output in log messages.
     *
     * @param enable true to enable colors, false to disable
     */
    public static void useColors(boolean enable) {
        useColors = enable;
    }

    /**
     * Enables or disables timestamp display in log messages.
     *
     * @param enable true to show timestamps, false to hide them
     */
    public static void showTimestamp(boolean enable) {
        showTimestamp = enable;
    }
} 