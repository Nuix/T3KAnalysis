package com.nuix.proserv.t3k.conn.config;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class Configuration {
    @Getter @Setter
    private String t3k_server_url;

    @Getter @Setter
    private int t3k_server_port;

    @Getter @Setter
    private String t3k_server_path;

    @Getter @Setter
    private String nuix_output_path;

    @Getter @Setter
    private int nuix_batch_size;

    @Getter @Setter
    private int nuix_retry_count;

    @Getter @Setter
    private int nuix_retry_seconds;

    @Getter @Setter
    @SerializedName("Nuix Worker Settings")
    private WorkerConfig nuix_worker_config;

}
