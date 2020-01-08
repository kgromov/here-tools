package com.pefrormance.analyzer.model;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class ResultRow {
    private final String updateRegion;
    private final String logLevel;
    private final String message;
}
