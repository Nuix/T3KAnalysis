package com.nuix.proserv.t3k.detections;

import com.google.gson.*;
import lombok.Getter;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * Provides details about a match to a TextDetection.
 */
public class TextMatch implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class Deserializer implements JsonDeserializer<TextMatch> {

        @Override
        public TextMatch deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            JsonArray jsonArray = json.getAsJsonArray();

            TextMatch match = new TextMatch();
            match.matchedText = jsonArray.get(0).getAsString();
            match.startPage = jsonArray.get(1).getAsInt();
            match.startCharacter = jsonArray.get(2).getAsInt();
            match.startCharacterPosition = jsonArray.get(3).getAsDouble();
            match.endPage = jsonArray.get(4).getAsInt();
            match.endCharacter = jsonArray.get(5).getAsInt();
            match.endCharacterPosition = jsonArray.get(6).getAsDouble();

            return match;
        }
    }

    @Getter
    private String matchedText;

    @Getter
    private int startPage;

    @Getter
    private int endPage;

    @Getter
    private int startCharacter;

    @Getter
    private int endCharacter;

    @Getter
    private double startCharacterPosition;

    @Getter
    private double endCharacterPosition;

    protected TextMatch() {}

    @Override
    public String toString() {
        return String.format("%s [%d/%d -> %d/%d]",
                matchedText, startPage, startCharacter, endPage, endCharacter);
    }
}
