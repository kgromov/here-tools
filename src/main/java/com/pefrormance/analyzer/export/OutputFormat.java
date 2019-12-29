package com.pefrormance.analyzer.export;

public enum OutputFormat
{
    SQ3(".sq3") ,
    CSV(".csv");

    private final String format;

    OutputFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public static OutputFormat getOutputFormat(String fileExtension)
    {
        return OutputFormat.valueOf(fileExtension.toUpperCase());
    }
}
