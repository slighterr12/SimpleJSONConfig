package com.twodevsstudio.simplejsonconfig.def.adapters;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public class InterfaceAdapter implements JsonSerializer, JsonDeserializer {
    
    private static final String CLASSNAME = "CLASSNAME";
    
    private static final String DATA = "DATA";
    
    public Object deserialize(@NotNull JsonElement jsonElement, Type type,
                              @NotNull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(CLASSNAME);
        String className = prim.getAsString();
        Class klass = getObjectClass(className);
        return jsonDeserializationContext.deserialize(jsonObject.get(DATA), klass);
    }
    
    public JsonElement serialize(@NotNull Object jsonElement, Type type, @NotNull JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(CLASSNAME, jsonElement.getClass().getName());
        jsonObject.add(DATA, jsonSerializationContext.serialize(jsonElement));
        return jsonObject;
    }
    
    private Class getObjectClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e.getMessage());
        }
    }
    
}

