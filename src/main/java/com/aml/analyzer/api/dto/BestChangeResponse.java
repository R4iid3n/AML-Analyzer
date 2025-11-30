package com.aml.analyzer.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * BestChange-compatible response format.
 * Simple, user-friendly format that BestChange aggregator expects.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BestChangeResponse {

    private boolean success;

    @JsonProperty("risk_score")
    private Integer riskScore;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("risk_tags")
    private List<String> riskTags;

    @JsonProperty("report_url")
    private String reportUrl;

    private String message;

    // Extended info (optional, for premium users)
    @JsonProperty("illicit_volume_pct")
    private String illicitVolumePct;

    @JsonProperty("last_illicit_activity")
    private String lastIllicitActivity;
}
