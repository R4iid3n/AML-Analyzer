package com.aml.analyzer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskScore {
    private int totalScore;
    private RiskLevel riskLevel;
    private List<ScoreComponent> scoreBreakdown;
    private BigDecimal illicitVolumePct;
    private BigDecimal cleanVolumePct;
    private List<RiskTag> tags;

    public enum RiskLevel {
        LOW(0, 20),
        MEDIUM(21, 49),
        HIGH(50, 74),
        CRITICAL(75, 100);

        private final int min;
        private final int max;

        RiskLevel(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public static RiskLevel fromScore(int score) {
            for (RiskLevel level : values()) {
                if (score >= level.min && score <= level.max) {
                    return level;
                }
            }
            return LOW;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreComponent {
        private String dimension;
        private int value;
        private String explanation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskTag {
        private String code;
        private Severity severity;
        private String description;

        public enum Severity {
            LOW, MEDIUM, HIGH, CRITICAL
        }
    }
}
