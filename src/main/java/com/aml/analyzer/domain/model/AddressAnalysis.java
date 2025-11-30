package com.aml.analyzer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressAnalysis {
    private String address;
    private String asset;
    private String network;
    private String clusterId;

    // Sanctions dimension
    private BigDecimal directSanctionedVolumePct;
    private BigDecimal indirectSanctionedVolumePct1Hop;
    private BigDecimal indirectSanctionedVolumePct2to4Hop;

    // Illicit categories (FATF taxonomy)
    private Map<IllicitCategory, BigDecimal> illicitCategoryVolumes;

    // Behavioral features
    private BehavioralMetrics behavioralMetrics;

    // Temporal features
    private TemporalMetrics temporalMetrics;

    // Graph analysis
    private GraphMetrics graphMetrics;

    private LocalDateTime analyzedAt;

    public enum IllicitCategory {
        DARKNET_MARKETS("Darknet Markets"),
        SCAMS_FRAUD("Scams & Fraud"),
        STOLEN_FUNDS("Stolen Funds (Hacks & Exploits)"),
        MIXERS_PRIVACY("Mixers & Privacy Pools"),
        GAMBLING("Gambling"),
        HIGH_RISK_EXCHANGES("High-Risk Exchanges"),
        RANSOMWARE("Ransomware"),
        TERRORIST_FINANCING("Terrorist Financing"),
        CHILD_ABUSE("Child Abuse Material");

        private final String displayName;

        IllicitCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BehavioralMetrics {
        private long transactionCount30d;
        private long transactionCount180d;
        private BigDecimal averageTransactionAmount;
        private BigDecimal medianTransactionAmount;
        private int fanInDegree;
        private int fanOutDegree;
        private double fanInOutRatio;
        private boolean hasPeelChainPattern;
        private int peelChainLength;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemporalMetrics {
        private LocalDateTime lastIllicitTxTime;
        private long lastIllicitTxDaysAgo;
        private BigDecimal illicitVolume30dPct;
        private BigDecimal illicitVolume90dPct;
        private BigDecimal illicitVolume180dPct;
        private LocalDateTime firstSeenTime;
        private LocalDateTime lastActiveTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphMetrics {
        private int clusterSize;
        private int connectedComponents;
        private double clusteringCoefficient;
        private int maxHopDistanceToIllicit;
        private int totalCounterparties;
        private int illicitCounterparties;
    }
}
