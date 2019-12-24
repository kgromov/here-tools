package com.pefrormance.analyzer.service;

import com.pefrormance.analyzer.model.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ConsoleLogsParser {

    private static final Function<File, String> FILE_TO_REGION = path ->
    {
        String fileName = path.getName();
        int startIndex = fileName.indexOf('_');
        int endIndex = fileName.indexOf("build_number");
        return startIndex!= -1 && endIndex != -1 ? fileName.substring(startIndex + 1, endIndex - 1) : fileName;
    };

    private final String product;

    public ConsoleLogsParser(String product) {
        this.product = product;
    }

    public List<String> bypass(Settings settings) throws IOException {
        List<String> lines = new ArrayList<>();
        AtomicLong count = new AtomicLong();
        String logLevel = settings.getLogFile().getLogLevel();
        String filSuffix = settings.getLogFile().getFileName();
        String expression = settings.getExpressionToFind();

        Files.find(settings.getLogsFolder(),
                Short.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile()
                        && filePath.getFileName().toString().startsWith(product)
                        && filePath.getFileName().toString().contains("console_log"))
                .map(Path::toFile)
                .sorted()
                .parallel()
                .forEach(file ->
                {
                    System.out.println("Processing " + file.getName() + " file.");
                    String uRName = FILE_TO_REGION.apply(file);
                    try (FileInputStream fileInputStream = new FileInputStream(file);
                         ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
                         Scanner in = new Scanner(zipInputStream))  // replace Scanner to more adequate class
                    {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                            if (zipEntry.getName().contains(filSuffix)) {
                                break;
                            }
                        }

                        while (in.hasNext()) {
                            String line = in.nextLine();
                            if (line.contains(logLevel) && line.contains(expression)) {
                                String data = uRName + "," + line;
                                System.out.println(data);
                                synchronized (lines) {
                                    lines.add(data);
                                }
                            }
                            count.incrementAndGet();
                            if (count.get() > 1L && count.get() % 10000L == 0L) {
                                System.out.println("Processed " + count + " rows.");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return lines;
    }
}
