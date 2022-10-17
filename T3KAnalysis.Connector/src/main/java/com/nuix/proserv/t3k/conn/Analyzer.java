package com.nuix.proserv.t3k.conn;

import com.google.gson.Gson;
import com.nuix.proserv.t3k.T3KApi;
import com.nuix.proserv.t3k.conn.config.Configuration;
import com.nuix.proserv.t3k.results.AnalysisResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

public abstract class Analyzer<T> {
    protected static final Logger LOG = LogManager.getLogger(Application.LOGGER_NAME);

    private final Configuration config;

    private final AnalysisListener analysisListener;

    private final BatchListener bacthListener;

    private final ResultsListener resultsListener;

    private final String configDirectory;

    private final T3KApi api;

    private final SourceId sourceIdMaker;

    protected Analyzer(T3KApi api, String configPath, Configuration configuration,
                       AnalysisListener listener,
                       BatchListener batchListener,
                       ResultsListener resultsListener) {
        this.api = api;
        this.config = configuration;
        this.configDirectory = configPath;
        this.analysisListener = listener;
        this.bacthListener = batchListener;
        this.resultsListener = resultsListener;

        Path sourceIdPath = Path.of(configDirectory, "t3k_data_id.json");
        this.sourceIdMaker = new SourceId(sourceIdPath.toAbsolutePath().toString());
    }

    public abstract void analyze(T toAnalyze, BlockingQueue<AnalysisResult> completedResults);

    protected String getServerSidePath() {
        return config.getT3k_server_path();
    }

    protected int getBatchSize() {
        return config.getNuix_batch_size();
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

    protected void updateBatchStarted(int index, int count, String message) {
        if(null != bacthListener) {
            bacthListener.batchStarted(index, count, message);
        }
    }

    protected void updateBatchUpdated(int index, int count, String message) {
        if(null != bacthListener) {
            bacthListener.batchUpdated(index, count, message);
        }
    }

    protected void updateBatchCompleted(int index, int count, String message) {
        if(null != bacthListener) {
            bacthListener.batchCompleted(index, count, message);
        }
    }

    protected void updateResultAnalyzed() {
        if(null != resultsListener) {
            resultsListener.incrementAnalyzed();
        }
    }

    protected void updateResultError() {
        if (null != resultsListener) {
            resultsListener.incrementErrors();
        }
    }

    protected void updateResultNoMatch() {
        if (null != resultsListener) {
            resultsListener.incrementNotMatched();
        }
    }

    protected void updateResultDetections(int addedCount) {
        if (null != resultsListener) {
            resultsListener.addDetections(addedCount);
        }
    }

    protected SourceId getSourceId() {
        return this.sourceIdMaker;
    }


}
