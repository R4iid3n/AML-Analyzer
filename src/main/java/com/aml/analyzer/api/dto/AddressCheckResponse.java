package com.aml.analyzer.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressCheckResponse {

    private String requestId;
    private AnalysisStatus status;
    private LocalDateTime checkedAt;

    private String address;
    private String asset;
    private String network;
    private String clusterId;

    private Integer riskScore;
    private String riskLevel;

    private List<ScoreBreakdownDto> scoreBreakdown;
    private List<RiskTagDto> tags;

    private BigDecimal illicitVolumePct;
    private BigDecimal cleanVolumePct;

    private String reportUrl;
    private String reportHtmlUrl;
    private String reportPdfUrl;

    // For long-running async analysis
    private String statusUrl;

    // Audit trail
    private ScoreHistory scoreHistory;

    public enum AnalysisStatus {
        QUEUED,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreBreakdownDto {
        private String dimension;
        private Integer value;
        private String explanation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskTagDto {
        private String code;
        private String severity;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreHistory {
        private Integer previousScore;
        private String previousRiskLevel;
        private LocalDateTime previousCheckAt;
        private Integer scoreDelta;
        private String changeReason;
    }
}
