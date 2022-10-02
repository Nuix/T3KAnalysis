package com.nuix.proserv.t3k.conn;

import com.google.gson.Gson;
import com.nuix.proserv.t3k.T3KApi;
import com.nuix.proserv.t3k.conn.config.Configuration;

import com.nuix.proserv.t3k.detections.DetectionWithData;
import com.nuix.proserv.t3k.results.AnalysisResult;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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

    public void analyze(List<String> itemsToAnalyze, BlockingQueue<AnalysisResult> completedResults) throws FileNotFoundException {
        if(1 == itemsToAnalyze.size()) {

            // single
            SingleItemAnalyzer analyzer = new SingleItemAnalyzer(api, configDirectory, config, analysisListener);

            BlockingQueue<AnalysisResult> completedItems = new LinkedBlockingQueue<>();
            analyzer.analyze(itemsToAnalyze.get(0), completedItems);

            AnalysisResult results = null;
            try {
                results = completedItems.take();
            } catch (InterruptedException e) {
                LOG.error("Getting the results from the completed queue was interrupted");
                try {
                    completedResults.put(END_OF_ANALYSIS);
                } catch (InterruptedException ex) {
                    LOG.error("Signalling end of analysis (caused by interrupted getting results) was interrupted.");
                }
                return;
            }

            try {
                completedResults.put(results);
            } catch (InterruptedException e) {
                LOG.error("Putting completed results into the result queue was interrupted.");
            }

            try {
                completedResults.put(END_OF_ANALYSIS);
            } catch (InterruptedException e) {
                LOG.error("Signalling end of analysis (after completed results) was interrupted.");
            }
        } else if (itemsToAnalyze.size() > 1) {
            LOG.info("Processing batches of items.");

            BatchAnalyzer analyzer = new BatchAnalyzer(api, configDirectory, config, batchListener);

            List<List<String>> batches = analyzer.buildBatches(itemsToAnalyze);
            final int batchCount = batches.size();
            int currentBatchIndex = 0;

            for(List<String> batch : batches) {
                currentBatchIndex++;

                LOG.info("Processing batch {}/{}", currentBatchIndex, batchCount);
                if(null != batchListener) {
                    batchListener.batchStarted(currentBatchIndex, batchCount, String.format(
                            "Processing Batch %d / %d", currentBatchIndex, batchCount
                    ));
                }

                analyzer.analyze(batch, completedResults);

                LOG.info("Finished batch {}/{}", currentBatchIndex, batchCount);
                if(null != batchListener) {
                    batchListener.batchStarted(currentBatchIndex, batchCount, String.format(
                            "Completed Batch %d / %d", currentBatchIndex, batchCount
                    ));
                }
            }

        } else {
            // Empty list, return an empty map.
            LOG.warn("The list of items to analyze is empty.");
            try {
                completedResults.put(END_OF_ANALYSIS);
            } catch (InterruptedException e) {
                LOG.error("Signalling end of analysis (caused by no items to analyze) was interrupted.");
            }
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
            BlockingQueue<AnalysisResult> completedAnalysis = new LinkedBlockingQueue<>();

            app.analyze(toAnalyze, completedAnalysis);

            AnalysisResult result = null;
            try {
                result = completedAnalysis.take();
            } catch (InterruptedException e) {
                LOG.error("Taking the results from analysis was interrupted");
            }

            LOG.info("Results: {}", result);

        } catch (IOException e) {
            LOG.error(e);
        }

    }

    public static final AnalysisResult END_OF_ANALYSIS = new AnalysisResult() {
        @Override
        protected void addDataToDetection(DetectionWithData detectionWithData, Map<String, Object> map) {
            // Do nothing
        }
    };

}
