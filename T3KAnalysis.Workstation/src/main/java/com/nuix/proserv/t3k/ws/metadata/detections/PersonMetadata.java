package com.nuix.proserv.t3k.ws.metadata.detections;

import com.nuix.proserv.t3k.detections.Detection;
import com.nuix.proserv.t3k.detections.PersonDetection;
import nuix.CustomMetadataMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PersonMetadata extends DetectionMetadata implements MetadataWithScore {
    private static final String T3K_PERSON = String.format(T3K_TEMPLATE, "%d", "Person");
    private static final String T3K_AGE = String.format("%s|%s", T3K_PERSON, "Age");
    private static final String T3K_GENDER = String.format("%s|%s", T3K_PERSON, "Gender");
    private static final String T3K_P_SCORE = String.format(T3K_SCORE, T3K_PERSON);

    protected PersonMetadata(CustomMetadataMap metadataMap) {
        super(metadataMap);
    }

    @Override
    public String getTypeTemplate(Detection detection) {
        return T3K_PERSON;
    }

    @Override
    public void applyDetection(Detection detection, int detectionIndex) {
        if(! (detection instanceof PersonDetection)) {
            LOG.error("The detection is not a person but is being treated as such.  Skipping it. {}", detection);
            return;
        }

        PersonDetection person = (PersonDetection)detection;
        getMetadataMap().putText(String.format(T3K_GENDER, detectionIndex), person.getGender());
        getMetadataMap().putText(String.format(T3K_AGE, detectionIndex), person.getGender());
        getMetadataMap().putFloat(String.format(T3K_P_SCORE, detectionIndex), person.getScore());
    }
}
