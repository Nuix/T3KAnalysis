package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.*;
import com.nuix.proserv.t3k.ws.ScriptingBase;
import com.nuix.proserv.t3k.ws.metadata.T3KMetadata;
import lombok.Getter;
import nuix.CustomMetadataMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Abstract parent class for translating {@link Detection} instances to custom metadata applied to an item.
 * <p>
 *     This class represents the functionality of translating a Detection provided by T3K to custom metadata fields
 *     applied to a Nuix item.  There are implementations for various known types of things that can be detected by
 *     T3K.  Currently known types of detections are: "age/gender" (referred to as 'person'), "object", "md5", "text",
 *     and "CCR" (meaning 'curated content result').  Each of these types of detections will result in a different
 *     implementation of {@link Detection}, which will then translate into a different subclass of this abstract
 *     DetectionMetadata.
 * </p>
 * <p>
 *     New types of Detection can be added by extending this baseclass, implementing the
 *     {@link #applyDetection(Detection, int)} and {@link #getTypeTemplate(Detection)} methods, and mapping the
 *     resulting class to the type the Detection represents (as defined by the XxxDetection.TYPE constant) in the
 *     {@link #DETECTION_METADATA_MAP}.  Implementation Note: Currently the DETECTION_METADATA_TYPES map is an
 *     immutable map made by using {@link Map#of()}.  This has a limit of 10 key-value pairs, so if the number of
 *     results sources expands a different technique may be needed to create the map.
 * </p>
 * <p>
 *     This class implements the non-functional {@link T3KMetadata} interface so all children will have direct access
 *     to the constants present therein.
 * </p>
 */
public abstract class DetectionMetadata implements T3KMetadata {
    /**
     * A logger that all subclasses will have access to.  This will log to the same location as the {@link ScriptingBase}
     */
    protected static final Logger LOG = LogManager.getLogger(ScriptingBase.LOGGING_NAME);

    /**
     * Immutable map containing the mapping of supported detection types (by XxxDetection.TYPE) to
     * DetectionMetadata implementation (by class) used to apply the results to an item.
     */
    private static final Map<String, Class<? extends DetectionMetadata>> DETECTION_METADATA_MAP = Map.of(
            PersonDetection.TYPE, PersonMetadata.class,
            ObjectDetection.TYPE, ObjectMetadata.class,
            MD5Detection.TYPE, MD5Metadata.class,
            TextDetection.TYPE, TextMetadata.class,
            CCRDetection.TYPE, CCRMetadata.class
    );

    /**
     * Each instance will contain a {@link CustomMetadataMap} that will be used to store the detection details.
     */
    @Getter
    private final CustomMetadataMap metadataMap;

    /**
     * Create an instance of the DetectionMetadata.  This is an abstract class, and should only be initialized from
     * children.
     * @param map The {@link CustomMetadataMap} to store results in
     * @throws IllegalArgumentException if map is null
     */
    protected DetectionMetadata(CustomMetadataMap map) {
        if (null == map) throw new IllegalArgumentException("The CustomMetadataMap used to store DetectionMetadata must not be null.");

        this.metadataMap = map;
    }

    /**
     * Provide a String to use as a Format String template to assign custom metadata data to.
     * <p>
     *     The result string will be the full path to the detection name, and have a placeholder for the detection's
     *     index.  For example, for an 'army_tank' detection, the expected return would be:
     *     {@code "T3K Detections|%d|army_tank"}.  Child metadata will be added to this label as needed for the
     *     details of the detection.
     * </p>
     * @param detection The {@link Detection} to generate a custom metadata label for.
     * @return A Format string, with a placeholder for an integer index representing this detection.
     */
    public abstract String getTypeTemplate(Detection detection);

    /**
     * Apply the {@link Detection} data to the {@link CustomMetadataMap}.  The result of this method will be assigned
     * custom metadata for a single index with all the details for this detection present.
     * <p>
     *     The detections will be added to the item as children to the
     *     T3K_DETECTIONS metadata in the form T3K_DETECTION - &lt;index&gt; - &lt;detection name&lt;, where the index
     *     is this detections order in the detected results list.  The detection name is determined by the type of the
     *     detection.  Each detection will also have child metadata based on the result and detection types. An example
     *     of a single 'Person' detection might be as follows:
     * </p>
     * <ul>
     *     <li>
     *         T3K Detections
     *         <ul>
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
     * @param detection The {@link Detection} with the data to add to the custom metadata
     * @param detectionIndex The index in the detections list for this detection
     */
    public abstract void applyDetection(Detection detection, int detectionIndex);

    /**
     * Static factory method for creating an appropriate DetectionMetadata instance given the provided {@link Detection}.
     * <p>
     *     This method will use the Detection's TYPE constant to look up the appropriate DetectionMetadata to generate.
     *     If it can't find one, or for some reason can't instantiate it, then it will generate an
     *     {@link UnknownMetadata} instance instead.
     * </p>
     * @param detection The {@link Detection} used as the source of data for the custom metadata
     * @param metadataMap The {@link CustomMetadataMap} to store the custom metadata in
     * @return An instance of an appropriate subclass of DetectionMetadata, or an {@link UnknownMetadata} if one can't be found.
     */
    public static DetectionMetadata getMetadataForDetection(Detection detection, CustomMetadataMap metadataMap) {
        String detectionType = detection.getType();

        if (null == detectionType) {
            LOG.warn("This is an unknown detection type.  Returning a generic metadata type");
            return new UnknownMetadata(metadataMap);
        }

        Class<? extends DetectionMetadata> metadataClass = DETECTION_METADATA_MAP.getOrDefault(detectionType, UnknownMetadata.class);
        try {
            //Constructor<?>[] constructors = metadataClass.getConstructors();
            Constructor<? extends DetectionMetadata> constructor = metadataClass.getDeclaredConstructor(CustomMetadataMap.class);
            constructor.setAccessible(true);
            DetectionMetadata detectionMetadata = constructor.newInstance(metadataMap);
            return detectionMetadata;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOG.warn("The class for the metadata for detection type {} could not be instantiated, using a default instance.", detectionType);
            return new UnknownMetadata(metadataMap);
        }
    }
}
