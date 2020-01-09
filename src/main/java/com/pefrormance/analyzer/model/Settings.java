package com.pefrormance.analyzer.model;

import com.pefrormance.analyzer.export.OutputFormat;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.regex.Pattern;

@Getter
@ToString
public class Settings
{
    private Set<Product> products;
    private String updateRegion;
    private String mapPath;
    private LogFile logFile;
    private String expressionToFind;
    private Pattern expressionPattern;
    private OutputFormat outputFormat;
    private String outputDir;

    private Settings(Builder builder)
    {
      this.products = builder.products;
      this.updateRegion = builder.updateRegion;
      this.mapPath = builder.mapPath;
      this.logFile = builder.logFile;
      this.expressionToFind = builder.expressionToFind;
      this.expressionPattern = builder.expressionPattern;
      this.outputFormat = builder.outputFormat;
      this.outputDir = builder.outputDir == null ? Paths.get(".").normalize().toAbsolutePath().toString() : builder.outputDir;
    }

    public Path getDataFolder()
    {
        return Paths.get(outputDir).resolve("logs-data");
    }

    public Path getResultsFolder()
    {
        return Paths.get(outputDir).resolve("results");
    }

    public boolean isRegExp()
    {
        return expressionPattern != null;
    }

    public void reset()
    {
        this.products = null;
        this.updateRegion = null;
        this.mapPath = null;
        this.logFile = null;
        this.expressionToFind = null;
        this.outputFormat = null;
        this.outputDir = null;
        this.expressionPattern = null;
    }

    public static final class Builder
    {
        private Set<Product> products;
        private String updateRegion;
        private String mapPath;
        private LogFile logFile;
        private String expressionToFind;
        private Pattern expressionPattern;
        private OutputFormat outputFormat;
        private String outputDir;

        public Settings build()
        {
            return new Settings(this);
        }

        public Builder product(Set<Product> products)
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

        public Builder logFile(LogFile logFile)
        {
            this.logFile = logFile;
            return this;
        }

        public Builder expressionToFind(String expressionToFind)
        {
            this.expressionToFind = expressionToFind;
            return this;
        }

        public Builder expressionPattern(Pattern expressionPattern)
        {
            this.expressionPattern = expressionPattern;
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
