package com.nuix.proserv.t3k.ws;

public interface ProgressListener {
    void updateProgress(int step, int total, String message);
}
