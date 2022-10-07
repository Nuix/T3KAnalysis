package com.nuix.proserv.t3k.detections;

import com.nuix.proserv.t3k.T3KApiException;
import lombok.Getter;

public class VideoDetectionData implements DetectionData<Integer> {
    private static final long serialVersionUID = 1L;

    public static final String VIDEO_FRAME = "frame";

    @Getter
    private final int frame;

    public VideoDetectionData(int frame) {
        this.frame = frame;
    }

    public Integer getData() {
        return frame;
    }

    @Override
    public String toString() {
        return String.format("Frame: %d", frame);
    }
}
