package com.nuix.proserv.t3k.ws.metadataprofile;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.*;

/**
 * Data object for the metadata profile.
 * <p>
 *     This class is a wrapper around a {@link java.util.LinkedHashSet} of {@link Metadata} instances.  The set ensures
 *     no columns are added multiple times, and the Linked implementation ensures they are ordered.  This is the
 *     file that is directly marshalled to and from disk in XML format.
 * </p>
 */
@EqualsAndHashCode
public class MetadataProfile {

    /**
     * The {@link Set} of columns in the metadata profile.  This will be an ordered Set implementation.
     */
    @Getter private Set<Metadata> columns = new LinkedHashSet<>();

}
