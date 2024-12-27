package com.ur91k.clichat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.*;

public class ServerConfig {
    private static final String CONFIG_FILE = "servers.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, ServerEntry> servers = new HashMap<>();

    public static class ServerEntry {
        private final String name;
        private final String ip;
        private final int port;
        private final long lastConnected;

        public ServerEntry(String name, String ip, int port) {
            this.name = name;
            this.ip = ip;
            this.port = port;
            this.lastConnected = System.currentTimeMillis();
        }

        public String getName() { return name; }
        public String getIp() { return ip; }
        public int getPort() { return port; }
        public long getLastConnected() { return lastConnected; }
    }

    public void addServer(String name, String ip, int port) {
        servers.put(name, new ServerEntry(name, ip, port));
        save();
    }

    public List<ServerEntry> getRecentServers() {
        return servers.values().stream()
            .sorted((a, b) -> Long.compare(b.getLastConnected(), a.getLastConnected()))
            .toList();
    }

    public Optional<ServerEntry> getServer(String name) {
        return Optional.ofNullable(servers.get(name));
    }

    public void load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                TypeToken<?> type = TypeToken.getParameterized(Map.class, String.class, ServerEntry.class);
                servers = GSON.fromJson(reader, type.getType());
                if (servers == null) {
                    servers = new HashMap<>();
                }
            } catch (IOException e) {
                System.err.println("Error loading server config: " + e.getMessage());
                servers = new HashMap<>();
            }
        }
    }

    private void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(servers, writer);
        } catch (IOException e) {
            System.err.println("Error saving server config: " + e.getMessage());
        }
    }
} 