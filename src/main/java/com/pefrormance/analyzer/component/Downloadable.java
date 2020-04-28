package com.pefrormance.analyzer.component;

import com.pefrormance.analyzer.service.AwsCommand;

import java.util.concurrent.CompletableFuture;

/**
 * Created by konstantin on 27.04.2020.
 */
public interface Downloadable {
    CompletableFuture<Void> download(AwsCommand command);
}
