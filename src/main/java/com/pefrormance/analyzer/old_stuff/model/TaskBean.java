package com.pefrormance.analyzer.old_stuff.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskBean implements Serializable{
    private String name;
    @JsonProperty("Start")
    private String start;
    @JsonProperty("End")
    private String end;
    private double duration;
}
