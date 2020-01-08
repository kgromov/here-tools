package com.pefrormance.analyzer.service;

import com.pefrormance.analyzer.model.LogFile;
import com.pefrormance.analyzer.model.ResultRow;
import com.pefrormance.analyzer.model.Settings;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ConsoleLogsParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleLogsParser.class);

    private static final Function<File, String> FILE_TO_REGION = path ->
    {
        String fileName = path.getName();
        int startIndex = fileName.indexOf('_');
        int endIndex = fileName.indexOf("build_number");
        return startIndex != -1 && endIndex != -1 ? fileName.substring(startIndex + 1, endIndex - 1) : fileName;
    };

    private final String product;

    public ConsoleLogsParser(String product) {
        this.product = product;
    }

    public List<ResultRow> bypass(Settings settings) throws IOException {
        List<ResultRow> lines = new ArrayList<>();
        AtomicLong count = new AtomicLong();
        String logLevel = settings.getLogFile().getLogLevel();
        String filSuffix = settings.getLogFile().getFileName();
        String expression = settings.getExpressionToFind();

        LOGGER.info("Start processing product = " + product);
        // TODO: if split to HD/Akela - should be changed: {Akela - console_log, HD = HD_LOGS}
        Files.find(settings.getDataFolder(),
                Short.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile()
                        && filePath.getFileName().toString().startsWith(product)
                        && filePath.getFileName().toString().contains("console_log"))
                .map(Path::toFile)
                .sorted()
                .parallel()
                .forEach(file ->
                {
                    LOGGER.debug("Processing " + file.getName() + " file.");
                    Map<Pair<String, String>, AtomicInteger> messages = new LinkedHashMap<>();
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
                                // TODO:
                                // 1) retrieve logLevel for ANY
                                // 2) retrieve message
                                messages.computeIfAbsent(Pair.of(logLevel, line), value -> new AtomicInteger()).incrementAndGet();
//                                ResultRow row =  new ResultRow(uRName, line);
//                                LOGGER.debug(row.toString());
                                /*synchronized (lines) {
                                    lines.add(row);
                                }*/
                            }
                            count.incrementAndGet();
                            if (count.get() > 1L && count.get() % 10000L == 0L) {
                                LOGGER.debug("Processed " + count + " rows.");
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error occurred while processing " + file, e);
                    }
                    // aggregate
                    if (!messages.isEmpty()) {
                        synchronized (lines)
                        {
                            messages.entrySet().stream()
                                    .map(e -> new ResultRow(uRName, e.getKey().getLeft(), e.getKey().getRight(), e.getValue().get()))
                                    .forEach(lines::add);
                        }
                    }
                });
        LOGGER.info("Finish processing product = " + product);
        return lines;
    }


    private static final Pattern LOG_LEVEL_PATTERN = Pattern.compile(getLogLevelRegularExpression());

    private static String getLogLevelRegularExpression()
    {
        return Stream.of(LogFile.ERROR, LogFile.WARN, LogFile.INFO, LogFile.DEBUG,LogFile.TRACE)
                .map(Enum::name)
                .collect(Collectors.joining(")|(", "$\\w+(", ")\\w+"));
    }

    private static final String AKELA_LOG_DELIMITER = " -> ";

    private String getLogLevelFromRow(String message, LogFile logFile)
    {
        String logLevel = logFile == LogFile.ANY ? getLogLevelFromRow(message, logFile) : logFile.name();
        Matcher matcher = LOG_LEVEL_PATTERN.matcher(message);
        if (matcher.find())
        {
            return matcher.group();
        }
        return logFile.name();
    }
}
