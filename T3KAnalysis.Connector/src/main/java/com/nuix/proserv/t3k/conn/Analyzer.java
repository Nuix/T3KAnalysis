package com.nuix.proserv.t3k.conn;

import com.google.gson.Gson;
import com.nuix.proserv.t3k.T3KApi;
import com.nuix.proserv.t3k.conn.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class Analyzer {
    private static final Logger LOG = LogManager.getLogger(Analyzer.class.getCanonicalName());

    private final Configuration config;

    private final AnalysisListener analysisListener;

    private final String configDirectory;

    private final T3KApi api;

    protected Analyzer(T3KApi api, String configPath, Configuration configuration, AnalysisListener listener) {
        this.api = api;
        this.config = configuration;
        this.configDirectory = configPath;
        this.analysisListener = listener;
    }

    protected String getServerSidePath() {
        return config.getT3k_server_path();
    }

    protected T3KApi getApi() {
        return api;
    }

    protected void updateAnalysisStarted(String message) {
        if (null != analysisListener) {
            analysisListener.analysisStarted(message);
        }
    }

    protected void updateAnalysisUpdated(int step, int stepCount, String message) {
        if (null != analysisListener) {
            analysisListener.analysisUpdated(step, stepCount, message);
        }
    }

    protected  void updateAnalysisCompleted(String message) {
        if (null != analysisListener) {
            analysisListener.analysisCompleted(message);
        }
    }

    protected void updateAnalysisError(String message) {
        if (null != analysisListener) {
            analysisListener.analysisError(message);
        }
    }

    protected SourceId getSourceId() {
        Path sourceIdPath = Path.of(configDirectory, "t3k_data_id.json");

        if(Files.exists(sourceIdPath)) {
            try {
                return new Gson().fromJson(new FileReader(sourceIdPath.toFile()), SourceId.class);
            } catch(FileNotFoundException e) {
                // Should be impossible since I do the check...
                LOG.error("The source id file ({}) could not be read.", sourceIdPath);
                return new SourceId();
            }
        } else {
            return new SourceId();
        }
    }

    protected void cacheSourceId(SourceId sourceId) throws IOException {
        Path sourceIdPath = Path.of(configDirectory, "t3k_data_id.json");
        try (FileWriter writer = new FileWriter(sourceIdPath.toFile())) {

            String json = new Gson().toJson(sourceId);
            writer.write(json);
        }
    }

}