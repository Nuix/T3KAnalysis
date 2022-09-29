package com.nuix.proserv.t3k.results;

import java.util.Map;

public class VideoResult extends AnalysisResult implements RasteredResult, ResultWithNalvis {
    public static final String FRAME_COUNT = "frame_count_video";
    public static final String FPS = "fps_video";
    public static final String KEYFRAMES = "key_frame_positions";
    public static final String KEYFRAME_SEARCH = "n_positions_analyzed_for_keyframe_detection";

    // TODO: Implement isVideoResult
    public static boolean isVideoResult(Map<String, Object> resultData) {
        return false;
    }
}
