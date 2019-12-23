package com.pefrormance.analyzer.service;

import com.pefrormance.analyzer.export.Exporter;
import com.pefrormance.analyzer.model.Settings;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by konstantin on 22.12.2019.
 */
public class ConsoleLogsManager {

    private final Settings settings;


    public ConsoleLogsManager(Settings settings) {
        this.settings = settings;
    }

    public void run() {
        // intermediate layer for product:
        // 1) find files;
        // 2) split to stripes/FJPool or other strategy
        // Introduce 1 more layer or process all files per product;
        // Export

        long start = System.nanoTime();
        try
        {
            String outputDir = settings.getLogsFolder().toString();
            settings.getProducts().stream().parallel().forEach(product ->
            {
                try {
                    // download
                    new Downloader(product, settings.getUpdateRegion(), settings.getMapPath(), outputDir).download(System.out::println);
                    // bypass
                    ConsoleLogsParser parser = new ConsoleLogsParser(product);
                    List<String> outputData =  parser.bypass(settings);
                    // export
                    Exporter exporter = settings.getOutputFormat().getExporter();
                    exporter.init();
                    // TODO: logic for empty data?
                    exporter.export(outputData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        finally
        {
            System.out.println(String.format("Time elapsed = %d s", TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS)));

        }
    }

    public void removeLogFiles() {
        try {
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
        } catch (IOException e) {

        }
    }
}
