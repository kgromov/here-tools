package com.pefrormance.analyzer.service;

import com.pefrormance.analyzer.model.Settings;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by konstantin on 22.12.2019.
 */
public class ConsoleLogsParser {

    private final Settings settings;


    public ConsoleLogsParser(Settings settings) {
        this.settings = settings;
    }

    public void run()
    {
        // download
        // intermediate layer for product:
        // 1) find files;
        // 2) split to stripes/FJPool or other strategy
        // Introduce 1 more layer or process all files per product;
        // Export
    }

    public void removeLogFiles() {
        try
        {
            Files.walkFileTree(settings.getLogsFolder(), new SimpleFileVisitor<Path>() {
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
        }
        catch (IOException e)
        {

        }
    }
}
