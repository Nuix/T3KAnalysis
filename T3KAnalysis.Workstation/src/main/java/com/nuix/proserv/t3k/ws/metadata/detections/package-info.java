/**
 * This package contains classes that translate detections found during T3K analysis (in the form of
 * {@link com.nuix.proserv.t3k.detections.Detection} objects) into custom metadata fields to apply to Nuix items.
 * <p>
 *     The {@link com.nuix.proserv.t3k.ws.metadata.detections.DetectionMetadata} class is the abstract parent of all the
 *     subsequent implementations.  There will be an implementation for each known type of Detection.  Detection types
 *     are based on the type of detections found on an item.  Currently known types are: "aga/gender" (referred to as
 *     'person' here), "object", "CCR" (for curated content result), "md5", and "text".  Use the following procedure
 *     to add a new detection type:
 * </p>
 * <ol>
 *     <li>Subclass {@link com.nuix.proserv.t3k.ws.metadata.detections.DetectionMetadata}</li>
 *     <li>Implement the {@link com.nuix.proserv.t3k.ws.metadata.detections.DetectionMetadata#getTypeTemplate(com.nuix.proserv.t3k.detections.Detection)} method</li>
 *     <li>Implement the {@link com.nuix.proserv.t3k.ws.metadata.detections.DetectionMetadata#applyDetection(com.nuix.proserv.t3k.detections.Detection, int)} method</li>
 *     <li>Register the new DetectionMetadata to the {@link com.nuix.proserv.t3k.ws.metadata.detections.DetectionMetadata#DETECTION_METADATA_MAP} map by mapping the Detection implementation to the new metadata class</li>
 * </ol>
 * <p>
 *     This package also implements {@link com.nuix.proserv.t3k.ws.metadata.detections.UnknownMetadata} which provides
 *     a default type to use when the actual type of a Detection is not known.  The
 *     {@link com.nuix.proserv.t3k.ws.metadata.detections.MetadataWithScore} interface provides constants for some
 *     common matadata fields used by Detections that have scores.  An implementing class can implement this interface
 *     to gain easy access to those constants.
 * </p>
 */
package com.nuix.proserv.t3k.ws.metadata.detections;