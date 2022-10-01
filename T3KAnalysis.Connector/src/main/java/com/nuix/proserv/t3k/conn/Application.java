package com.nuix.proserv.t3k.conn;

import com.google.gson.Gson;
import com.nuix.proserv.t3k.T3KApi;
import com.nuix.proserv.t3k.conn.config.Configuration;

import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;

public class Application {
    private static final Logger LOG = LogManager.getLogger(Application.class.getCanonicalName());
    private final T3KApi api;

    private final String exportLocation;

    private final String serverSidePath;

    @Setter
    private BatchListener batchListener;

    @Setter
    private AnalysisListener analysisListener;

    public Application(String pathToConfig) throws FileNotFoundException {
        File configFile = new File(pathToConfig);
        if(!configFile.exists()) {
            throw new FileNotFoundException("The configuration can not be found: " + pathToConfig);
        }

        FileReader reader = new FileReader(configFile);
        Gson gson = new Gson();
        Configuration config = gson.fromJson(reader, Configuration.class);
        LOG.debug("Configuration: " + config);

        api = new T3KApi(config.getT3k_server_url(), config.getT3k_server_port(),
                config.getNuix_batch_size(), config.getNuix_retry_count(), config.getNuix_retry_seconds());

        exportLocation = config.getNuix_output_path();
        serverSidePath = config.getT3k_server_path();

    }

    public static void main(String[] args) {

        //File dataPath = new File(System.getenv("ProgramData"));
        Path dataPath = Path.of(System.getenv("ProgramData"), "Nuix", "Nuix T3K Analysis");
        File configFile = new File(dataPath.toFile(), "t3k_settings.json");
        LOG.debug("Program Data: {} [{}]", configFile.toString(), configFile.exists());

        try {
            Application app = new Application(configFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            LOG.error(e);
        }

    }
}
