package com.nuix.proserv.t3k.detections;

import java.util.Map;

public class ObjectDetection extends Detection implements DetectionWithData, DetectionWithLocation, DetectionWithScore {
    public static final String TYPE = "object";
    public static final String CLASSIFICATION = "class_name";

    // TODO implement isObjectDetection
    public static boolean isObjectDetection(Map<String, Object> detectionData) {
        return false;
    }
}
