package com.nuix.proserv.t3k.detections;

import com.nuix.proserv.t3k.T3KApiException;
import lombok.Getter;

import java.util.Map;

public class VideoDetectionData implements DetectionData<Integer> {
    public static final String VIDEO_FRAME = "frame";

    @Getter
    private Integer data;

    VideoDetectionData(Object[] data) {
        storeDataFields(data);
    }

    @Override
    public String toString() {
        return data.toString();
    }

    private void storeDataFields(Object[] detectionData) {
        if(detectionData.length != 2) {
            throw new T3KApiException(String.format(
                "Unexpected data.  Expected to have 2 values, \"frame\", and the frame number.  Found %s values.",
                detectionData.length
            ));
        }
        if(VIDEO_FRAME.equals(detectionData[0])) {
            data = (Integer)detectionData[1];
        } else {
            throw new T3KApiException(String.format(
                "The key term \"frame\" not found in the data.  %s",
                detectionData
            ));
        }
    }
}
