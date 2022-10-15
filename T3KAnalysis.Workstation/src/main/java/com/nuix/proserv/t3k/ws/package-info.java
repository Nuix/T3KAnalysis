/**
 * The c.n.p.t3k.ws package is used to host features and functions related to the T3K analysis tool inside the
 * Workstation environment.
 * <p>
 *     There are two top-level entry points for this package:
 * </p>
 * <dl>
 *     <dt>{@link com.nuix.proserv.t3k.ws.ScriptingBase}</dt>
 *     <dd>
 *         This class holds the starting point for T3K analysis.  It wraps the lower level Connector workflows
 *         with Workstation-aware processes, like
 *         {@link com.nuix.proserv.t3k.ws.ScriptingBase#exportItems(java.util.List, nuix.Utilities, com.nuix.proserv.t3k.ws.ProgressListener)}.
 *         The goal with this class is to simplify the scripts and share as much code between the Worker-Side and
 *         Post-Processing Scripts by hosting the code in a shared Java library.
 *     </dd>
 *     <dt>{@link com.nuix.proserv.t3k.ws.MetadataProfileBase}</dt>
 *     <dd>
 *         This class holds helper methods for the scripted metadata used for the T3K Analysis metadata profile.  It
 *         consists of two methods, one for calculating the display of a "person" type of detection, and one objects and
 *         CCR types of connections.
 *     </dd>
 * </dl>
 * <p>
 *     This package also contains a {@link com.nuix.proserv.t3k.ws.ProgressListener} interface for feeding back
 *     incremental progress on a simple task, and {@link com.nuix.proserv.t3k.ws.WorkerItemMetadataMapWrapper}:
 *     a class that wraps a nuix.WorkerItem in the nuix.CustomMetadataMap interface so Worker Side Scripts and
 *     Post Processing Scripts can share means of applying custom metadata results.
 * </p>
 * <p>
 *     To support these operations the package hosts two sub-packages:
 * </p>
 * <dl>
 *     <dt>{@link com.nuix.proserv.t3k.ws.metadata}</dt>
 *     <dd>
 *         This holds classes used to represent the Custom Metadata corresponding to the various different kinds of
 *         detections.  Each class in this package contains operations needed to apply those detections to an item in
 *         the case.
 *     </dd>
 *     <dt>{@link com.nuix.proserv.t3k.ws.metadataprofile}</dt>
 *     <dd>
 *         This holds classes used to represent the Metadata Profile and is responsible for writing and modifying
 *         the T3K Analysis metadata profile.
 *     </dd>
 * </dl>
 */
package com.nuix.proserv.t3k.ws;

