package com.nuix.proserv.t3k.ws.metadataprofile;

import lombok.*;

/**
 * Data object representing a column's scripted metadata.
 * <p>
 *     This will hold an optional String with the script in it.  The script should be written to a CDATA section when
 *     marshalled to XML.
 * </p>
 */
@ToString @EqualsAndHashCode @AllArgsConstructor
public class ScriptedExpression {
    /**
     * This is the scripted metadata string for displaying a {@link com.nuix.proserv.t3k.detections.PersonDetection}
     */
    public static final String PERSON_SCRIPT_TEMPLATE = "java_import \"com.nuix.proserv.t3k.ws.MetadataProfileBase\"\n" +
            "MetadataProfileBase::display_person_data $current_item.custom_metadata";

    /**
     * This is the scripted metadata string for non-person types of detections.  It is specifically designed for
     * {@link com.nuix.proserv.t3k.detections.ObjectDetection} and {@link com.nuix.proserv.t3k.detections.CCRDetection}
     * but could be used for similarly designed detections.
     */
    public static final String TYPE_SCRIPT_TEMPLATE = "java_import \"com.nuix.proserv.t3k.ws.MetadataProfileBase\"\n" +
            "MetadataProfileBase::display_object_data $current_item.custom_metadata, \"%s\"";

    /**
     * The {@link ScriptType}, or language, for the script used to translate this script to values.
     */
    @Getter private ScriptType type;

    /**
     * The Script used to calculate column values.  If this is null, then no script expression should be saved.
     * Although this class provides two constants for the scripts to use, any other valid script can be assigned.
     */
    @Getter @Setter private String script;

    /**
     * Create a new ScriptedExpression with a null script and Ruby language.
     */
    public ScriptedExpression() { this(ScriptType.ruby, null); }

}
