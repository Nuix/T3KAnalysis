/**
 * Classes used for reading and building a Metadata Profile.
 * <p>
 *     This package contains several data objects ({@link com.nuix.proserv.t3k.ws.metadataprofile.MetadataProfile},
 *     {@link com.nuix.proserv.t3k.ws.metadataprofile.Metadata}, and
 *     {@link com.nuix.proserv.t3k.ws.metadataprofile.ScriptedExpression}) that represent the metadata profile,
 *     a column in the metadata profile, and a scripted expression for a scripted metadata column (respectively).
 * </p>
 * <p>
 *     This also has the {@link com.nuix.proserv.t3k.ws.metadataprofile.MetadataProfileReaderWriter} class which is
 *     responsible for marshalling the data to and from the XML file it is stored in.
 * </p>
 * <p>
 *     This package is used by the {@link com.nuix.proserv.t3k.ws.ScriptingBase} class to generate the metadata profile.
 *     The scripted metadata columns it generates will use the {@link com.nuix.proserv.t3k.ws.MetadataProfileBase}
 *     class to do the work.
 * </p>
 */
package com.nuix.proserv.t3k.ws.metadataprofile;
