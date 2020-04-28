package com.pefrormance.analyzer.component;

import com.pefrormance.analyzer.service.AwsCommand;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Created by konstantin on 27.04.2020.
 */
@Slf4j
@Component("dummy")
public class DummyDownloader implements Downloadable {

    @Override
    @Async
    @SneakyThrows
    public CompletableFuture<Void> download(AwsCommand command) {
        log.info("Download files by aws command: {}", command);
        Thread.sleep(10000L);
        return null;
    }
}
