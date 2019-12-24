package com.pefrormance.analyzer.export;


import com.pefrormance.analyzer.model.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by konstantin on 22.12.2019.
 */
public class CsvExporter implements Exporter {
    private  final String resultFile;

    private CsvExporter(CsvExporterBuilder builder) {
        this.resultFile = Exporter.getResultFile(builder.product, builder.settings).toString();
    }

    @Override
    public void export(Collection<String> data) throws IOException {
        String header = HEADERS.stream().collect(Collectors.joining(","));
        List<String> lines = new ArrayList<>();
        lines.add(header);
        lines.addAll(data);
        Files.write(Paths.get(resultFile), lines);
    }

    public static final class CsvExporterBuilder implements ExporterBuilder
    {
        private String product;
        private Settings settings;

        @Override
        public ExporterBuilder product(String product) {
            this.product = product;
            return this;
        }

        @Override
        public ExporterBuilder settings(Settings settings) {
            this.settings = settings;
            return this;
        }

        @Override
        public Exporter build() {
            return new CsvExporter(this);
        }
    }
}
