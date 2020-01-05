package com.pefrormance.analyzer.service;


import com.pefrormance.analyzer.export.AggregatedResultsExporter;
import com.pefrormance.analyzer.model.LogsHolder;
import com.pefrormance.analyzer.model.ResultRowBean;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TasksTimeAnalyzer extends Task<Void>{
    private static final Logger LOGGER = LoggerFactory.getLogger(TasksTimeAnalyzer.class);
    private static final NumberFormat FORMAT = NumberFormat.getPercentInstance();
    private final Settings settings;

    public TasksTimeAnalyzer(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected Void call() {
        AtomicInteger progress = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        long start = System.nanoTime();
        try {
            updateProgress(progress.get(), settings.getProducts().size());
            updateMessage("Progress: " + FORMAT.format(progress.doubleValue() / settings.getProducts().size()));
            String outputDir = settings.getDataFolder().toString();

            AggregatedResultsExporter exporter = new AggregatedResultsExporter(settings);
            exporter.init(settings.getResultsFolder());

            settings.getProducts().stream().parallel().forEach(product ->
            {
                LOGGER.info("Start processing product = " + product);
                try {
                    LOGGER.info("Start downloading files for product = " + product);
                    List<Future<?>> results = new ArrayList<>(2);
                    results.add(executor.submit(() -> {
                        try {
                            new Downloader(product, settings.getUpdateRegion(), settings.getMap1Path(), outputDir).download(LOGGER::debug);
                        } catch (Exception e) {
                            LOGGER.error(String.format("Error occurred while processing %s, product = %s", settings.getMap1Path(), product), e);
                        }
                    }));
                    results.add(executor.submit(() -> {
                        try {
                            new Downloader(product, settings.getUpdateRegion(), settings.getMap2Path(), outputDir).download(LOGGER::debug);
                        } catch (Exception e) {
                            LOGGER.error(String.format("Error occurred while processing %s, product = %s", settings.getMap2Path(), product), e);
                        }
                    }));
                    results.forEach(r -> {
                        try {
                            r.get();
                        } catch (InterruptedException | ExecutionException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    LOGGER.info("Finish downloading files for product = " + product);

                    LogsHolder logsHolder = new LogsHolder(product);
                    Map<String, Collection<ResultRowBean>> data = logsHolder.bypassLogs(settings);
                    exporter.export(product, data);

                    progress.incrementAndGet();
                    updateProgress(progress.get(), settings.getProducts().size());
                    updateMessage("Progress: " + FORMAT.format(progress.doubleValue() / settings.getProducts().size()));
                    LOGGER.info("Finish processing product = " + product);
                } catch (Exception e) {
                    LOGGER.error(String.format("Unable to collect data for product = %s", product), e);
                }
            });
            exporter.vacuum();
        } finally {
            executor.shutdown();
            removeLogFiles();
            LOGGER.info(String.format("Time elapsed = %d s", TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS)));
        }
        return null;
    }

    public void removeLogFiles() {
        try
        {
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
        }
        catch (IOException e)
        {
            LOGGER.error("Unable to clear temporary data folder:", e);
        }
    }
}
