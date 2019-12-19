package com.pefrormance.analyzer.service;


import com.pefrormance.analyzer.model.LogsHolder;
import com.pefrormance.analyzer.model.Settings;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TasksTimeAnalyzer {

    private final Settings settings;

    public TasksTimeAnalyzer(Settings settings) {
        this.settings = settings;
    }

    public void run() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        long start = System.nanoTime();
        try {
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
                    Thread.sleep(500);
                } catch (Exception e) {

                }
            });
        } finally {
            executor.shutdown();
            System.out.println(String.format("Time elapsed = %d s", TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS)));

        }
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

    public static void main(String[] args) throws IOException {
        Settings settings = new Settings.Builder()
//                    .product(new HashSet<>(Arrays.asList("LC", "FB", "3D", "WOM", "FTS")))
                .product(new HashSet<>(Arrays.asList("LC")))
//                    .updateRegion("DEU_G8")//
                .map1Path("s3://akela-artifacts/Akela-191E3/CMP-e88110d/logs")
                /*.map2Path("s3://akela-artifacts/Akela-19135/CMP-0c3fe8b/logs")
                .map1Path("s3://akela-artifacts/Akela-19135/CMP-1430c6d/logs/")*/
                .map2Path("s3://akela-artifacts/Akela-191E3/CMP-0e8a4f7/logs/")
                .outputDir("C:\\HERE-CARDS\\my_dev_presubmits\\trash\\demo")
                .build();
        TasksTimeAnalyzer analyzer = new TasksTimeAnalyzer(settings);
        analyzer.run();
        analyzer.removeLogFiles();
    }
}
