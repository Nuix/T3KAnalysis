package com.nuix.proserv.t3k.detections;

import lombok.Getter;

import java.util.Map;

public class MD5Detection extends Detection {
    public static final String TYPE = "md5";
    public static final String HIT = "hit";
    public static final String HIT_TYPE = "type";
    public static final String HASH = "hash";
    public static final String DESCRIPTION = "description";
    public static final String HIT_METADATA = "metadata";
    public static final String HIT_ID = "id";

    @Getter
    private String hitType;

    @Getter
    private String hash;

    @Getter
    private String description;

    @Getter
    private int hitId;

    @Getter
    private Map<String, String> metadata;

    private MD5Detection() {}

    @Override
    public String toString() {
        return super.toString() +
                " Hit ID: " + hitId +
                " Hit Type: " + hitType +
                " Hash: " + hash +
                " Description: " + description +
                " Metadata: " + metadata;
    }

    public static boolean isMD5Detection(Map<String, Object> detectionData) {
        return TYPE.equals(detectionData.get(Detection.TYPE));
    }

    public static MD5Detection parseDetection(Map<String, Object> detectionData) {
        if(isMD5Detection(detectionData)) {
            MD5Detection detection = new MD5Detection();

            Map<String, Object> hitDefinition = (Map<String, Object>)detectionData.get(HIT);
            detection.hitId = (int)hitDefinition.get(HIT_ID);
            detection.hitType = (String)hitDefinition.get(HIT_TYPE);
            detection.description = (String)hitDefinition.get(DESCRIPTION);
            detection.hash = (String)hitDefinition.get(HASH);
            detection.metadata = (Map<String, String>)hitDefinition.getOrDefault(HIT_METADATA, Map.of());

            Detection.fillSharedValues(detection, detectionData);

            return detection;
        } else {
            return null;
        }
    }
}
