package com.nuix.proserv.t3k.ws;

import nuix.CustomMetadataMap;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MetadataProfileBase {
    private static final String SCORE_FORMAT = "%.1f%%";
    private static final String SIMILARITY_FORMAT = "%.3f";
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
    private static final String SIMILARITY_LABEL_POSTFIX = "|Similarity";

    private static final int RETURN_ITEM_COUNT = 3;

    public static String formatContent(String contentFormat, double contentValue, String baseLabel, CustomMetadataMap metadataMap) {
        String pageLabel = baseLabel + PAGE_LABEL_POSTFIX;
        String imageLabel = baseLabel + IMAGE_LABEL_POSTFIX;
        String frameLabel = baseLabel + FRAME_LABEL_POSTFIX;

        String result = String.format(contentFormat, contentValue);
        if(metadataMap.containsKey(pageLabel)) {

            int page = ((Number)metadataMap.getOrDefault(pageLabel, 0)).intValue();
            int image = ((Number)metadataMap.getOrDefault(imageLabel, 0)).intValue();

            result = result + String.format(" (pg. %d, img. %d)", page, image);
        } else if (metadataMap.containsKey(frameLabel)) {
            int frame = ((Number)metadataMap.getOrDefault(frameLabel, 0)).intValue();

            result = result + String.format(" (frame %d)", frame);
        }

        return result;
    }

    public static String finalizeOutput(List<String> toDisplay) {
        String output = toDisplay.stream().limit(RETURN_ITEM_COUNT).collect(Collectors.joining(","));

        if(toDisplay.size() > RETURN_ITEM_COUNT) {
            output = output + "...";
        }

        return output;
    }

    public static String displayObjectData(CustomMetadataMap metadataMap, String contentType) {
        int detectionCount = ((Number)metadataMap.getOrDefault(COUNT_LABEL, 0)).intValue();

        List<String> foundItems = new LinkedList<>();
        for(int i = 1; i <= detectionCount; i++) {
            String detectionLabel = String.format(DETECTION_LABEL_FORMAT, i, contentType);
            String scoreLabel = detectionLabel + SCORE_LABEL_POSTFIX;
            String similarityLabel = detectionLabel + SIMILARITY_LABEL_POSTFIX;

            if(metadataMap.containsKey(scoreLabel)) {
                double score = ((Number)metadataMap.getOrDefault(scoreLabel, 0.0)).doubleValue();
                String store = formatContent(SCORE_FORMAT, score, detectionLabel, metadataMap);
                foundItems.add(store);
            } else if (metadataMap.containsKey(similarityLabel)) {
                double similarity = ((Number)metadataMap.getOrDefault(similarityLabel, 0.0)).doubleValue();
                String store = formatContent(SIMILARITY_FORMAT, similarity, detectionLabel, metadataMap);
                foundItems.add(store);
            }

        }

        return finalizeOutput(foundItems);
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

        return finalizeOutput(foundItems);

    }
}
