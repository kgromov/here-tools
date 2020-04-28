package com.pefrormance.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pefrormance.analyzer.component.FileBypasser;
import com.pefrormance.analyzer.config.InjectedSettings;
import com.pefrormance.analyzer.old_stuff.model.ResultRowBean;
import com.pefrormance.analyzer.old_stuff.model.TaskBean;
import com.pefrormance.analyzer.old_stuff.model.TasksHolder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogsHolder {
    private final InjectedSettings settings;
    private final FileBypasser fileBypasser;

    @SneakyThrows
    public Map<String, Collection<ResultRowBean>> bypassLogs(String product)
    {
        Map<String, Collection<ResultRowBean>> data = new TreeMap<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);

        Map<String, Pair<Path, Path>> logPairsPerRegion = fileBypasser.aggregateLogPerRegion(product);
        logPairsPerRegion.entrySet().stream().parallel().forEach(entry ->
        {
            String region = entry.getKey();
            Pair<Path, Path> logs = entry.getValue();
            log.debug("Start processing region = " + region);
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
                log.debug("Finish processing region = " + region);
            } catch (IOException e) {
                log.error("Error occurred while processing region = " + region, e);
            }
            finally {
                log.debug(String.format("Json + DB converters = %d ms", TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS)));
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
