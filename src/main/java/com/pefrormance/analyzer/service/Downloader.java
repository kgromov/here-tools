package com.pefrormance.analyzer.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class Downloader {
    // TODO: probably move to enum; for WOM market is required not UR
    private static final String AWS_COPY_COMMAND = "aws s3 cp";
    private static final String AWS_SYNC_COMMAND = "aws s3 sync";
    private final String product;
    private final String updateRegion;
    private final String sourcePath;
    private final String destPath;

    public Downloader(String market, String updateRegion, String sourcePath, String destPath) {
        this.product = market;
        this.updateRegion = updateRegion;
        this.sourcePath = sourcePath;
        this.destPath = destPath;
    }

    public void download(Consumer<String> logConsumer) throws Exception
    {
        String command = getAwsCopyCommand();
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String s;
        while ((s = stdInput.readLine()) != null) {
            logConsumer.accept(s);
        }
    }

    // aws s3 cp <s3_path> <local_path> --recursive --exclude "*" --include "LC_*json" | --include "FB_MUC_*json"
    private String getAwsCopyCommand()
    {
//        StringBuilder commandBuilder = new StringBuilder(AWS_COPY_COMMAND);
        StringBuilder commandBuilder = new StringBuilder(AWS_SYNC_COMMAND);
        commandBuilder.append(' ')
                .append(sourcePath)
                .append(' ')
                .append(destPath)
                .append(' ')
//                .append("--recursive --exclude \"*\"")
                .append("--exclude \"*\"")
                .append(' ')
                .append("--include \"")
                .append(product)
                .append('*');
        if (updateRegion != null)
        {
            commandBuilder.append(updateRegion).append('*');
        }
        commandBuilder.append("json\"");
        return commandBuilder.toString();
    }
}
