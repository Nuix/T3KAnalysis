package com.nuix.proserv.t3k.ws.metadataprofile;

/**
 * The allowed types of scripted expressions.
 * <p>
 *     When serialized to disk, they should be serialized as shown here - case matters.
 * </p>
 */
public enum ScriptType {
    ruby,
    python,
    ECMAScript
}
