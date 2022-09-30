package com.nuix.proserv.t3k.detections;

import lombok.Getter;

public class TextMatch {
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

    private TextMatch() {}

    @Override
    public String toString() {
        return String.format("%s [%d/%d -> %d/%d]",
                matchedText, startPage, startCharacter, endPage, endCharacter);
    }

    public static TextMatch parseTextMatch(Object[] matchData) {
        TextMatch match = new TextMatch();
        match.matchedText = (String)matchData[0];
        match.startPage = (int)matchData[1];
        match.startCharacter = (int)matchData[2];
        match.startCharacterPosition = (double)matchData[3];
        match.endPage = (int)matchData[4];
        match.endCharacter = (int)matchData[5];
        match.endCharacterPosition = (double)matchData[6];

        return match;
    }
}
