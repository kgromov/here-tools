package com.pefrormance.analyzer.service;

import com.pefrormance.analyzer.model.LogFile;
import com.pefrormance.analyzer.model.ResultRow;
import com.pefrormance.analyzer.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
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
        LogFile logFile = settings.getLogFile();
        String filSuffix = settings.getLogFile().getFileName();
        String expression = settings.getExpressionToFind();

        LOGGER.info("Start processing product = " + product);
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
                    List<ResultRow> rowsPerRegion = new ArrayList<>();
                    String updateRegion = FILE_TO_REGION.apply(file);
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
                            Optional<String> logLevel = getLogLevelFromRow(line, logFile);
                            if (logLevel.isPresent() && line.contains(expression)) {
                                String [] messages = line.split(AKELA_LOG_DELIMITER);
                                String message = messages.length > 1
                                        ? messages[1]
                                        : line.substring(line.indexOf(logLevel.get() + logLevel.get() + 1));
                                ResultRow row = new ResultRow(updateRegion, logLevel.get(), message);
                                rowsPerRegion.add(row);
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
                    if (!rowsPerRegion.isEmpty()) {
                        synchronized (lines)
                        {
                            lines.addAll(rowsPerRegion);
                        }
                    }
                });
        LOGGER.info("Finish processing product = " + product);
        return lines;
    }


    private static final Pattern LOG_LEVEL_PATTERN = Pattern.compile(getLogLevelRegularExpression());

    // TODO: change to pair
    private static String getLogLevelRegularExpression()
    {
        return Stream.of(LogFile.ERROR, LogFile.WARN, LogFile.INFO, LogFile.DEBUG,LogFile.TRACE)
                .map(Enum::name)
                .collect(Collectors.joining(")|(", "$\\w+(", ")\\w+"));
    }

    // TODO: probably add as regExp part or even combine regExp into 1?
    private static final String AKELA_LOG_DELIMITER = " -> ";

    private Optional<String> getLogLevelFromRow(String message, LogFile logFile)
    {
        Matcher matcher = LOG_LEVEL_PATTERN.matcher(message);
        boolean isFound = matcher.find();
        if (isFound && logFile != LogFile.ANY)
        {
            return Optional.of(logFile.getLogLevel());
        }
        else if (isFound)
        {
            return Optional.of(matcher.group());
        }
        return Optional.empty();
    }
}
