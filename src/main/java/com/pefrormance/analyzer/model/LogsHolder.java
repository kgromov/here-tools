package com.pefrormance.analyzer.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pefrormance.analyzer.export.RegionResultsExporter;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

public class LogsHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogsHolder.class);

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

    public Map<String, Collection<ResultRowBean>> bypassLogs(Settings settings) throws IOException
    {
        Map<String, Collection<ResultRowBean>> data = new TreeMap<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);

//        RegionResultsExporter exporter = new RegionResultsExporter(product, settings.getMap1Name(), settings.getMap2Name());

        Map<String, List<Path>> logsPerRegion = Files.find(settings.getDataFolder(),
                Short.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile()
                        && filePath.getFileName().toString().startsWith(product)
                        && filePath.toString().endsWith(".json"))
                .sorted(Comparator.comparing(o -> o.getFileName().toString()))
                .collect(Collectors.groupingBy(PATH_TO_REGION));

        Map<String, Pair<Path, Path>> logPairsPerRegion = logsPerRegion.entrySet().stream()
                .filter(e -> e.getValue().size() == 2)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Pair.of(e.getValue().get(0), e.getValue().get(1))));

        /*if (!logPairsPerRegion.isEmpty())
        {
            exporter.init(settings.getResultsFolder());
        }*/

//        logPairsPerRegion.forEach((region, logs) ->
        logPairsPerRegion.entrySet().stream().parallel().forEach(entry ->
        {
            String region = entry.getKey();
            Pair<Path, Path> logs = entry.getValue();
            LOGGER.debug("Start processing region = " + region);
            long start = System.nanoTime();
            try {
                TasksHolder tasksHolderMap1 = new TasksHolder(region);
                tasksHolderMap1.setTasks(logs.getLeft().toFile(), mapper);

                TasksHolder tasksHolderMap2 = new TasksHolder(region);
                tasksHolderMap2.setTasks(logs.getRight().toFile(), mapper);

                synchronized (LogsHolder.class)
                {
                    data.put(region, getResultsPerRegion(tasksHolderMap1, tasksHolderMap2));
                }
//                exporter.exportRegion(region, tasksHolderMap1, tasksHolderMap2);
                LOGGER.debug("Finish processing region = " + region);
            } catch (IOException e) {
                LOGGER.error("Error occurred while processing region = " + region, e);
            }
            finally {
                LOGGER.debug(String.format("Json + DB converters = %d ms", TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS)));
            }
        });
        return data;
    }

    private Collection<ResultRowBean> getResultsPerRegion(TasksHolder first, TasksHolder second)
    {
        List<ResultRowBean> data = new ArrayList<>();
        Map<String, Double> tasksFirst =  first.getTasks().stream()
                .collect(Collectors.toMap(TaskBean::getName, TaskBean::getDuration));

        Map<String, Double> tasksSecond =  second.getTasks().stream()
                .collect(Collectors.toMap(TaskBean::getName, TaskBean::getDuration));

        Set<String> allTaskNames = new HashSet<>(tasksFirst.keySet());
        allTaskNames.addAll(tasksSecond.keySet());

        for (String taskName : allTaskNames)
        {
            ResultRowBean rowBean = new ResultRowBean(taskName, tasksFirst.get(taskName), tasksSecond.get(taskName));
            data.add(rowBean);
        }
        // total
        ResultRowBean rowBean = new ResultRowBean("Total", first.getTotalTime().sum(), second.getTotalTime().sum());
        data.add(rowBean);
        return data;
    }

}
