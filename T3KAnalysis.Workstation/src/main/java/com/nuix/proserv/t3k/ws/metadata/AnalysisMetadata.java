package com.nuix.proserv.t3k.ws.metadata;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.results.DocumentResult;
import com.nuix.proserv.t3k.results.ImageResult;
import com.nuix.proserv.t3k.results.VideoResult;
import com.nuix.proserv.t3k.ws.ScriptingBase;
import lombok.Getter;
import nuix.CustomMetadataMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Abstract base class for all classes that translate AnalysisResults to Custom Metadata.
 * <p>
 *     This class represents the functionality of translating {@link AnalysisResult} instances to custom metadata
 *     applied to an object.  There are implementations for each of the known types of results, which are based on
 *     the types of items analyzed.  Currently known types are Images, Videos, and Documents.  Each of these types
 *     of items produces a different, type-specific implementation of AnalysisResult, and each implementation of
 *     AnalysisResult will map to a specific implementation of this abstract class.
 * </p>
 * <p>
 *     New types can be added by extending this base class, implementing the {@link #addDetection(Detection)} method,
 *     and adding the mapping of the specific AnalysisResult class name to the new AnalysisMetadata subclass in the
 *     {@link #ANALYSIS_METADATA_TYPES} map.  Implementation Note: Currently the ANALYSIS_METADATA_TYPES map is an
 *     immutable map made by using {@link Map#of()}.  This has a limit of 10 key-value pairs, so if the number of
 *     results sources expands a different technique may be needed to create the map.
 * </p>
 * <p>
 *     This class implements the non-functional {@link T3KMetadata} interface so all children will have direct access
 *     to the constants present therein.
 * </p>
 */
public abstract class AnalysisMetadata implements T3KMetadata {
    /**
     * A logger that will be shared by all children of this class.  It will log to the same place as the ScriptingBase's
     * log.
     */
    protected static final Logger LOG = LogManager.getLogger(ScriptingBase.LOGGING_NAME);

    /**
     * Immutable map containing the mapping of {@link AnalysisResult} implementations (by canonical name) to
     * AnalysisMetadata implementation (by class) used to apply the results to an item.
     */
    private static final Map<String, Class<? extends AnalysisMetadata>> ANALYSIS_METADATA_TYPES = Map.of(
            ImageResult.class.getCanonicalName(), ImageMetadata.class,
            VideoResult.class.getCanonicalName(), VideoMetadata.class,
            DocumentResult.class.getCanonicalName(), DocumentMetadata.class
    );

    /**
     * Each instance will contain a {@link CustomMetadataMap} that will be used to store the analysis results.
     */
    @Getter private final CustomMetadataMap metadataMap;

    /**
     * Each instance will contain a {@link AnalysisResult} that acts as the source of the data to store as metadata.
     */
    @Getter private final AnalysisResult analysisResult;

    /**
     * Create an instance of the AnalysisMetadata.  This is an abstract class, and should only be initialized from
     * children.  Both of the parameters are required, providing null on either will cause a
     * {@link IllegalArgumentException}.
     * @param metadataMap The {@link CustomMetadataMap} to store results in
     * @param analysisResult The {@link AnalysisResult} that is this instance's source of data
     * @throws IllegalArgumentException if either metadataMap or analysisResult are null
     */
    protected AnalysisMetadata(CustomMetadataMap metadataMap, AnalysisResult analysisResult) {
        if (null == metadataMap) throw new IllegalArgumentException("The CustomMetadataMap provided to AnalysisMetadata must not be null.");

        if (null == analysisResult) throw new IllegalArgumentException("The AnalysisResult provided to AnalysisMetadata must not be null.");

        this.metadataMap = metadataMap;
        this.analysisResult = analysisResult;
    }

    /**
     * Apply the {@link AnalysisResult} data to the {@link CustomMetadataMap}.  The result of this method will be the
     * item containing the custom metadata derived from the analysis.
     * <p>
     *     If there were no detections found in the results, then {@link T3KMetadata#T3K_DETECTION} will be assigned
     *     {@link T3KMetadata#T3K_NO_MATCHES}.  If there are 1 or more detections, then it will be assigned
     *     {@link T3KMetadata#T3K_MATCH_FOUND}, and the {@link T3KMetadata#T3K_COUNT} field will be assigned the
     *     number of detections found.
     * </p>
     * <p>
     *     Additionally, if there are 1 or more detections, the detections will be added to the item as children to the
     *     T3K_DETECTIONS metadata in the form T3K_DETECTION - &lt;index&gt; - &lt;detection name&lt;, where the index
     *     starts at one and runs to the number of detections.  The detection name is determined by the type of the
     *     detection.  Each detection will also have child metadata based on the result and detection types. An example
     *     of an document with a single 'Person' detection might be as follows:
     * </p>
     * <ul>
     *     <li>
     *         T3K Detections: Match Detected
     *         <ul>
     *             <li>Count: 1</li>
     *             <li>
     *                 Person
     *                 <ul>
     *                     <li>Age: 5</li>
     *                     <li>Gender: Male</li>
     *                     <li>Score: 53</li>
     *                     <li>Page: 2</li>
     *                     <li>Image: 4</li>
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     * </ul>
     */
    public void applyResults() {
        int detectionCount = analysisResult.getDetectionCount();

        if(0 == detectionCount) {
            getMetadataMap().putText(T3K_DETECTION, T3K_NO_MATCHES);
        } else {
            getMetadataMap().putText(T3K_DETECTION, T3K_MATCH_FOUND);
            getMetadataMap().putInteger(T3K_COUNT, detectionCount);
            analysisResult.forEachDetection(this::addDetection);
        }
    }

    /**
     * Add a detection to this metadata.
     * <p>
     *     Translate the provided {@link Detection} custom metadata to store in this instance's
     *     {@link CustomMetadataMap}. The result of this call will be the Detection's information, including any
     *     included {@link com.nuix.proserv.t3k.detections.DetectionData}, being added to the custom metadata for the
     *     current item.  The actual custom metadata assigned will depend on the AnalysisMetadata implementation and,
     *     ultimately the type of AnalysisResult and Detection type.
     * </p>
     * @param detection the {@link Detection} whose values should be added to the custom metadata.
     */
    protected abstract void addDetection(Detection detection);


    /**
     * Find an AnalysisMetadata implementation that can be used to translate the provided AnalysisResult, use the
     * default implementation provided as a parameter.
     * <p>
     *     This method will use the {@link #ANALYSIS_METADATA_TYPES} map and loop through all the AnalysisResult
     *     subclasses assigned, seeing if any of them are assignable from the passed in AnalysisResult's class.  If
     *     it is then this method will return the found Class.  If nothing matches then the default type will be returned.
     * </p>
     * @param analysisResult A {@link AnalysisResult} instance to get an AnalysisMetadata for
     * @param defaultImpl A subclass of AnalysisMetadata to use if there are no matches to the provided analysis results.
     * @return A Class for a suitable AnalysisMetadata implementation, or the default if none found.
     */
    private static Class<? extends AnalysisMetadata> lookupImplementationOrDefault(AnalysisResult analysisResult,
                                                                                   Class<? extends AnalysisMetadata> defaultImpl) {

        Class<? extends AnalysisMetadata> mdClass = null;

        for(Map.Entry<String, Class<? extends AnalysisMetadata>> entry : ANALYSIS_METADATA_TYPES.entrySet()) {
            try {
                Class<? extends AnalysisResult> resultClass = (Class<? extends AnalysisResult>)Class.forName(entry.getKey());
                LOG.debug("Working on {}", resultClass.getCanonicalName());

                if (resultClass.isAssignableFrom(analysisResult.getClass())) {
                    mdClass = entry.getValue();
                    break;
                }
            } catch (ClassNotFoundException e) {
                LOG.error(e);
                LOG.error("The located class can not be loaded, ignoring and trying others. {}", entry.getKey());
            }
        }

        if (null == mdClass) {
            return defaultImpl;
        } else {
            return mdClass;
        }

    }

    /**
     * Create an instance of an AnalysisMetadata subclass and return it.
     * <p>
     *     The type of the returned object will be based on the type of the {@link AnalysisResult} passed in, as mapped
     *     by this class' {@link #ANALYSIS_METADATA_TYPES} map.  This will return an instance of the
     *     {@link UnknownAnalysisTypeMetadata} if there is no good match to the source results, or if there is trouble
     *     instantiating the matching metadata class.
     * </p>
     * @param analysisResult The {@link AnalysisResult} to get an AnalysisMetadata instance for
     * @param metadataMap The {@link CustomMetadataMap} to store metadata in
     * @return A matching instance of an AnalysisMetadata implementation or {@link UnknownAnalysisTypeMetadata} instance.
     */
    public static AnalysisMetadata getInstance(AnalysisResult analysisResult, CustomMetadataMap metadataMap) {
        Class<? extends AnalysisMetadata> metadataClass = lookupImplementationOrDefault(analysisResult,
                UnknownAnalysisTypeMetadata.class);

        try {
            Constructor<? extends AnalysisMetadata> constructor = metadataClass.getConstructor(CustomMetadataMap.class, AnalysisResult.class);
            LOG.debug("Constructor: {}", constructor);

            AnalysisMetadata metadata = constructor.newInstance(metadataMap, analysisResult);
            LOG.debug("Metadata Implementation: {}", metadata);
            return metadata;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOG.error(e);
            LOG.error("A metadata implementation for the provided analysis type ({}) cannot be found or created.  " +
                    "Using a default one.", analysisResult.getClass().getCanonicalName());
            return new UnknownAnalysisTypeMetadata(metadataMap, analysisResult);
        }
    }
}
