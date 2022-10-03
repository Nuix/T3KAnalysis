package com.nuix.proserv.ws;

public interface ProgressListener {
    void updateProgress(int step, int total, String message);
}
