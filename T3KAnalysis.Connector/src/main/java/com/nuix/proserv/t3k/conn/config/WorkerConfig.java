package com.nuix.proserv.t3k.conn.config;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class WorkerConfig {

    @Getter @Setter
    private int workerCount = 2;

    @Getter @Setter
    private String workerTemp = "C:/WorkerTemp";

    @Getter @Setter
    private long workerMemory = 1024;
}
