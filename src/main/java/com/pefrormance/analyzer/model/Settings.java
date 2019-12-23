package com.pefrormance.analyzer.model;

import com.pefrormance.analyzer.export.OutputFormat;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Getter
@ToString
public class Settings
{
    private Set<String> products;
    private String updateRegion;
    private String mapPath;
    private String logLevel;
    private String expressionToFind;
    private OutputFormat outputFormat;
    private String outputDir;

    private Settings(Builder builder)
    {
      this.products = builder.products;
      this.updateRegion = builder.updateRegion;
      this.mapPath = builder.mapPath;
      this.logLevel = builder.logLevel;
      this.expressionToFind = builder.expressionToFind;
      this.outputFormat = builder.outputFormat;
      this.outputDir = builder.outputDir == null ? Paths.get(".").normalize().toAbsolutePath().toString() : builder.outputDir;
    }

    public Path getLogsFolder()
    {
        return Paths.get(outputDir).resolve("logs");
    }

    public Path getResultsFolder()
    {
        return Paths.get(outputDir).resolve("results");
    }


    public void reset()
    {
        this.products = null;
        this.updateRegion = null;
        this.mapPath = null;
        this.logLevel = null;
        this.expressionToFind = null;
        this.outputFormat = null;
        this.outputDir = null;
    }

    public static final class Builder
    {
        private Set<String> products;
        private String updateRegion;
        private String mapPath;
        private String logLevel;
        private String expressionToFind;
        private OutputFormat outputFormat;
        private String outputDir;

        public Settings build()
        {
            return new Settings(this);
        }

        public Builder product(Set<String> products)
        {
            this.products = products;
            return this;
        }

        public Builder updateRegion(String updateRegion)
        {
            this.updateRegion = updateRegion;
            return this;
        }

        public Builder mapPath(String mapPath)
        {
            this.mapPath = mapPath;
            return this;
        }

        public Builder logLevel(String logLevel)
        {
            this.logLevel = logLevel;
            return this;
        }

        public Builder expressionToFind(String expressionToFind)
        {
            this.expressionToFind = expressionToFind;
            return this;
        }

        public Builder outputFormat(OutputFormat outputFormat)
        {
            this.outputFormat = outputFormat;
            return this;
        }

        public Builder outputDir(String outputDir)
        {
            this.outputDir = outputDir;
            return this;
        }
    }
}
