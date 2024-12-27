package com.ur91k.clichat.net;

import com.google.gson.*;
import org.joml.Vector4f;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageAdapter implements JsonSerializer<Message>, JsonDeserializer<Message> {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(Vector4f.class, new Vector4fAdapter())
        .create();

    @Override
    public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        
        // Required fields
        obj.addProperty("type", src.getType().name());
        obj.addProperty("senderId", src.getSenderId());
        obj.addProperty("senderName", src.getSenderName());
        obj.add("senderColor", gson.toJsonTree(src.getSenderColor()));
        obj.addProperty("content", src.getContent());
        obj.addProperty("timestamp", src.getTimestamp().format(DATE_FORMAT));
        
        // Optional fields
        if (src.getTargetId() != null) {
            obj.addProperty("targetId", src.getTargetId());
        }
        if (src.getOldValue() != null) {
            obj.addProperty("oldValue", src.getOldValue());
        }
        if (src.getNewValue() != null) {
            obj.addProperty("newValue", src.getNewValue());
        }
        
        return obj;
    }

    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        
        // Parse required fields
        Message.Type type = Message.Type.valueOf(obj.get("type").getAsString());
        String senderId = obj.get("senderId").getAsString();
        String senderName = obj.get("senderName").getAsString();
        Vector4f senderColor = gson.fromJson(obj.get("senderColor"), Vector4f.class);
        String content = obj.get("content").getAsString();
        
        // Create message
        Message message = new Message(type, senderId, senderName, senderColor, content);
        
        // Set timestamp if present
        if (obj.has("timestamp")) {
            LocalDateTime timestamp = LocalDateTime.parse(obj.get("timestamp").getAsString(), DATE_FORMAT);
            message.setTimestamp(timestamp);
        }
        
        // Set optional fields if present
        if (obj.has("targetId")) {
            message.setTargetId(obj.get("targetId").getAsString());
        }
        if (obj.has("oldValue")) {
            message.setOldValue(obj.get("oldValue").getAsString());
        }
        if (obj.has("newValue")) {
            message.setNewValue(obj.get("newValue").getAsString());
        }
        
        return message;
    }
} 