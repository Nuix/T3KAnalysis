package com.nuix.proserv.t3k.ws.metadataprofile;

import lombok.*;

/**
 * This represents a single column in a metadata profile.
 * <p>
 *     The column will have a Type, and a Name.  Optionally the column can have a {@link ScriptedExpression} if this
 *     is a scripted metadata.  See {@link nuix.MetadataItem#getType()} for details on the type of the metadata.
 * </p>
 */
@ToString @EqualsAndHashCode @AllArgsConstructor
public class Metadata {

    /**
     * The {@link MetadataType} for this column.  See the comment for {@link MetadataType#SCRIPTED} for special
     * considerations for scripted metadata.
     */
    @Getter private MetadataType type;

    /**
     * The name for the metadata column - as it is displayed at the top of the column
     */
    @Getter private String name;

    /**
     * Optional scripting expression for this column if it is a scripted metadata column
     */
    @Getter private ScriptedExpression scriptedExpression;

    /**
     * Creates a new Metadata column of type {@link MetadataType#SPECIAL}, name "Name" and no scripting.
     */
    public Metadata() { this(MetadataType.SPECIAL, "Name", null); }
}
