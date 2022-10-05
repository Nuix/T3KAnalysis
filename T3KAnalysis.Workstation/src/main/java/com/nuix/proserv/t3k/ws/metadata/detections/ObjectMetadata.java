package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.ObjectDetection;
import com.nuix.proserv.t3k.ws.metadata.T3KMetadata;
import nuix.CustomMetadataMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ObjectMetadata  extends DetectionMetadata implements MetadataWithScore {
    private static final String T3K_CLASSIFICATION = String.format(T3KMetadata.T3K_TEMPLATE, "%s", "%s");

    protected ObjectMetadata(CustomMetadataMap map) {
        super(map);
    }

    @Override
    public String getTypeTemplate(Detection detection) {
        if (detection instanceof ObjectDetection) {
            return String.format(T3K_CLASSIFICATION, "%d", ((ObjectDetection)detection).getClassification());
        } else {
            LOG.warn("The type of the detection is not an Object as expected, returning a generic template.");
            return String.format(T3K_CLASSIFICATION, "%d", detection.getType());
        }
    }

    @Override
    public void applyDetection(Detection detection, int detectionIndex) {
        if(! (detection instanceof ObjectDetection)) {
            LOG.error("The detection is not an object but is being treated as such.  Skipping it. {}", detection);
            return;
        }

        ObjectDetection object = (ObjectDetection)detection;
        String scoreLabel = String.format(T3K_SCORE, String.format(getTypeTemplate(detection), detectionIndex));
        getMetadataMap().putFloat(scoreLabel, object.getScore());

    }
}
