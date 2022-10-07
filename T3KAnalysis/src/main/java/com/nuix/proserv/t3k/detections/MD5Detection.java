package com.nuix.proserv.t3k.detections;

import lombok.Getter;

import java.util.Map;

public class MD5Detection extends Detection {
    private static final long serialVersionUID = 1L;

    public static final String TYPE = "md5";
    public static final String HIT = "hit";

    @Getter
    private String type;

    @Getter
    private String hash;

    @Getter
    private String description;

    @Getter
    private int id;

    @Getter
    private Map<String, String> metadata;

    private MD5Detection() {}

    @Override
    public String toString() {
        return super.toString() +
                " Hit ID: " + id +
                " Hit Type: " + type +
                " Hash: " + hash +
                " Description: " + description +
                " Metadata: " + metadata;
    }

    public static boolean isMD5Detection(Map<String, Object> detectionData) {
        return TYPE.equals(detectionData.get(Detection.TYPE));
    }
}
