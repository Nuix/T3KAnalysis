package com.nuix.proserv.t3k.conn;

import com.google.gson.Gson;
import com.nuix.proserv.t3k.T3KApi;
import com.nuix.proserv.t3k.conn.config.Configuration;

import com.nuix.proserv.t3k.results.AnalysisResult;
import com.nuix.proserv.t3k.results.ResultMetadata;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Application {

    public static final String LOGGER_NAME = "com.nuix.proserv.t3k";
    private static final Logger LOG = LogManager.getLogger(LOGGER_NAME);
    private final T3KApi api;

    private final String configDirectory;

    @Getter
    private final Configuration config;

    private final String exportLocation;

    private final String serverSidePath;

    @Setter
    private BatchListener batchListener;

    @Setter
    private AnalysisListener analysisListener;

    @Setter
    private ResultsListener resultsListener;

    public Application(String pathToConfig) {
        File configFile = new File(pathToConfig);
        configDirectory = configFile.getParent();
        LOG.debug("Configuration is in {}", configDirectory);

        String configState = "OK";

        FileReader reader = null;

        if(configFile.isDirectory()) {
            LOG.error("The configuration needs to be the path to a settings file, but a directory was provided: {}", pathToConfig);
            LOG.error("Using default configurations.");
            configState = "ERROR";
        } else if(!configFile.exists()) {
            LOG.error("The configuration file {} was not found.  Using defaults", pathToConfig);
            configState = "ERROR";
        } else {

            try {
                reader = new FileReader(configFile);
            } catch (FileNotFoundException e) {
                LOG.error("Config file {} not found: {}.  Using defaults instead.", pathToConfig, e.getMessage());
                configState = "ERROR";
            }
        }

        if ("ERROR".equals(configState)) {
            config = new Configuration();
        } else {
            Gson gson = new Gson();
            config = gson.fromJson(reader, Configuration.class);
        }
        LOG.debug("Configuration: " + config);

        api = new T3KApi(config.getT3k_server_url(), config.getT3k_server_port(),
                config.getNuix_batch_size(), config.getNuix_retry_count(), config.getNuix_retry_seconds());

        exportLocation = config.getNuix_output_path();

        Path pathToExport = Path.of(exportLocation);
        if(!Files.exists(pathToExport)) {
            try {
                Files.createDirectories(pathToExport);
            } catch (IOException e) {
                LOG.error("Path to export data does not exist and can't be created.  This will lead to export errors later.");
            }
        }

        serverSidePath = config.getT3k_server_path();
    }

    public void analyze(List<String> itemsToAnalyze, BlockingQueue<AnalysisResult> completedResults) {
        if(1 == itemsToAnalyze.size()) {

            // single
            SingleItemAnalyzer analyzer = new SingleItemAnalyzer(api, configDirectory, config, analysisListener, resultsListener);

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

        } else if (itemsToAnalyze.size() > 1) {
            LOG.info("Processing batches of items.");

            BatchAnalyzer analyzer = new BatchAnalyzer(api, configDirectory, config, batchListener, resultsListener);

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
                    batchListener.batchCompleted(currentBatchIndex, batchCount, String.format(
                            "Completed Batch %d / %d", currentBatchIndex, batchCount
                    ));
                }
            }

        } else {
            // Empty list, return an empty map.
            LOG.warn("The list of items to analyze is empty.");
        }

        // No matter what, signal the end of analysis when done
        try {
            completedResults.put(END_OF_ANALYSIS);
        } catch (InterruptedException e) {
            LOG.error("Signalling end of analysis (after completed results) was interrupted.");
        }

    }

    public static void main(String[] args)
    {
        //File dataPath = new File(System.getenv("ProgramData"));
        Path dataPath = Path.of(System.getenv("ProgramData"), "Nuix", "Nuix T3K Analysis");
        File configFile = new File(dataPath.toFile(), "t3k_settings.json");
        LOG.debug("Program Data: {} [{}]", configFile.toString(), configFile.exists());

        FileReader reader = null;
        try {
            reader = new FileReader(configFile);
        } catch (FileNotFoundException e) {
            LOG.error(e);
        }

        Configuration config = null;
        if (reader != null)
        {
            config = new Gson().fromJson(reader, Configuration.class);
        } else {
            config = new Configuration();
        }

        String sourcePath = config.getNuix_output_path();
        File sourceDir = new File(sourcePath);
        File[] sourceFiles = sourceDir.listFiles();
        if (null == sourceFiles) sourceFiles = new File[0];
        java.util.List<String> toAnalyze = Arrays.stream(sourceFiles).map(File::getAbsolutePath).collect(Collectors.toList());

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

        AnalysisListener analyzer = new AnalysisListener() {
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
                LOG.error("Analysis Error: {}", message);
            }
        };

            Application app = new Application(configFile.getAbsolutePath());
            app.setBatchListener(batcher);
            app.setAnalysisListener(analyzer);

            //String file = "C:\\Projects\\ProServ\\T3K\\Data\\processing\\71f218c4-9bdd-4701-8576-634eaccc1a86.jpg";
            //List<String> toAnalyze = List.of(file);
            BlockingQueue<AnalysisResult> completedAnalysis = new LinkedBlockingQueue<>();

            app.analyze(toAnalyze, completedAnalysis);

            processResults(completedAnalysis);

    }

    private static void processResults(BlockingQueue<AnalysisResult> results)
    {

        while(true) {
            AnalysisResult currentResult = null;

            try {
                currentResult = results.take();
            } catch (InterruptedException e) {
                LOG.info("Getting the next item from the queue was interrupted.  Trying again.");
                continue;
            }

            if (Application.END_OF_ANALYSIS.equals(currentResult)) {
                LOG.trace("Came to the end of the queue, exiting work.");
                break;
            }

            String filePath = currentResult.getMetadata().getFile_path();
            String fileName = Path.of(filePath).getFileName().toString();
            LOG.info("{} [{}]: Found {} things.", currentResult.getMetadata().getId(), fileName, currentResult.getDetectionCount());
            currentResult.forEachDetection(LOG::info);
        }
    }

    public static final AnalysisResult END_OF_ANALYSIS = new AnalysisResult() {
        class EmptyMetadata extends ResultMetadata {}

        final ResultMetadata metadata = new EmptyMetadata();

        @Override
        public ResultMetadata getMetadata() {
            return metadata;
        }
    };

}
