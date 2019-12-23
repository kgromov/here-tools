package com.pefrormance.analyzer.export;

public enum OutputFormat {
    SQ3(".sq3") {
        @Override
        public Exporter getExporter() {
            return new SqliteExporter();
        }
    },
    CSV(".csv") {
        @Override
        public Exporter getExporter() {
            return new CsvExporter();
        }
    };
    private final String format;

    OutputFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public abstract Exporter getExporter();

    // TODO: or probably builder as input parameters are required
    public static OutputFormat getOutputFormat(String fileExtension)
    {
        return OutputFormat.valueOf(fileExtension.toLowerCase());
    }
}
