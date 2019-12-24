package com.pefrormance.analyzer.export;

import com.pefrormance.analyzer.model.Settings;

public interface ExporterBuilder {

    ExporterBuilder product(String product);

    ExporterBuilder settings(Settings settings);

    Exporter build();

}
