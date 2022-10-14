package com.nuix.proserv.t3k.ws.metadataprofile;

import lombok.*;


@ToString @EqualsAndHashCode @AllArgsConstructor
public class ScriptedExpression {
    public static final String PERSON_SCRIPT_TEMPLATE = "java_import \"com.nuix.proserv.t3k.ws.MetadataProfileBase\"\n" +
            "MetadataProfileBase::display_person_data $current_item.custom_metadata";
    public static final String TYPE_SCRIPT_TEMPLATE = "java_import \"com.nuix.proserv.t3k.ws.MetadataProfileBase\"\n" +
            "MetadataProfileBase::display_object_data $current_item.custom_metadata, \"%s\"";

    @Getter
    private String type;

    @Getter @Setter
    private String script;

    public ScriptedExpression() { this("ruby", null); }

}
