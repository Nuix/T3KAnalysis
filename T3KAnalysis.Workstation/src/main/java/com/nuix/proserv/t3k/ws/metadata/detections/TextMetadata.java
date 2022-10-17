package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.TextDetection;
import nuix.CustomMetadataMap;

/**
 * Translate {@link TextDetection} into {@link CustomMetadataMap}
 * <p>
 *     A Text Detection comes from matching the source with predefined text.  The metadata provides information about
 *     the search term that was matched as well as the list of matches to the search term.
 * </p>
 */
public class TextMetadata extends DetectionMetadata {

    /**
     * The Text hit detection custom metadata label.  This is a Format String with a single numerical field with for the
     * detection's index.
     */
    private static final String T3K_TEXT = T3K_TEMPLATE + "Text Hit";

    /**
     * This is the string the text detection matches.
     */
    private static final String T3K_STRING = METADATA_LEVEL_SEPARATOR + "String";

    /**
     * This is the description of the string the text detection matches.
     */
    private static final String T3K_DESCRIPTION = METADATA_LEVEL_SEPARATOR + "Description";

    /**
     * This is the language of the string the text detection matches.
     */
    private static final String T3K_LANGUAGE = METADATA_LEVEL_SEPARATOR + "Language";

    /**
     * This is if the search string that this text detection matches was a REGEX search.
     */
    private static final String T3K_REGEX = METADATA_LEVEL_SEPARATOR + "Regex Hit";

    /**
     * This is if the search string that this detection matches was a Fuzzy search.
     */
    private static final String T3K_FUZZY = METADATA_LEVEL_SEPARATOR + "Fuzzy Hit";

    /**
     * This is the distance of the matched text to the search text (the number of character replacements needed to get
     * from the search to the actual).  It applies to fuzzy searched.
     */
    private static final String T3K_MLR = METADATA_LEVEL_SEPARATOR + "Minimal Levenshtein Ratio";

    /**
     * The detection covers one or more portions of text that are found to match the defined search term.  Ultimately
     * these will be displayed as a series below the text detection.  This is the parent to the matched list.
     */
    private static final String T3K_MATCH = METADATA_LEVEL_SEPARATOR + "Matches";

    /**
     * The text detection will match one or more portion of the document.  This is the count of the items that
     * were found.
     */
    private static final String T3K_MATCH_COUNT = T3K_MATCH + METADATA_LEVEL_SEPARATOR + "Count";

    /**
     * Each match to the search term will have a list of matches that will appear under the matches custom metadata
     * field.  Append this to the matches label to form a Format String with a placeholder in for the index into the
     * list for a match.
     */
    private static final String T3K_MATCH_INDEX = T3K_MATCH + METADATA_LEVEL_SEPARATOR + "%d" + METADATA_LEVEL_SEPARATOR;

    /**
     * Each match to the search term will have the text in the document that was found to match the search term.
     */
    private static final String T3K_MATCH_TEXT = "Text";

    /**
     * Each match to the search term will define the page where the beginning of the matched text was found.
     */
    private static final String T3K_MATCH_STARTPAGE = "Start Page";

    /**
     * Each match to the search term will define the page where the end of the matched text was found.
     */
    private static final String T3K_MATCH_ENDPAGE = "End Page";

    /**
     * Each match to the search term will define the character on the page where the matched text started.
     */
    private static final String T3K_MATCH_STARTCHAR = "Start Character";

    /**
     * Each match to the search term will define the character on the page where the matched text ended.
     */
    private static final String T3K_MATCH_ENDCHAR = "End Character";

    /**
     * Each match to the search tem will define the fraction (as character of the start / total characters on page) of
     * the page's content where the text started.
     */
    private static final String T3K_MATCH_STARTPOS = "Start Position";

    /**
     * Each match to the search tem will define the fraction (as character of the end / total characters on page) of
     * the page's content where the text ended.
     */
    private static final String T3K_MATCH_ENDPOS = "End Position";

    /**
     * Create a new TextMetadata object with the {@link CustomMetadataMap} used to store data.
     * @param map The {@link CustomMetadataMap} to store data in
     */
    protected TextMetadata(CustomMetadataMap map) {
        super(map);
    }

    /**
     * {@inheritDoc}
     * @param detection The {@link Detection} to generate a custom metadata label for.
     * @return A Format String for the Text Hit with a placeholder for this detection's index.
     */
    @Override
    public String getTypeTemplate(Detection detection) {
        return T3K_TEXT;
    }

    /**
     * {@inheritDoc}
     * @param detection The {@link Detection} with the data to add to the custom metadata
     * @param detectionIndex The index in the detections list for this detection
     */
    @Override
    public void applyDetection(Detection detection, int detectionIndex) {
        if(! (detection instanceof TextDetection)) {
            LOG.error("The detection is not a text hit but is being treated as such.  Skipping it. {}", detection);
            return;
        }

        TextDetection text = (TextDetection) detection;
        String baseLabel = String.format(T3K_TEXT, detectionIndex);
        getMetadataMap().putText(baseLabel + T3K_STRING, text.getString());
        getMetadataMap().putText(baseLabel + T3K_LANGUAGE, text.getLanguage());
        getMetadataMap().putText(baseLabel + T3K_DESCRIPTION, text.getDescription());
        getMetadataMap().putBoolean(baseLabel + T3K_REGEX, text.isRegex());
        getMetadataMap().putBoolean(baseLabel + T3K_FUZZY, text.isFuzzy());
        getMetadataMap().putFloat(baseLabel + T3K_MLR, text.getMinimal_levenshtein_ratio());

        getMetadataMap().putInteger(baseLabel + T3K_MATCH_COUNT, text.getMatchesCount());

        int[] matchIndex = { 0 };
        text.forEachMatch(textMatch -> {
            matchIndex[0] = matchIndex[0] + 1;

            String matchBaseLabel = String.format(baseLabel + T3K_MATCH_INDEX, matchIndex[0]);
            getMetadataMap().putText(matchBaseLabel + T3K_MATCH_TEXT, textMatch.getMatchedText());
            getMetadataMap().putInteger(matchBaseLabel + T3K_MATCH_STARTPAGE, textMatch.getStartPage());
            getMetadataMap().putInteger(matchBaseLabel + T3K_MATCH_STARTCHAR, textMatch.getStartCharacter());
            getMetadataMap().putFloat(matchBaseLabel + T3K_MATCH_STARTPOS, textMatch.getStartCharacterPosition());
            getMetadataMap().putInteger(matchBaseLabel + T3K_MATCH_ENDPAGE, textMatch.getEndPage());
            getMetadataMap().putInteger(matchBaseLabel + T3K_MATCH_ENDCHAR, textMatch.getEndCharacter());
            getMetadataMap().putInteger(matchBaseLabel + T3K_MATCH_ENDPOS, textMatch.getEndCharacterPosition());

        });
    }
}
