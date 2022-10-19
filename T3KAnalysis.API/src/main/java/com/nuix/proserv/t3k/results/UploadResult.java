package com.nuix.proserv.t3k.results;

import com.google.gson.*;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class UploadResult implements Serializable {

    public static class Deserializer implements JsonDeserializer<UploadResult> {
        private static final long serialVersionUID = 1L;

        @Override
        public UploadResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            Map<Long, String> idMap = new HashMap<>();
            jsonObject.keySet().forEach(id -> {
                Long thisId = Long.valueOf(id);
                String path = jsonObject.get(id).getAsString();
                idMap.put(thisId, path);
            });
            return new UploadResult(idMap);
        }
    }

    private Map<Long, String> idToAddressMap = new ConcurrentHashMap<>();

    protected UploadResult(Map<Long, String> idMap) {
        this.idToAddressMap.putAll(idMap);
    }

    public int getResultCount() {
        return idToAddressMap.size();
    }

    public void forEachId(Consumer<Long> consumer) {
        idToAddressMap.keySet().forEach(consumer);
    }

    public String get(long id) {
        return idToAddressMap.get(id);
    }
}
