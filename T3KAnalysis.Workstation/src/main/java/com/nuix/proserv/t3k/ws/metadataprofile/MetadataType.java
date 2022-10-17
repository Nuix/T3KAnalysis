package com.nuix.proserv.t3k.ws.metadataprofile;

import nuix.MetadataItem;

/**
 * The type of a {@link Metadata} column.
 * <p>
 *     The type definitions come from {@link MetadataItem#getType()}, and the documentation there should be consulted.
 *     There is one exception - and that is, the docs say to use {@link #SCRIPTED} for scripted metadata but Workstation
 *     uses {@link #SPECIAL}.  To follow Workstation's precedent, this application will use {@code SPECIAL} instead of
 *     {@code SCRIPTED}.
 * </p>
 */
public enum MetadataType {
    /**
     * Metadata defined by the application, such as GUID, or Name
     */
    SPECIAL,
    /**
     * Regular metdata gathered from the item during ingestion
     */
    PROPERTY,
    /**
     * Metadata derived from other metadata using a simple expression
     */
    DERIVED,
    /**
     * Metadata created at the evidence level at load time
     */
    EVIDENCE,
    /**
     * Scripted metadata using an arbitrary scripting expression.
     * ** NOTE ** Not used as such in this application, {@link #SPECIAL} is used instead.
     */
    SCRIPTED,
    /**
     * User-defined custom metadata
     */
    CUSTOM
}
