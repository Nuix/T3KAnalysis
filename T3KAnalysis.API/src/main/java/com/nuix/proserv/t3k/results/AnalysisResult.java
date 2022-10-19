package com.nuix.proserv.t3k.results;

import com.google.gson.*;
import com.nuix.proserv.t3k.T3KApi;
import com.nuix.proserv.t3k.T3KApiException;
import com.nuix.proserv.t3k.detections.*;
import lombok.Getter;
import lombok.ToString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@ToString
public abstract class AnalysisResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class Deserializer implements JsonDeserializer<AnalysisResult> {

        private static Map<String, Object> jsonMetadataToMap(JsonObject metadataJsonObject) {
            return metadataJsonObject.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        public AnalysisResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            JsonElement metadataElement = jsonObject.get(METADATA);
            if(null == metadataElement) {
                LOG.error("The metadata for this result can not be read from the JSON. {}", json.toString());
                throw new T3KApiException(String.format(
                        "The metadata for this result can not be read. %s",
                        json.toString()
                ));
            }

            Map<String, Object> metadata = jsonMetadataToMap(metadataElement.getAsJsonObject());

            Class<? extends AnalysisResult> analysisResultImplementation = null;
            if (VideoResult.isVideoResult(metadata)) {
                analysisResultImplementation = VideoResult.class;
            } else if (ImageResult.isImageResults(metadata)) {
                analysisResultImplementation = ImageResult.class;
            } else if (DocumentResult.isDocumentResult(metadata)) {
                analysisResultImplementation = DocumentResult.class;
            } else {
                throw new T3KApiException(String.format(
                        "Unable to determine the type of this results. %s",
                        metadata
                ));
            }

            AnalysisResult result = context.deserialize(json, analysisResultImplementation);

            JsonObject detectionsList = jsonObject.getAsJsonObject(DETECTIONS);
            detectionsList.keySet().forEach(index -> {
                JsonElement detectionElement = detectionsList.get(index);
                Detection detection = context.deserialize(detectionElement, Detection.class);
                result.detections.add(detection);
            });

            return result;
        }
    }

    public static AnalysisResult parseResult(String jsonString) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(AnalysisResult.class, new Deserializer())
                .registerTypeAdapter(Detection.class, new DetectionTypeMap())
                .registerTypeAdapter(DetectionData.class, new DetectionDataDeserializer())
                .registerTypeAdapter(TextMatch.class, new TextMatch.Deserializer()).create();

        return gson.fromJson(jsonString, AnalysisResult.class);
    }

    protected static final Logger LOG = LogManager.getLogger(T3KApi.LOGGER_NAME);

    private static final String METADATA = "metadata";
    private static final String DETECTIONS = "detections";

    @Getter
    private transient List<Detection> detections = new LinkedList<>();

    protected AnalysisResult() {}

    public abstract ResultMetadata getMetadata();

    public int getDetectionCount() {
        return detections.size();
    }

    public void forEachDetection(Consumer<Detection> consumer) {
        detections.forEach(consumer);
    }
}
