package com.pefrormance.analyzer.export;

public enum OutputFormat {
    SQ3(".sq3") {
        @Override
        public ExporterBuilder getExporterBuilde() {
            return new SqliteExporter.SqliteExporterBuilder();
        }
    },
    CSV(".csv") {
        @Override
        public ExporterBuilder getExporterBuilde() {
            return new CsvExporter.CsvExporterBuilder();
        }
    };
    private final String format;

    OutputFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public abstract ExporterBuilder getExporterBuilde();

    public static OutputFormat getOutputFormat(String fileExtension)
    {
        return OutputFormat.valueOf(fileExtension.toUpperCase());
    }
}
