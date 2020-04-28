package com.pefrormance.analyzer.config;

import com.pefrormance.analyzer.model.Product;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
/*import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;*/
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Created by konstantin on 27.04.2020.
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "settings")
public class InjectedSettings {
    private static final String DEFAULT_VERSION1 = "map1";
    private static final String DEFAULT_VERSION2 = "map2";

    private String updateRegion;
//    @NotBlank
    private String map1Path;
    private String map1Name;
//    @NotBlank
    private String map2Path;
    private String map2Name;
//    @NotBlank
    private String outputDir;
//    @NotEmpty
    private Set<Product> products;

    @PostConstruct
    public void print()
    {
        log.info("Settings: {}", this.toString());
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
}
