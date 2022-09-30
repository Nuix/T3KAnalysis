package com.nuix.proserv.t3k.results;

import com.nuix.proserv.t3k.detections.DetectionWithData;
import com.nuix.proserv.t3k.detections.VideoDetectionData;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.IntConsumer;

public class VideoResult extends AnalysisResult implements RasteredResult, ResultWithNalvis {
    public static final String FRAME_COUNT = "frame_count_video";
    public static final String FPS = "fps_video";
    public static final String KEYFRAMES = "key_frame_positions";
    public static final String KEYFRAME_SEARCH = "n_positions_analyzed_for_keyframe_detection";

    @Getter
    private int frameCount;
    @Getter
    private int framesPerSecond;
    private int[] keyFrameList;
    @Getter
    private int keyFrameSearch;

    @Getter
    private int width;

    @Getter
    private int height;

    @Getter
    private String nalvisString;

    private VideoResult() {}

    public int getKeyframeCount() {
        return keyFrameList.length;
    }

    public void forEachKeyframe(IntConsumer consumer) {
        Arrays.stream(keyFrameList).forEach(consumer);
    }

    @Override
    protected void addDataToDetection(DetectionWithData detection, Map<String, Object> detectionData) {
        Object[] data = (Object[]) detectionData.getOrDefault(DetectionWithData.DATA, new Object[0]);
        VideoDetectionData videoDetectionData = new VideoDetectionData(data);
        detection.setData(videoDetectionData);
    }

    @Override
    public String toString() {
        final StringBuilder output = new StringBuilder(super.toString())
                .append(" Frames: ").append(frameCount)
                .append(" Frames Per Second: ").append(framesPerSecond)
                .append(" Frame Size: ").append(width).append("x").append(height)
                .append(" Keyframe Search Count: ").append(keyFrameSearch)
                .append(" Keyframes: [");

        forEachKeyframe(keyframe -> {
            output.append(keyframe).append(", ");
        });

        output.delete(output.length() - 2, output.length() - 1);
        output.append("]");

        return output.toString();
    }

    public static boolean isVideoResult(Map<String, Object> metadata) {
        return metadata.containsKey(FPS);
    }

    public static VideoResult parseResult(Map<String, Object> metadata) {
        if(isVideoResult(metadata)) {
            VideoResult result = new VideoResult();

            result.frameCount = (int) metadata.get(FRAME_COUNT);
            result.framesPerSecond = (int) metadata.get(FPS);
            result.keyFrameList = (int[])metadata.get(KEYFRAMES);
            result.keyFrameSearch = (int) metadata.get(KEYFRAME_SEARCH);

            result.width = (int)metadata.get(WIDTH);
            result.height = (int)metadata.get(HEIGHT);
            result.nalvisString = (String)metadata.getOrDefault(NALVIS, "");

            AnalysisResult.fillSharedFields(result, metadata);

            return result;
        } else {
            return null;
        }
    }
}
