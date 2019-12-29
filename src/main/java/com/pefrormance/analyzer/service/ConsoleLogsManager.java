package com.pefrormance.analyzer.service;

import com.pefrormance.analyzer.export.OutputFormat;
import com.pefrormance.analyzer.export.SqliteExporter;
import com.pefrormance.analyzer.model.LogFile;
import com.pefrormance.analyzer.model.Settings;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        long start = System.nanoTime();
        try
        {
            String outputDir = settings.getLogsFolder().toString();

            SqliteExporter exporter = new SqliteExporter(settings);
            exporter.init(settings);

            settings.getProducts().stream().parallel().forEach(product ->
            {
                try {
                    // download
//                    new Downloader(product, settings.getUpdateRegion(), settings.getMapPath(), outputDir).download(System.out::println);
                    // bypass
                    ConsoleLogsParser parser = new ConsoleLogsParser(product);
                    List<String> outputData =  parser.bypass(settings);
                    // export
                    exporter.export(product, outputData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
//            exporter.vacuum();
        } catch (IOException e) {
            e.printStackTrace();
        } finally
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

    public static void main(String[] args) {
        Set<String> products = new HashSet<>(Arrays.asList("LC", "FB", "WOM"));

        Settings settings = new Settings.Builder()
                .product(products)
//                .updateRegion(updateRegion.getText())
                .mapPath("s3://akela-artifacts/Akela-191E3/CMP-0e8a4f7/logs/")
                .expressionToFind("Found a not simple geometry")
                .logFile(LogFile.WARN)
                .outputFormat(OutputFormat.SQ3)
                .outputDir("C:\\Projects\\here-tools\\out")
                .build();

        ConsoleLogsManager manager = new ConsoleLogsManager(settings);
        manager.run();
        manager.removeLogFiles();
    }
}
