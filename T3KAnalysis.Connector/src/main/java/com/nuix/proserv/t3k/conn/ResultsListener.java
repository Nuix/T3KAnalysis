package com.nuix.proserv.t3k.conn;

public interface ResultsListener {
    void incrementAnalyzed();
    void incrementErrors();
    void incrementNotMatched();
    void addDetections(int count);
}
