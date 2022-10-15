/**
 * This package contains classes that translate T3K analysis results (in the form of
 * {@link com.nuix.proserv.t3k.results.AnalysisResult} objects) into custom metadata fields to apply to nuix items.
 * <p>
 *     The {@link com.nuix.proserv.t3k.ws.metadata.AnalysisMetadata} class is the abstract parent of all the subsequent
 *     implementations.  There will be an implementation for each known type of AnalysisResult.  Analysis results are
 *     based on the type of item that was analyzed - currently Image, Video, or Document.  When a new type is added
 *     the following should be done:
 * </p>
 * <ol>
 *     <li>Subclass {@link com.nuix.proserv.t3k.ws.metadata.AnalysisMetadata}</li>
 *     <li>Implment the {@link com.nuix.proserv.t3k.ws.metadata.AnalysisMetadata#applyDetection(com.nuix.proserv.t3k.detections.Detection)} method</li>
 *     <li>If the new type provides data for the detection (like page number or frame id) then apply the data store in the result to the metadasta</li>
 *     <li>Register the new AnalysisMetadata to the {@link com.nuix.proserv.t3k.ws.metadata.AnalysisMetadata#ANALYSIS_METADATA_TYPES} map by mapping the AnalysisResult implementation to the new metadata class</li>
 * </ol>
 * <p>
 *     This package also implements {@link com.nuix.proserv.t3k.ws.metadata.UnknownAnalysisTypeMetadata} which provides
 *     a default type to use when the actual type of an AnalysisResult is not known.  The
 *     {@link com.nuix.proserv.t3k.ws.metadata.T3KMetadata} interface provides constants for some common matadata field
 *     names.  An implementing class can implement this interface to gain easy access to those constants.
 * </p>
 */
package com.nuix.proserv.t3k.ws.metadata;