package com.nuix.proserv.t3k.ws;

/**
 * Callback interface for providing feedback on simple incremental processes.
 */
public interface ProgressListener {

    /**
     * Update a listener on the current progress of a process.
     * @param step The current step in the process
     * @param total The total number of steps in the process
     * @param message Any additional message to send with the progress.  This may be null.
     */
    void updateProgress(int step, int total, String message);
}
