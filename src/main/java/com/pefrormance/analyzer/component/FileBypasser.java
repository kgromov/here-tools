package com.pefrormance.analyzer.component;

import com.pefrormance.analyzer.config.InjectedSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by konstantin on 28.04.2020.
 */
@Slf4j
@Component
public class FileBypasser {
    private static final Function<Path, String> PATH_TO_REGION = path ->
    {
        String fileName = path.getFileName().toString();
        int startIndex = fileName.indexOf('_');
        int endIndex = fileName.indexOf("build_number");
        return startIndex != -1 && endIndex != -1 ? fileName.substring(startIndex + 1, endIndex - 1) : fileName;
    };

    private final Path dataFolder;

    public FileBypasser(InjectedSettings settings) {
        this.dataFolder = settings.getDataFolder();
    }

    public Map<String, Pair<Path, Path>> aggregateLogPerRegion(String product) {
        try {
            return Files.find(dataFolder,
                    Short.MAX_VALUE,
                    (filePath, fileAttr) -> fileAttr.isRegularFile()
                            && filePath.getFileName().toString().startsWith(product)
                            && filePath.toString().endsWith(".json"))
                    .sorted(Comparator.comparing(o -> o.getFileName().toString()))
                    .collect(Collectors.groupingBy(PATH_TO_REGION))
                    .entrySet().stream()
                    .filter(e -> e.getValue().size() == 2)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> Pair.of(e.getValue().get(0), e.getValue().get(1))));
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    public void removeLogFiles() {
        try {
            log.debug(String.format("Remove collected data from: %s", dataFolder));
            Files.walkFileTree(dataFolder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Unable to clear temporary data folder:", e);
        }
    }
}
