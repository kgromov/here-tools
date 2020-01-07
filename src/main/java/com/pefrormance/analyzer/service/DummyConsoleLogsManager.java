package com.pefrormance.analyzer.service;

import com.pefrormance.analyzer.export.OutputFormat;
import com.pefrormance.analyzer.export.SqliteExporter;
import com.pefrormance.analyzer.model.LogFile;
import com.pefrormance.analyzer.model.Product;
import com.pefrormance.analyzer.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by konstantin on 22.12.2019.
 */
public class DummyConsoleLogsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyConsoleLogsManager.class);

    private final Settings settings;

    public DummyConsoleLogsManager(Settings settings) {
        this.settings = settings;
    }

    public void run() {
        long start = System.nanoTime();
        try
        {
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
                    List<String> outputData =  parser.bypass(settings);
                    // export
                    exporter.export(product, outputData);
                    LOGGER.info("Finish processing product = " + product);
                } catch (Exception e) {
                    LOGGER.error(String.format("Error occurred while processing %s, product = %s", settings.getMapPath(), product), e);
                }
            });
            exporter.vacuum();
        }  finally
        {
            LOGGER.info(String.format("Time elapsed = %d s", TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS)));
        }
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

    public static void main(String[] args) {
        String [] productNames = {"LC", "FB", "WOM"};
        Set<Product> products = Arrays.stream(productNames).map(Product::getProductByName).collect(Collectors.toSet());

        Settings settings = new Settings.Builder()
                .product(products)
//                .updateRegion(updateRegion.getText())
                .mapPath("s3://akela-artifacts/Akela-191E3/CMP-0e8a4f7/logs/")
                .expressionToFind("Found a not simple geometry")
                .logFile(LogFile.WARN)
                .outputFormat(OutputFormat.SQ3)
                .outputDir("C:\\Projects\\here-tools\\out")
                .build();

        DummyConsoleLogsManager manager = new DummyConsoleLogsManager(settings);
        manager.run();
        manager.removeLogFiles();
    }
}
