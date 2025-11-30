package com.aml.analyzer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Entity cluster - a collection of addresses across multiple chains
 * that are controlled by the same entity.
 *
 * This is what GetBlock and CoinKYT lack - cross-chain entity intelligence.
 *
 * Example: A hacker steals ETH, converts some to BTC via a mixer,
 * and moves USDT to TRON. All three addresses should be clustered
 * together as one risky entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityCluster {

    private String clusterId;

    private List<ClusterAddress> addresses;

    private ClusteringConfidence confidence;

    private List<ClusteringEvidence> evidence;

    private int riskScore;

    private RiskScore.RiskLevel riskLevel;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum ClusteringConfidence {
        LOW,      // Weak heuristics only
        MEDIUM,   // Multiple heuristics align
        HIGH,     // Strong on-chain evidence
        VERIFIED  // Known entity (exchange, service, etc.)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusterAddress {
        private String address;
        private String asset;
        private String network;
        private LocalDateTime firstSeen;
        private LocalDateTime lastActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusteringEvidence {
        private ClusteringHeuristic heuristic;
        private String description;
        private double confidence;

        public enum ClusteringHeuristic {
            // Common ownership heuristics
            MULTI_INPUT("Multiple inputs in same transaction - likely same owner"),
            CHANGE_ADDRESS("Change address pattern"),
            SELF_TRANSFER("Self-transfer detected"),

            // Cross-chain heuristics
            BRIDGE_TRANSFER("Bridge transfer tracked"),
            EXCHANGE_DEPOSIT_WITHDRAWAL("Deposit-withdrawal from same exchange"),
            SWAP_PATTERN("Swap service used, addresses linked"),

            // Behavioral heuristics
            TIMING_PATTERN("Transaction timing correlation"),
            AMOUNT_FINGERPRINT("Unique amount pattern"),
            GAS_PRICE_PATTERN("Consistent gas price preference"),

            // Known entity
            TAGGED_ENTITY("Address tagged as known entity");

            private final String description;

            ClusteringHeuristic(String description) {
                this.description = description;
            }

            public String getDescription() {
                return description;
            }
        }
    }
}
