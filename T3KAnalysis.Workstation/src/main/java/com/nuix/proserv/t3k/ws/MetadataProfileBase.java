package com.nuix.proserv.t3k.ws;

import nuix.CustomMetadataMap;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MetadataProfileBase {
    private static final String SCORE_FORMAT = "%.1f%%";
    private static final String AGE_GENDER_FORMAT = "%s: %d (" + SCORE_FORMAT + ")";
    private static final String T3K_LABEL = "T3K Detections";
    private static final String COUNT_LABEL = T3K_LABEL + "|Count";
    private static final String DETECTION_LABEL_FORMAT = T3K_LABEL + "|%d|%s";
    private static final String SCORE_LABEL_POSTFIX = "|Score";
    private static final String PAGE_LABEL_POSTFIX = "|Page";
    private static final String IMAGE_LABEL_POSTFIX = "|Image";
    private static final String FRAME_LABEL_POSTFIX = "|Frame";
    private static final String AGE_LABEL_POSTFIX = "|Age";
    private static final String GENDER_LABEL_POSTFIX = "|Gender";

    private static final int RETURN_ITEM_COUNT = 3;

    public static String displayObjectData(CustomMetadataMap metadataMap, String contentType) {
        int detectionCount = ((Number)metadataMap.getOrDefault(COUNT_LABEL, 0)).intValue();

        List<String> foundItems = new LinkedList<>();
        for(int i = 1; i <= detectionCount; i++) {
            String detectionLabel = String.format(DETECTION_LABEL_FORMAT, i, contentType);
            String scoreLabel = detectionLabel + SCORE_LABEL_POSTFIX;
            if(metadataMap.containsKey(scoreLabel)) {

                double score = ((Number)metadataMap.getOrDefault(scoreLabel, 0.0)).doubleValue();

                String pageLabel = detectionLabel + PAGE_LABEL_POSTFIX;
                String frameLabel = detectionLabel + FRAME_LABEL_POSTFIX;

                String store = "";
                if(metadataMap.containsKey(pageLabel)) {
                    String imageLabel = detectionLabel + IMAGE_LABEL_POSTFIX;

                    int page = ((Number)metadataMap.getOrDefault(pageLabel, 0)).intValue();
                    int image = ((Number)metadataMap.getOrDefault(imageLabel, 0)).intValue();

                    store = String.format(SCORE_FORMAT + " (pg. %d, img. %d)", score, page, image);
                } else if (metadataMap.containsKey(frameLabel)) {
                    int frame = ((Number)metadataMap.getOrDefault(frameLabel, 0)).intValue();

                    store = String.format(SCORE_FORMAT + " (frame %d)", score, frame);
                } else {
                    store = String.format(SCORE_FORMAT, score);
                }
                foundItems.add(store);
            }
        }

        String output = foundItems.stream().limit(RETURN_ITEM_COUNT).collect(Collectors.joining(","));

        if(foundItems.size() > RETURN_ITEM_COUNT) {
            output = output + "...";
        }

        return output;
    }

    public static String displayPersonData(CustomMetadataMap metadataMap) {
        int detectionCount = ((Number)metadataMap.getOrDefault(COUNT_LABEL, 0)).intValue();

        List<String> foundItems = new LinkedList<>();
        for(int i = 1; i <= detectionCount; i++) {
            String detectionLabel = String.format(DETECTION_LABEL_FORMAT, i, "Person");
            String scoreLabel = detectionLabel + SCORE_LABEL_POSTFIX;
            if(metadataMap.containsKey(scoreLabel)) {
                // This detection is found...
                double score = ((Number)metadataMap.getOrDefault(scoreLabel, 0.0)).doubleValue();

                String ageLabel = detectionLabel + AGE_LABEL_POSTFIX;
                String genderLabel = detectionLabel + GENDER_LABEL_POSTFIX;

                int age = ((Number)metadataMap.getOrDefault(ageLabel, 0)).intValue();
                String gender = metadataMap.getOrDefault(genderLabel, "none").toString();

                foundItems.add(String.format(AGE_GENDER_FORMAT, gender, age, score));
            }
        }

        String output = foundItems.stream().limit(RETURN_ITEM_COUNT).collect(Collectors.joining(","));
        if(foundItems.size() > RETURN_ITEM_COUNT) {
            output = output + "...";
        }

        return output;

    }
}
