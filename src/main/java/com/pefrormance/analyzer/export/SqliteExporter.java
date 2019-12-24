package com.pefrormance.analyzer.export;

import com.pefrormance.analyzer.model.Settings;

import java.util.Collection;

/**
 * Created by konstantin on 22.12.2019.
 */
public class SqliteExporter implements Exporter {
    private  final String resultFile;

    private SqliteExporter(SqliteExporterBuilder builder) {
        this.resultFile = Exporter.getResultFile(builder.product, builder.settings).toString();
    }

    @Override
    public void export(Collection<String> data) {

    }

    public static final class SqliteExporterBuilder implements ExporterBuilder
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
            return new SqliteExporter(this);
        }
    }
}
