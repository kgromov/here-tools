package com.pefrormance.analyzer.service;

import com.pefrormance.analyzer.export.SqliteExporter;
import com.pefrormance.analyzer.model.ResultRow;
import com.pefrormance.analyzer.model.Settings;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by konstantin on 22.12.2019.
 */
public class ConsoleLogsManager extends Task<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleLogsManager.class);
    private static final NumberFormat FORMAT = NumberFormat.getPercentInstance();
    private final Settings settings;

    public ConsoleLogsManager(Settings settings) {
        this.settings = settings;
    }

    @Override
    public Void call() {
        long start = System.nanoTime();
        AtomicInteger progress = new AtomicInteger();
        try
        {
            updateProgress(progress.get(), settings.getProducts().size());
            updateMessage("Progress: " + FORMAT.format(progress.doubleValue() / settings.getProducts().size()));
            String outputDir = settings.getDataFolder().toString();

            SqliteExporter exporter = new SqliteExporter(settings);
            exporter.init(settings);

            settings.getProducts().stream().parallel().forEach(product ->
            {
                LOGGER.info("Start processing product = " + product);
                try {
                    LOGGER.info("Start downloading files for product = " + product);
                    // download
                    new Downloader(product.getName(), settings.getUpdateRegion(), settings.getMapPath(), outputDir).download(LOGGER::debug);
                    LOGGER.info("Finish downloading files for product = " + product);
                    // bypass
                    ConsoleLogsParser parser = new ConsoleLogsParser(product.getName());
                    List<ResultRow> outputData =  parser.bypass(settings);
                    // export
                    exporter.export(product, outputData);

                    progress.incrementAndGet();
                    updateProgress(progress.get(), settings.getProducts().size());
                    updateMessage("Progress: " + FORMAT.format(progress.doubleValue() / settings.getProducts().size()));
                    LOGGER.info("Finish processing product = " + product);
                } catch (Exception e) {
                    LOGGER.error(String.format("Error occurred while processing %s, product = %s", settings.getMapPath(), product), e);
                }
            });
            exporter.vacuum();
        }  finally
        {
            removeLogFiles();
            LOGGER.info(String.format("Time elapsed = %d s", TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS)));
        }
        return null;
    }

    public void removeLogFiles() {
        try {
            LOGGER.debug(String.format("Remove collected data from: %s", settings.getDataFolder()));
            Files.walkFileTree(settings.getDataFolder(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("Unable to clear temporary data folder:", e);
        }
    }
}
