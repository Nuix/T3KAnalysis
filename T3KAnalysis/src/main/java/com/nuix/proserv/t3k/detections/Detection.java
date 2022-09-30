package com.nuix.proserv.t3k.detections;

import lombok.Getter;

import java.util.Map;

public abstract class Detection {
    public static final String TYPE = "type";
    public static final String INFO = "info";

    @Getter
    private String type;

    @Getter
    private String info;

    protected Detection() {}

    @Override
    public String toString() {
        return String.format("Type: %s, Info: %s", type, info);
    }

    public static Detection parseDetection(Map<String, Object> detectionData) {

    }

    protected static void fillSharedValues(Detection detection, Map<String, Object> detectionData) {
        detection.type = (String)detectionData.get(TYPE);

        detection.info = (String)detectionData.getOrDefault(INFO, "");
    }
}
