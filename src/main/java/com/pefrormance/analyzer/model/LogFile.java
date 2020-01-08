package com.pefrormance.analyzer.model;

public enum LogFile
{
    // Akela
    NONE("--None--", null),
    TRACE("TRACE", "logfile.log"),
    DEBUG("DEBUG", "logfile.log"),
    INFO("INFO", "logfile.log"),
    WARN("WARN", "logfile.log"),
    ERROR("ERROR", "error.log"),
    ANY("ANY", "logfile.log"),
    // HDLC -> HD_LOGS.zip
    HDLC("ANY", "sourceDataValidator.csv");

    private final String logLevel;
    private final String fileName;

    LogFile(String logLevel, String fileName) {
        this.logLevel = logLevel;
        this.fileName = fileName;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public String getFileName() {
        return fileName;
    }

    public static LogFile getLogFile(String logLevel)
    {
        if (logLevel.contains("None"))
        {
            return LogFile.NONE;
        }
        return valueOf(logLevel.toUpperCase());
    }
}
