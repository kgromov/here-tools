package com.pefrormance.analyzer.model;

import lombok.Data;

@Data
public class ResultRowBean {
    private final String taskName;
    private final Double durationMap1;
    private final Double durationMap2;
}
