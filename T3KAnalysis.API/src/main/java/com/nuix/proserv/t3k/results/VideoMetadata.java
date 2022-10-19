package com.nuix.proserv.t3k.results;

import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.function.IntConsumer;

@ToString
public class VideoMetadata extends ResultMetadata {
    private static final long serialVersionUID = 1L;

    @Getter
    private int frame_count_video;

    @Getter
    private double fps_video;

    private int[] key_frame_positions = new int[0];

    @Getter
    private int n_positions_analyzed_for_keyframe_detection;

    @Getter
    private int width;

    @Getter
    private int height;

    public int getKeyFrameCount() {
        return key_frame_positions.length;
    }

    public void forEachKeyFrame(IntConsumer consumer) {
        Arrays.stream(key_frame_positions).forEach(consumer);
    }
}
