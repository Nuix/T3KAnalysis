package com.nuix.proserv.t3k.conn;

import lombok.Getter;

public class SourceId {
    private long id;

    public long getNextId() {
        return id++;
    }
}
