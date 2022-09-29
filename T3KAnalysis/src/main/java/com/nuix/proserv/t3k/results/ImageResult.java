package com.nuix.proserv.t3k.results;

import java.util.Map;

public class ImageResult extends AnalysisResult implements HashedResult, RasteredResult, ResultWithNalvis {
    public static final String MODE = "mode";
    public static final String BYTES = "file_size";
    public static final String IMAGE_SIZE = "size";
    public static final String PHOTODNA = "photoDNA";

    // TODO implement isImageResults
    public static boolean isImageResults(Map<String, Object> resultsData) {
        return false;
    }
}
