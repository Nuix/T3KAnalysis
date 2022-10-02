package com.nuix.proserv.t3k.conn;

import com.google.gson.Gson;
import com.nuix.proserv.t3k.T3KApi;
import com.nuix.proserv.t3k.conn.config.Configuration;

import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.results.PollResults;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {
    private static final Logger LOG = LogManager.getLogger(Application.class.getCanonicalName());
    private final T3KApi api;

    private final String configDirectory;

    private final Configuration config;

    private final String exportLocation;

    private final String serverSidePath;

    @Setter
    private BatchListener batchListener;

    @Setter
    private AnalysisListener analysisListener;

    public Application(String pathToConfig) throws IOException {
        File configFile = new File(pathToConfig);
        if(!configFile.exists()) {
            throw new FileNotFoundException("The configuration can not be found: " + pathToConfig);
        }

        if(configFile.isDirectory()) {
            throw new IllegalArgumentException("The configuration needs to be the path to a settings file, but a directory was provided: " + pathToConfig);
        }

        configDirectory = configFile.getParent();
        LOG.debug("Configuration is in {}", configDirectory);

        FileReader reader = new FileReader(configFile);
        Gson gson = new Gson();
        config = gson.fromJson(reader, Configuration.class);
        LOG.debug("Configuration: " + config);

        api = new T3KApi(config.getT3k_server_url(), config.getT3k_server_port(),
                config.getNuix_batch_size(), config.getNuix_retry_count(), config.getNuix_retry_seconds());

        exportLocation = config.getNuix_output_path();

        Path pathToExport = Path.of(exportLocation);
        if(!Files.exists(pathToExport)) {
            Files.createDirectories(pathToExport);
        }

        serverSidePath = config.getT3k_server_path();
    }

    public Map<String, AnalysisResult> analyze(List<String> itemsToAnalyze) throws FileNotFoundException {
        if(1 == itemsToAnalyze.size()) {

            // single
            SingleItemAnalyzer analyzer = new SingleItemAnalyzer(api, configDirectory, config, analysisListener);
            AnalysisResult results = analyzer.analyze(itemsToAnalyze.get(0));
            return Map.of(itemsToAnalyze.get(0), results);

        } else if (itemsToAnalyze.size() > 1) {
            return Map.of();
        } else {
            return Map.of();
        }
    }

    public static void main(String[] args) {

        //File dataPath = new File(System.getenv("ProgramData"));
        Path dataPath = Path.of(System.getenv("ProgramData"), "Nuix", "Nuix T3K Analysis");
        File configFile = new File(dataPath.toFile(), "t3k_settings.json");
        LOG.debug("Program Data: {} [{}]", configFile.toString(), configFile.exists());

        BatchListener batcher = new BatchListener() {
            @Override
            public void batchStarted(int index, int batchCount, String message) {
                LOG.info("Batch {}/{} started: {}", index, batchCount, message);
            }

            @Override
            public void batchUpdated(int index, int batchCount, String message) {
                LOG.info("Batch {}/{} updated: {}", index, batchCount, message);
            }

            @Override
            public void batchCompleted(int index, int batchCount, String message) {
                LOG.info("Batch {}/{} completed: {}", index, batchCount, message);
            }
        };

        AnalysisListener analizer = new AnalysisListener() {
            @Override
            public void analysisStarted(String message) {
                LOG.info("Analysis started: {}", message);
            }

            @Override
            public void analysisUpdated(int step, int stepCount, String message) {
                LOG.info("Analysis updated, step {}/{}: {}", step, stepCount, message);
            }

            @Override
            public void analysisCompleted(String message) {
                LOG.info("Analysis completed: {}", message);
            }

            @Override
            public void analysisError(String message) {

            }
        };

        try {
            Application app = new Application(configFile.getAbsolutePath());
            app.setBatchListener(batcher);
            app.setAnalysisListener(analizer);

            String file = "C:\\Projects\\ProServ\\T3K\\Data\\processing\\71f218c4-9bdd-4701-8576-634eaccc1a86.jpg";
            List<String> toAnalyze = List.of(file);
            AnalysisResult result = app.analyze(toAnalyze).get(file);
            LOG.info("Results: {}", result);

        } catch (IOException e) {
            LOG.error(e);
        }

    }
}
