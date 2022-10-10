package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.TextDetection;
import nuix.CustomMetadataMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TextMetadata extends DetectionMetadata {
    private static final Logger LOG = LogManager.getLogger(TextMetadata.class.getCanonicalName());

    private static final String T3K_TEXT = String.format(T3K_TEMPLATE, "%d", "Text Hit");

    private static final String T3K_STRING = String.format("%s|%s", T3K_TEXT, "String");

    private static final String T3K_DESCRIPTION = String.format("%s|%s", T3K_TEXT, "Description");

    private static final String T3K_LANGUAGE = String.format("%s|%s", T3K_TEXT, "Language");

    private static final String T3K_REGEX = String.format("%s|%s", T3K_TEXT, "Regex Hit");

    private static final String T3K_FUZZY = String.format("%s|%s", T3K_TEXT, "Fuzzy Hit");

    private static final String T3K_MLR = String.format("%s|%s", T3K_TEXT, "Minimal Levenshtein Ratio");

    private static final String T3K_MATCH = String.format("%s|%s", T3K_TEXT, "Matches");

    private static final String T3K_MATCH_TEXT = String.format("%s|%s|%s", T3K_MATCH, "%d", "Text");

    private static final String T3K_MATCH_STARTPAGE = String.format("%s|%s|%s", T3K_MATCH, "%d", "Start Page");

    private static final String T3K_MATCH_ENDPAGE = String.format("%s|%s|%s", T3K_MATCH, "%d", "End Page");

    private static final String T3K_MATCH_STARTCHAR = String.format("%s|%s|%s", T3K_MATCH, "%d", "Start Character");

    private static final String T3K_MATCH_ENDCHAR = String.format("%s|%s|%s", T3K_MATCH, "%d", "End Character");

    private static final String T3K_MATCH_STARTPOS = String.format("%s|%s|%s", T3K_MATCH, "%d", "Start Position");

    private static final String T3K_MATCH_ENDPOS = String.format("%s|%s|%s", T3K_MATCH, "%d", "End Position");

    protected TextMetadata(CustomMetadataMap map) {
        super(map);
    }

    @Override
    public String getTypeTemplate(Detection detection) {
        return T3K_TEXT;
    }

    @Override
    public void applyDetection(Detection detection, int detectionIndex) {
        if(! (detection instanceof TextDetection)) {
            LOG.error("The detection is not a text hit but is being treated as such.  Skipping it. {}", detection);
            return;
        }

        TextDetection text = (TextDetection) detection;
        getMetadataMap().putText(String.format(T3K_STRING, detectionIndex), text.getString());
        getMetadataMap().putText(String.format(T3K_LANGUAGE, detectionIndex), text.getLanguage());
        getMetadataMap().putText(String.format(T3K_DESCRIPTION, detectionIndex), text.getDescription());
        getMetadataMap().putBoolean(String.format(T3K_REGEX, detectionIndex), text.isRegex());
        getMetadataMap().putBoolean(String.format(T3K_FUZZY, detectionIndex), text.isFuzzy());
        getMetadataMap().putFloat(String.format(T3K_MLR, detectionIndex), text.getMinimal_levenshtein_ratio());

        int[] matchIndex = { 0 };
        text.forEachMatch(textMatch -> {
            matchIndex[0] = matchIndex[0] + 1;

            getMetadataMap().putText(String.format(T3K_MATCH_TEXT, detectionIndex, matchIndex[0]), textMatch.getMatchedText());
            getMetadataMap().putInteger(String.format(T3K_MATCH_STARTPAGE, detectionIndex, matchIndex[0]), textMatch.getStartPage());
            getMetadataMap().putInteger(String.format(T3K_MATCH_STARTCHAR, detectionIndex, matchIndex[0]), textMatch.getStartCharacter());
            getMetadataMap().putFloat(String.format(T3K_MATCH_STARTPOS, detectionIndex, matchIndex[0]), textMatch.getStartCharacterPosition());
            getMetadataMap().putInteger(String.format(T3K_MATCH_ENDPAGE, detectionIndex, matchIndex[0]), textMatch.getEndPage());
            getMetadataMap().putInteger(String.format(T3K_MATCH_ENDCHAR, detectionIndex, matchIndex[0]), textMatch.getEndCharacter());
            getMetadataMap().putInteger(String.format(T3K_MATCH_ENDPOS, detectionIndex, matchIndex[0]), textMatch.getEndCharacterPosition());

        });
    }
}
