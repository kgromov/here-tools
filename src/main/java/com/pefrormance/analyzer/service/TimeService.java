package com.pefrormance.analyzer.service;

import com.pefrormance.analyzer.component.Downloadable;
import com.pefrormance.analyzer.component.FileBypasser;
import com.pefrormance.analyzer.config.InjectedSettings;
import com.pefrormance.analyzer.old_stuff.model.ResultRowBean;
import com.pefrormance.analyzer.repository.ResultsExporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by konstantin on 27.04.2020.
 */
@Slf4j
@Service
public class TimeService {
    private final InjectedSettings settings;
    private final Downloadable downloader;
    private final ResultsExporter exporter;
    private final LogsHolder logsHolder;
    private final FileBypasser fileBypasser;

    public TimeService(InjectedSettings settings, @Qualifier("dummy") Downloadable downloader,
                       ResultsExporter exporter, LogsHolder logsHolder, FileBypasser fileBypasser) {
        this.settings = settings;
        this.downloader = downloader;
        this.exporter = exporter;
        this.logsHolder = logsHolder;
        this.fileBypasser = fileBypasser;
    }

    @Async
    public void call() {
        long start = System.nanoTime();
        try {
            settings.getProducts().stream().parallel().forEach(product ->
            {
                log.info("Start processing product = " + product);
                AwsCommand command1 = new AwsCommand.Builder()
                        .product(product.getName())
                        .updateRegion(settings.getUpdateRegion())
                        .sourcePath(settings.getMap1Path())
                        .destPath(settings.getOutputDir())
                        .build();
                AwsCommand command2 = new AwsCommand.Builder()
                        .product(product.getName())
                        .updateRegion(settings.getUpdateRegion())
                        .sourcePath(settings.getMap2Path())
                        .destPath(settings.getOutputDir())
                        .build();
                CompletableFuture.allOf(downloader.download(command1), downloader.download(command2)).join();
                log.info("Finish downloading files for product = " + product);

                Map<String, Collection<ResultRowBean>> data = logsHolder.bypassLogs(product.getName());
                exporter.export(product, data);
                log.info("Finish processing product = " + product);
            });
            exporter.vacuum();
        } finally {
            fileBypasser.removeLogFiles();
            log.info(String.format("Time elapsed = %d s", TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS)));
        }
    }
}
