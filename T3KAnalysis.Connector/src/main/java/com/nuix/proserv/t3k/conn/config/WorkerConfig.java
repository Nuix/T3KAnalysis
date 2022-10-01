package com.nuix.proserv.t3k.conn.config;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class WorkerConfig {

    @Getter @Setter
    private int workerCount;

    @Getter @Setter
    private String workerTemp;

    @Getter @Setter
    private long workerMemory;
}
