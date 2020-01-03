package com.pefrormance.analyzer.service;


import com.pefrormance.analyzer.model.LogsHolder;
import com.pefrormance.analyzer.model.Settings;
import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TasksTimeAnalyzer extends Task<Void>{
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
            String outputDir = settings.getLogsFolder().toString();
            settings.getProducts().stream().parallel().forEach(product ->
            {
                try {
                    List<Future<?>> results = new ArrayList<>(2);
                    results.add(executor.submit(() -> {
                        try {
                            new Downloader(product, settings.getUpdateRegion(), settings.getMap1Path(), outputDir).download(System.out::println);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }));
                    results.add(executor.submit(() -> {
                        try {
                            new Downloader(product, settings.getUpdateRegion(), settings.getMap2Path(), outputDir).download(System.out::println);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }));
                    results.forEach(r -> {
                        try {
                            r.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    });

                    LogsHolder logsHolder = new LogsHolder(product);
                    logsHolder.bypassLogs(settings);
                    progress.incrementAndGet();
                    updateProgress(progress.get(), settings.getProducts().size());
                    updateMessage("Progress: " + FORMAT.format(progress.doubleValue() / settings.getProducts().size()));
                } catch (Exception e) {

                }
            });
        } finally {
            executor.shutdown();
            removeLogFiles();
            System.out.println(String.format("Time elapsed = %d s", TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS)));

        }
        return null;
    }

    public void removeLogFiles() {
        try
        {
            Files.walkFileTree(settings.getLogsFolder(), new SimpleFileVisitor<Path>() {
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

        }
    }
}
