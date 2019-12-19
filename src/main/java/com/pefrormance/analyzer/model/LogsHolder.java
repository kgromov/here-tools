package com.pefrormance.analyzer.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pefrormance.analyzer.service.ResultsExporter;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

public class LogsHolder {
    private static final Function<Path, String> PATH_TO_REGION = path ->
    {
        String fileName = path.getFileName().toString();
        int startIndex = fileName.indexOf('_');
        int endIndex = fileName.indexOf("build_number");
        return startIndex!= -1 && endIndex != -1 ? fileName.substring(startIndex + 1, endIndex - 1) : fileName;
    };

    private final String product;

    public LogsHolder(String product) {
        this.product = product;
    }

    // or String outputDir
    public void bypassLogs(Settings settings) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);
        // TODO: or move outside?
        ResultsExporter exporter = new ResultsExporter(product, settings.getMap1Name(), settings.getMap2Name());

        Map<String, List<Path>> logsPerRegion = Files.find(settings.getLogsFolder(),
                Short.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile()
                        && filePath.getFileName().toString().startsWith(product)
                        && filePath.toString().endsWith(".json"))
                .sorted(Comparator.comparing(o -> o.getFileName().toString()))
                .collect(Collectors.groupingBy(PATH_TO_REGION));

        Map<String, Pair<Path, Path>> logPairsPerRegion = logsPerRegion.entrySet().stream()
                .filter(e -> e.getValue().size() == 2)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Pair.of(e.getValue().get(0), e.getValue().get(1))));

        if (!logPairsPerRegion.isEmpty())
        {
            exporter.init(settings.getResultsFolder());
        }

//        logPairsPerRegion.forEach((region, logs) ->
        logPairsPerRegion.entrySet().stream().parallel().forEach(entry ->
        {
            String region = entry.getKey();
            Pair<Path, Path> logs = entry.getValue();
            System.out.println("\nProcess region = " + region);
            long start = System.nanoTime();
            try {
                // TODO: parallel
                TasksHolder tasksHolderMap1 = new TasksHolder(region);
                tasksHolderMap1.setTasks(logs.getLeft().toFile(), mapper);

                TasksHolder tasksHolderMap2 = new TasksHolder(region);
                tasksHolderMap2.setTasks(logs.getRight().toFile(), mapper);

                exporter.exportRegion(region, tasksHolderMap1, tasksHolderMap2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                System.out.println(String.format("Json + DB converters = %d ms", TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS)));
            }
        });
    }

}
