package com.nuix.proserv.t3k.detections;

import java.util.Map;

public class MD5Detection extends Detection {
    public static final String TYPE = "md5";
    public static final String HIT = "hit";
    public static final String HIT_TYPE = "type";
    public static final String HASH = "hash";
    public static final String DESCRIPTION = "description";
    public static final String HIT_METADATA = "metadata";
    public static final String HIT_ID = "id";

    // TODO implement isMD5Detection
    public static boolean isMD5Detection(Map<String, Object> detectionData) {
        return false;
    }
}
