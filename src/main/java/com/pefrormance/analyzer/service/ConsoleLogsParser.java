package com.pefrormance.analyzer.service;

import com.pefrormance.analyzer.model.Settings;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ConsoleLogsParser {

    private final String product;

    public ConsoleLogsParser(String product) {
        this.product = product;
    }

    public List<String> bypass(Settings settings) throws IOException {
        List<String> lines = new ArrayList<>();
        AtomicLong count = new AtomicLong();

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
                    String uRName = file.getName().toUpperCase();
                    uRName = uRName.substring(0, uRName.toUpperCase().indexOf("_BUILD_NUMBER_"));
                    try (FileInputStream fileInputStream = new FileInputStream(file);
                         ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
                         Scanner in = new Scanner(zipInputStream))  // replace Scanner to more adequate class
                    {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                            if (zipEntry.getName().contains("error.log")) {
                                break;
                            }
                        }

                        while (in.hasNext()) {
                            String line = in.nextLine();
                            // TODO: should be replaced to regular expression
                            if (line.contains(errorToFind) && line.contains(errorToFind2)) {
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
