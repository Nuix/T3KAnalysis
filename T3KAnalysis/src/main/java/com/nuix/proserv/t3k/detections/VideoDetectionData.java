package com.nuix.proserv.t3k.detections;

import com.nuix.proserv.t3k.T3KApiException;
import lombok.Getter;

public class VideoDetectionData implements DetectionData<Integer> {
    public static final String VIDEO_FRAME = "frame";

    @Getter
    private Integer frame;

    public VideoDetectionData(Object[] data) {
        storeDataFields(data);
    }

    public Integer getData() {
        return frame;
    }

    @Override
    public String toString() {
        return frame.toString();
    }

    private void storeDataFields(Object[] detectionData) {
        if(detectionData.length != 2) {
            throw new T3KApiException(String.format(
                "Unexpected data.  Expected to have 2 values, \"frame\", and the frame number.  Found %s values.",
                detectionData.length
            ));
        }
        if(VIDEO_FRAME.equals(detectionData[0])) {
            frame = ((Number)detectionData[1]).intValue();
        } else {
            throw new T3KApiException(String.format(
                "The key term \"frame\" not found in the data.  %s",
                detectionData
            ));
        }
    }
}
