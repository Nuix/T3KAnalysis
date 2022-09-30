package com.nuix.proserv.t3k.detections;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

    @Getter
    private String text;

    @Getter
    private String description;

    @Getter
    private String language;

    @Getter
    private boolean regex;

    @Getter
    private boolean fuzzy;

    @Getter
    private double minimalLevenshteinRatio;

    private TextMatch[] matches;

    private TextDetection() {}

    public int getMatchesCount() {
        return matches.length;
    }

    public void forEachMatch(Consumer<TextMatch> consumer) {
        Arrays.stream(matches).forEach(consumer);
    }

    @Override
    public String toString() {
        final StringBuilder output = new StringBuilder(super.toString())
                .append(" Found: ").append(text)
                .append(" Description: ").append(description)
                .append(" Language: ").append(language)
                .append(" MLR: ").append(minimalLevenshteinRatio)
                .append(" Regex? ").append(regex)
                .append(" Fuzzy? ").append(fuzzy)
                .append(" Matches: [");

        forEachMatch((match) -> {
            output.append(match.toString()).append(", ");
        });

        output.delete(output.length() - 2, output.length() - 1);
        output.append("]");

        return output.toString();
    }

    public static boolean isTextDetection(Map<String, Object> detectionData) {
        return TYPE.equals(detectionData.get(Detection.TYPE));
    }

    public static TextDetection parseDetection(Map<String, Object> detectionData) {
        if(isTextDetection(detectionData)) {
            TextDetection detection = new TextDetection();

            Map<String, Object> hitData = (Map<String, Object>)detectionData.get(HIT);
            detection.text = (String)hitData.get(STRING);
            detection.description = (String)hitData.get(DESCRIPTION);
            detection.language = (String)hitData.getOrDefault(LANGUAGE, "");
            detection.minimalLevenshteinRatio = (double)hitData.getOrDefault(MLR, 0.0);
            detection.regex = (boolean)hitData.getOrDefault(IS_REGEX, false);
            detection.fuzzy = (boolean)hitData.getOrDefault(IS_FUZZY, false);

            Object[][] matches = (Object[][])hitData.getOrDefault(MATCHES, new Object[0][0]);
            List<TextMatch> matchList = new ArrayList<>();
            Arrays.stream(matches).forEach(match -> {
                TextMatch textMatch = TextMatch.parseTextMatch(match);
                matchList.add(textMatch);
            });
            detection.matches = matchList.toArray(new TextMatch[0]);

            Detection.fillSharedValues(detection, detectionData);
            return detection;
        } else {
            return null;
        }
    }
}
