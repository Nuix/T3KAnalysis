package com.nuix.proserv.t3k.results;

import com.nuix.proserv.t3k.T3KApi;
import com.nuix.proserv.t3k.T3KApiException;
import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.DetectionTypeMap;
import com.nuix.proserv.t3k.detections.DetectionWithData;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AnalysisResult {
    protected static final Logger LOG = LogManager.getLogger(T3KApi.LOGGER_NAME);

    public static final String METADATA = "metadata";
    public static final String ID = "id";
    public static final String PATH = "file_path";
    public static final String DETECTIONS = "detections";

    @Getter
    private long id;

    @Getter
    private String file;

    private final List<Detection> detections = new ArrayList<>();

    protected AnalysisResult() {}

    @Override
    public String toString() {
        final StringBuilder output = new StringBuilder().append("Results: ")
                .append(id).append(" {").append(file).append("}  Detections: [");

        forEachDetection((detection) -> {
            output.append(detection.toString()).append(", ");
        });

        output.delete(output.length() - 2, output.length() - 1);
        output.append("]");

        return output.toString();
    }

    protected void addDetection(Map<String, Object> detectionData) {
        Detection detection = DetectionTypeMap.getDetection(detectionData);

        if (detection instanceof DetectionWithData) {
            addDataToDetection((DetectionWithData) detection, detectionData);
        }

        detections.add(detection);
    }

    protected abstract void addDataToDetection(DetectionWithData detection, Map<String, Object> detectionData);

    public int getDetectionCount() {
        return detections.size();
    }

    public void forEachDetection(Consumer<Detection> consumer) {
        detections.forEach(consumer);
    }

    public static AnalysisResult parseResult(Map<String, Object> resultData) {
        Map<String, Object> metadata = (Map<String,Object>) resultData.get(METADATA);

        AnalysisResult result = null;
        if (VideoResult.isVideoResult(metadata)) {
            result = VideoResult.parseResult(metadata);
        } else if (ImageResult.isImageResults(metadata)) {
            result = ImageResult.parseResult(metadata);
        } else if (DocumentResult.isDocumentResult(metadata)) {
            result = DocumentResult.parseResult(metadata);
        } else {
            throw new T3KApiException(String.format(
               "Unable to determine the type of this results. %s",
               metadata
            ));
        }

        final AnalysisResult resultRef = result;

        Map<String, Map<String, Object>> detectionsMap = (Map<String, Map<String, Object>>)resultData
                .getOrDefault(DETECTIONS, new HashMap<String, Map<String, Object>>());
        detectionsMap.forEach((key, detectionDefinition) -> {
            resultRef.addDetection(detectionDefinition);
        });

        return result;
    }

    protected static void fillSharedFields(AnalysisResult result, Map<String, Object> metadata) {
        result.id = (long)metadata.get(ID);
        result.file = (String) metadata.get(PATH);
    }
}
