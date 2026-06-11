package com.yzh.yingshi.dto;

import lombok.Data;

import java.util.List;

@Data
public class PetAiVisionResult {

    private String riskLevel;

    private String summary;

    private String observedBehavior;

    private String evidenceBasis;

    private List<String> recommendations;

    private List<String> uncertainties;
}
