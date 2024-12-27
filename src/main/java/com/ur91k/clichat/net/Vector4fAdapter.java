package com.ur91k.clichat.net;

import com.google.gson.*;
import org.joml.Vector4f;
import java.lang.reflect.Type;

public class Vector4fAdapter implements JsonSerializer<Vector4f>, JsonDeserializer<Vector4f> {
    @Override
    public JsonElement serialize(Vector4f src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("x", src.x);
        obj.addProperty("y", src.y);
        obj.addProperty("z", src.z);
        obj.addProperty("w", src.w);
        return obj;
    }

    @Override
    public Vector4f deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        return new Vector4f(
            obj.get("x").getAsFloat(),
            obj.get("y").getAsFloat(),
            obj.get("z").getAsFloat(),
            obj.get("w").getAsFloat()
        );
    }
} 