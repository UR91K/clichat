package com.ur91k.clichat.app;

import com.ur91k.clichat.util.Logger;

public class ServerLauncher {
    private static final Logger logger = Logger.getLogger(ServerLauncher.class);
    
    public static void main(String[] args) {
        // Set up logging
        Logger.setGlobalMinimumLevel(Logger.Level.INFO);
        Logger.useColors(true);
        
        // Parse command line arguments
        String ip = "0.0.0.0";  // Default to all interfaces
        int port = 8887;        // Default port
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "--ip":
                    if (i + 1 < args.length) {
                        ip = args[++i];
                    }
                    break;
                case "--port":
                    if (i + 1 < args.length) {
                        try {
                            port = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            logger.error("Invalid port number: {}", args[i]);
                            System.exit(1);
                        }
                    }
                    break;
                case "--debug":
                    Logger.setGlobalMinimumLevel(Logger.Level.DEBUG);
                    break;
                case "--help":
                    printHelp();
                    System.exit(0);
                    break;
                case "--no-gui":
                    // TODO: Implement headless mode
                    logger.warn("Headless mode not implemented yet");
                    break;
                default:
                    logger.warn("Unknown argument: {}", args[i]);
                    break;
            }
        }
        
        // Start server
        logger.info("Starting server on {}:{}", ip, port);
        ServerApplication server = new ServerApplication(ip, port);
        server.run();
    }
    
    private static void printHelp() {
        System.out.println("CLIChat Server");
        System.out.println("Usage: java -jar clichat-server.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --ip <address>    IP address to bind to (default: 0.0.0.0)");
        System.out.println("  --port <port>     Port to listen on (default: 8887)");
        System.out.println("  --debug           Enable debug logging");
        System.out.println("  --no-gui          Run in headless mode (no terminal UI)");
        System.out.println("  --help            Show this help message");
    }
} 