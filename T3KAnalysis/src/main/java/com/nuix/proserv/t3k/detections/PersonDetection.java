package com.nuix.proserv.t3k.detections;

import java.util.Map;

public class PersonDetection extends Detection implements DetectionWithData, DetectionWithLocation {
    public static final String TYPE = "age/gender";
    public static final String AGE = "age";
    public static final String GENDER = "gender";
    public static final String SYMBOL = "gender_string";

    //TODO implement isPersonDetection
    public static boolean isPersonDetection(Map<String, Object> detectionData) {
        return false;
    }
}
