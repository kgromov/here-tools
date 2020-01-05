package com.pefrormance.analyzer.model;

import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Getter
@ToString
public class Settings
{
    private static final String DEFAULT_VERSION1 = "map1";
    private static final String DEFAULT_VERSION2 = "map2";

    // TODO: probably to enum: name; tableName
    private Set<String> products;
    private String updateRegion;
    private String map1Path;
    private String map1Name;
    private String map2Path;
    private String map2Name;
    private String outputDir;

    private Settings(Builder builder)
    {
      this.products = builder.products;
      this.updateRegion = builder.updateRegion;
      this.map1Path = builder.map1Path;
      this.map1Name = builder.map1Name;
      this.map2Path = builder.map2Path;
      this.map2Name = builder.map2Name;
      this.outputDir = builder.outputDir == null ? Paths.get(".").normalize().toAbsolutePath().toString() : builder.outputDir;
    }

    public Path getDataFolder()
    {
        return Paths.get(outputDir).resolve("data");
    }

    public Path getResultsFolder()
    {
        return Paths.get(outputDir).resolve("results");
    }

    public Path getLogsFolder()
    {
        return Paths.get(outputDir).resolve("logs");
    }

    private static String getVersion(String path, String defaultValue)
    {
        int startIndex = path.indexOf("Akela");
        int endIndex = path.indexOf("/logs");
        return startIndex != -1 && endIndex != -1 ? path.substring(startIndex, endIndex) : defaultValue;
    }

    public String getMap1Name()
    {
        return map1Name != null && !map1Name.isEmpty()
                ? map1Name
                : getVersion(map1Path, DEFAULT_VERSION1);
    }
    public String getMap2Name()
    {
        return map2Name != null && !map2Name.isEmpty()
                ? map2Name
                : getVersion(map2Path, DEFAULT_VERSION2);
    }


    public void reset()
    {
        this.products = null;
        this.updateRegion = null;
        this.map1Path = null;
        this.map2Path = null;
        this.outputDir = null;
    }

    public static final class Builder
    {
        private Set<String> products;
        private String updateRegion;
        private String map1Path;
        private String map1Name;
        private String map2Path;
        private String map2Name;
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

        public Builder map1Path(String map1Path)
        {
            this.map1Path = map1Path;
            return this;
        }

        public Builder map2Path(String map2Path)
        {
            this.map2Path = map2Path;
            return this;
        }

        public Builder map1Name(String map1Name)
        {
            this.map1Name = map1Name;
            return this;
        }

        public Builder map2Name(String map2Name)
        {
            this.map2Name = map2Name;
            return this;
        }

        public Builder outputDir(String outputDir)
        {
            this.outputDir = outputDir;
            return this;
        }
    }
}
