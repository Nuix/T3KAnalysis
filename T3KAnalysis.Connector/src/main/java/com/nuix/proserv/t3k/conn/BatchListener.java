package com.nuix.proserv.t3k.conn;

public interface BatchListener {
    void batchStarted(int index, int batchCount, String message);
    void batchUpdated(int index, int batchCount, String message);
    void batchCompleted(int index, int batchCount, String message);
}
