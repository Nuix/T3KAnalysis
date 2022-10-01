package com.nuix.proserv.t3k.conn;

public interface AnalysisListener {
    void analysisStarted(String message);
    void analysisUpdated(int step, int stepCount, String message);
    void analysisCompleted(String message);
    void analysisError(String message);
}
