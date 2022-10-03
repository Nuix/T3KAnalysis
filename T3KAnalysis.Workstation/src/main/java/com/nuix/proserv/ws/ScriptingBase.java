package com.nuix.proserv.ws;

import com.nuix.proserv.t3k.conn.Application;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.gson.Gson;

import nuix.Utilities;
import nuix.Item;
import nuix.SingleItemExporter;

import com.nuix.proserv.t3k.conn.config.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class ScriptingBase {
    private static final Logger LOG = LogManager.getLogger(ScriptingBase.class.getCanonicalName());

    @Getter
    private final Utilities utilities;

    @Getter
    private final Configuration config;

    @Getter
    private final Application app;

    private final String pathToConfig;


    public ScriptingBase(Utilities utilities, String pathToConfig) {
        this.utilities = utilities;
        this.pathToConfig = pathToConfig;

        this.app = new Application(pathToConfig);
        this.config = app.getConfig();

    }

    public List<String> exportItems(List<Item> itemsToExport, ProgressListener listener) {
        LOG.trace("Exporting items: itemsToExport");
        SingleItemExporter exporter = getUtilities().getBinaryExporter();

        List<Item> exportList = List.copyOf(itemsToExport);

        List<String> exportedItems = new ArrayList<>();
        final int itemCount = itemsToExport.size();
        int currentIndex = 0;
        listener.updateProgress(0, itemCount, "Beginning Export Process");

        for(Item item : exportList) {
            LOG.trace("Exporting {}", item.getName());
            currentIndex++;
            String name = item.getName();
            String guid = item.getGuid();
            String ext = item.getCorrectedExtension();

            String fileName = guid + "." + ext;
            String outputPath = Path.of(getConfig().getNuix_output_path(), fileName).toString();
            try {
                exporter.exportItem(item, outputPath);
                exportedItems.add(outputPath);
            } catch (IOException e) {
                LOG.error("Exception exporting {}: {}", name, e.getMessage());
                LOG.error(e);
            }

            listener.updateProgress(currentIndex, itemCount, String.format(
                    "Exported %s as %s", name, fileName
            ));
        }

        return exportedItems;
    }

    public void analyze(List<String> filesToAnalyze) {

    }
}
