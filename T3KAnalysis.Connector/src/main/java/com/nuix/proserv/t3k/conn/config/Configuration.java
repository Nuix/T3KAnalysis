package com.nuix.proserv.t3k.conn.config;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class Configuration {
    @Getter @Setter
    private String t3k_server_url = "http://127.0.0.1";

    @Getter @Setter
    private int t3k_server_port = 5000;

    @Getter @Setter
    private String t3k_server_path = "/CORE/resources/data";

    @Getter @Setter
    private String nuix_output_path = "C:/Data/processing";

    @Getter @Setter
    private int nuix_batch_size = 10;

    @Getter @Setter
    private int nuix_retry_count = 50;

    @Getter @Setter
    private int nuix_retry_seconds = 1;

    @Getter @Setter
    @SerializedName("Nuix Worker Settings")
    private WorkerConfig nuix_worker_config = new WorkerConfig();

}
