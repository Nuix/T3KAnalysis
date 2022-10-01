package com.nuix.proserv.t3k.conn;

import com.google.gson.Gson;
import com.nuix.proserv.t3k.T3KApi;
import com.nuix.proserv.t3k.conn.config.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

public class Application {
    private final T3KApi api;

    private final String exportLocation;
    private final String serverSidePath;

    public Application(String pathToConfig) throws FileNotFoundException {
        File configFile = new File(pathToConfig);
        if(!configFile.exists()) {
            throw new FileNotFoundException("The configuration can not be found: " + pathToConfig);
        }

        FileReader reader = new FileReader(configFile);
        Gson gson = new Gson();
        Configuration config = gson.fromJson(reader, Configuration.class);

        api = new T3KApi(config.getT3k_server_url(), config.getT3k_server_port(),
                config.getNuix_batch_size(), config.getNuix_retry_count(), config.getNuix_retry_seconds());

        exportLocation = config.getNuix_output_path();
        serverSidePath = config.getT3k_server_path();


    }

    public static void main(String[] args) {

    }
}
