package com.pefrormance.analyzer.component;

import com.pefrormance.analyzer.service.AwsCommand;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Created by konstantin on 27.04.2020.
 */
@Slf4j
@Component("aws")
public class AwsDownloader implements Downloadable {

    @Override
    @Async
    @SneakyThrows
    public CompletableFuture<Void> download(AwsCommand command) {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command.getCommand());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream())))
        {
            String s;
            while ((s = stdInput.readLine()) != null) {
                log.debug(s);
            }
        }
        return null;
    }
}
