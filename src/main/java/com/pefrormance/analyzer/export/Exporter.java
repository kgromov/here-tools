package com.pefrormance.analyzer.export;

import com.pefrormance.analyzer.model.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by konstantin on 22.12.2019.
 */
public interface Exporter
{
    List<String> HEADERS = Arrays.asList("UpdateRegion", "Details");

    // TODO: refactor this shit: with setter or getter
    default void init(String product, Settings settings) throws IOException {
        Path regionDbFile = getResultFile(product, settings);
        Files.createDirectories(settings.getResultsFolder());
        Files.deleteIfExists(regionDbFile);
        Files.createFile(regionDbFile);
    }

    static Path getResultFile(String product, Settings settings)
    {
        return settings.getResultsFolder().resolve(product + "_AGGREGATED_LOG" + settings.getOutputFormat().getFormat());
    }

    void export (Collection<String> data) throws IOException;
}
