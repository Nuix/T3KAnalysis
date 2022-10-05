package com.nuix.proserv.t3k.detections;

import com.nuix.proserv.t3k.T3KApiException;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;

public class DocumentDetectionData implements DetectionData<Integer[]> {
    public static final String PAGE = "document_page_number";
    public static final String IMAGE = "document_image_number";

    @Getter
    private int pageNumber;

    @Getter
    private int imageNumber;

    public DocumentDetectionData(Object[] data) {
        storeDataFields(data);
    }

    @Override
    public String toString() {
        return String.format("Page: %d Image: %d", pageNumber, imageNumber);
    }

    @Override
    public Integer[] getData() {
        return new Integer[]{ pageNumber, imageNumber };
    }

    private void storeDataFields(Object[] detectionData) {
        if (2 != detectionData.length) {
            throw new T3KApiException(String.format(
                "Expected 2 values in the data list.  It should be either '\"%s\", pageNumber', " +
                "or '[\"%s\", pageNumber], [\"%s\", imageNumber'].  Got %d values.",
                PAGE, PAGE, IMAGE, detectionData.length
            ));
        }

        if (detectionData[0] instanceof String) {
            // "page_number", page_num format
            if(!PAGE.equals(detectionData[0])) {
                throw new T3KApiException(String.format(
                    "Expected the key \"%s\" as the first item, but got: %s",
                        PAGE, detectionData[0]
                ));
            } else {
                pageNumber = ((Number)detectionData[1]).intValue();
            }
        } else {
            // [key, value], [key, value] format
            Arrays.stream(detectionData).forEach(entry -> {
                Object[] dataPoint = (Object[]) entry;
                if(PAGE.equals(dataPoint[0])) {
                    pageNumber = ((Number)dataPoint[1]).intValue();
                } else if (IMAGE.equals(dataPoint[0])) {
                    imageNumber = ((Number)dataPoint[1]).intValue();
                } else {
                    // unexpected key
                    throw new T3KApiException(String.format(
                        "Expected keys are \"%s\" and \"%s\", but got: %s",
                        PAGE, IMAGE, dataPoint[0]
                    ));
                }
            });
        }
    }
}
