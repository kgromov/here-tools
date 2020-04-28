package com.pefrormance.analyzer.service;

import lombok.Getter;
import lombok.ToString;

/**
 * Created by konstantin on 27.04.2020.
 */
@Getter
@ToString
public class AwsCommand {
    private final String command;

    private AwsCommand(Builder builder)
    {
        this.command = builder.buildCommand();
    }

    public static class Builder
    {
        private static final String AWS_SYNC_COMMAND = "aws s3 sync";
        private String sourcePath;
        private String destPath;
        private String product;
        private String updateRegion;

        public Builder sourcePath(String sourcePath)
        {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder destPath(String destPath)
        {
            this.destPath = destPath;
            return this;
        }

        public Builder product(String product)
        {
            this.product = product;
            return this;
        }

        public Builder updateRegion(String updateRegion)
        {
            this.updateRegion = updateRegion;
            return this;
        }

        public AwsCommand build()
        {
            return new AwsCommand(this);
        }

        private String buildCommand()
        {
            StringBuilder commandBuilder = new StringBuilder(AWS_SYNC_COMMAND);
            commandBuilder.append(' ')
                    .append(sourcePath)
                    .append(' ')
                    .append(destPath)
                    .append(' ')
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
}
