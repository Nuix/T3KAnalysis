package com.nuix.proserv.t3k.detections;

import com.google.gson.*;
import lombok.Getter;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

public class TextDetection extends Detection {
    private static final long serialVersionUID = 1L;

    public static class Deserializer implements JsonDeserializer<TextDetection> {

        @Override
        public TextDetection deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonElement hit = jsonObject.get(TextDetection.HIT);
            TextDetection text = context.deserialize(hit, TextDetection.class);

            JsonElement matches = jsonObject.get(TextDetection.MATCHES);
            TextMatch[] textMatches = context.deserialize(matches, TextMatch[].class);
            text.matches = textMatches;

            return text;
        }
    }

    public static final String TYPE = "text";
    public static final String HIT = "hit";
    public static final String MATCHES = "matches";

    @Getter
    private String string;

    @Getter
    private String description;

    @Getter
    private String language;

    @Getter
    private boolean regex;

    @Getter
    private boolean fuzzy;

    @Getter
    private double minimal_levenshtein_ratio;

    private transient TextMatch[] matches;

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
                .append(" Found: ").append(string)
                .append(" Description: ").append(description)
                .append(" Language: ").append(language)
                .append(" MLR: ").append(minimal_levenshtein_ratio)
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
}
