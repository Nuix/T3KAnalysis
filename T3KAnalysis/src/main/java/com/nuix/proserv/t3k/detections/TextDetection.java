package com.nuix.proserv.t3k.detections;

import java.util.Map;

public class TextDetection extends Detection {
    public static final String TYPE = "text";
    public static final String HIT = "hit";
    public static final String STRING = "string";
    public static final String DESCRIPTION = "description";
    public static final String LANGUAGE = "language";
    public static final String IS_REGEX = "regex";
    public static final String IS_FUZZY = "fuzzy";
    public static final String MLR = "minimal_levenshtein_ratio";
    public static final String MATCHES = "matches";

    //TODO implement isTextDetection
    public static boolean isTextDetection(Map<String, Object> detectionData) {
        return false;
    }
}
