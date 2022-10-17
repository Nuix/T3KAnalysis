package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.PersonDetection;
import nuix.CustomMetadataMap;

/**
 * Custom metadata translation for {@link PersonDetection} instances.
 * <p>
 *     On T3K, the detection type is "age/gender".
 * </p>
 */
public class PersonMetadata extends DetectionMetadata implements MetadataWithScore {
    /**
     * This is the custom metadata label for a Person detection.  It is a Format String with a single numerical
     * placeholder for the detection's index.
     */
    private static final String T3K_PERSON = T3K_TEMPLATE + "Person";

    /**
     * The Age label for a Person detection.
     */
    private static final String T3K_AGE = T3K_PERSON + METADATA_LEVEL_SEPARATOR + "Age";

    /**
     * The Gender label for a Person detection.
     */
    private static final String T3K_GENDER = T3K_PERSON + METADATA_LEVEL_SEPARATOR + "Gender";

    /**
     * Create a new PersonMetadata object with the {@link CustomMetadataMap} used to store values in
     * @param metadataMap The {@link nuix.CustomMetadataMap} to store the data in.
     */
    protected PersonMetadata(CustomMetadataMap metadataMap) {
        super(metadataMap);
    }

    /**
     * {@inheritDoc}
     * @param detection The {@link Detection} to generate a custom metadata label for.
     * @return The format string for the Person detection, with a single numerical field for the detection's index
     */
    @Override
    public String getTypeTemplate(Detection detection) {
        return T3K_PERSON;
    }

    /**
     * {@inheritDoc}
     * @param detection The {@link Detection} with the data to add to the custom metadata
     * @param detectionIndex The index in the detections list for this detection
     */
    @Override
    public void applyDetection(Detection detection, int detectionIndex) {
        if(! (detection instanceof PersonDetection)) {
            LOG.error("The detection is not a person but is being treated as such.  Skipping it. {}", detection);
            return;
        }

        PersonDetection person = (PersonDetection)detection;
        String baseLabel = String.format(T3K_PERSON, detectionIndex);
        getMetadataMap().putText(baseLabel + T3K_GENDER, person.getGender());
        getMetadataMap().putInteger(baseLabel + T3K_AGE, person.getAge());
        getMetadataMap().putFloat(baseLabel + T3K_SCORE, person.getScore());
    }
}
