package com.nuix.proserv.t3k.ws;

import nuix.CustomMetadataMap;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The purpose of this class is to reduce the complexity of scripts used for displaying the results of the T3K Analysis.
 * <p>
 *     The results of the analysis are displayed in a format that gathers metadata results and displays several (but
 *     not all) results for each item, and the format for each type of result is slightly different.  As such the
 *     metadata profiles use scripted metadata to show the desired results.
 * </p>
 * <p>
 *     Storing, reading, and debuging scripted metadata can be difficult, so the amount of actual script code is
 *     minimized by moving the work into this class.  This class has two static methods that act as entry points:
 * </p>
 * <dl>
 *     <dt>{@link #displayPersonData(CustomMetadataMap)}</dt>
 *     <dd>
 *         Formats display for "person" type detections.  This will display the person's gender and age as well as the
 *         score.
 *     </dd>
 *     <dt>{@link #displayObjectData(CustomMetadataMap, String)}</dt>
 *     <dd>
 *         For other types of detections, the displayObjectData is used.  This will collect results from detections
 *         that match the specified string and proved their score (for object detections) or similarity (for curated
 *         content results).
 *     </dd>
 * </dl>
 * <p>
 *     An example scripted metadata using Ruby using this would be:
 * </p>
 * <pre>
 * java_import "com.nuix.proserv.t3k.ws.MetadataProfileBase"
 * MetadataProfileBase::display_object_data $current_item.custom_metadata, "army_tank"
 * </pre>
 * <p>
 *     This would collect all the detections on the item that are for "army_tank" and display up to 3 scores per item.
 * </p>
 */
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

    /**
     * Create a displayable string for a single detection.  The string will include data relevant to where the item
     * was found (frame number for videos, page and image number for documents...) if they are known or applicable.
     * @param contentFormat The format string for displaying the content.
     * @param contentValue The score, similarity, or other double numerical value to display
     * @param baseLabel The custom metadata label under which the information of interest is found
     * @param metadataMap The nuix.CustomMetadataMap with the custom metadata to display
     * @return A formatted string with the content to display
     */
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

    /**
     * Limit the display to up to 3 result detections per item, to prevent overly complex displays.  If there are
     * (possibly) more than 3 detections of this type, then "..." will be appended to indicate as much.
     * @param toDisplay The list of all formatted strings to display
     * @return A single string combining up to three results.
     */
    public static String finalizeOutput(List<String> toDisplay) {
        String output = toDisplay.stream().limit(RETURN_ITEM_COUNT).collect(Collectors.joining(","));

        if(toDisplay.size() >= RETURN_ITEM_COUNT) {
            output = output + "...";
        }

        return output;
    }

    /**
     * Calculate and display values of the desired detection type.
     * <p>
     *     If the detection is an Object, then up to 3 scores will be displayed.  If it is a Curated Content Result,
     *     then up to 3 similarity values are displayed.  This will add location information (such as video frame
     *     or document page and image) when appropriate.
     * </p>
     * @param metadataMap The nuix.CustomMetadataMap with the content to display
     * @param contentType The detection type to calculate the display for
     * @return A single string with 1 or more displayed results for the detection type found on the current item.
     */
    public static String displayObjectData(CustomMetadataMap metadataMap, String contentType) {
        int detectionCount = ((Number)metadataMap.getOrDefault(COUNT_LABEL, 0)).intValue();

        List<String> foundItems = new LinkedList<>();
        for(int i = 1; i <= detectionCount && i <= RETURN_ITEM_COUNT; i++) {
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

    /**
     * Calculate and display values for a person detection.
     * <p>
     *     This will display results of up to 3 'person' detections.  Each detection will include the gender, age, and
     *     score.
     * </p>
     * @param metadataMap The nuix.CustomMetadataMap with the content to display
     * @return A string with up to 3 Person detections found on the current item.
     */
    public static String displayPersonData(CustomMetadataMap metadataMap) {
        int detectionCount = ((Number)metadataMap.getOrDefault(COUNT_LABEL, 0)).intValue();

        List<String> foundItems = new LinkedList<>();
        for(int i = 1; i <= detectionCount && i <= RETURN_ITEM_COUNT; i++) {
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
