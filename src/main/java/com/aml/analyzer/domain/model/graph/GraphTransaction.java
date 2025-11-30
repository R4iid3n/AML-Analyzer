package com.aml.analyzer.domain.model.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Graph transaction - edge in the blockchain transaction graph.
 *
 * Represents a directional edge from one entity to another.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphTransaction {

    private String txHash;

    private String fromEntityId;
    private String toEntityId;

    private BigDecimal amount;
    private String asset;

    private LocalDateTime timestamp;

    private TransactionDirection direction;  // OUTGOING, INCOMING from perspective

    // Edge properties for pattern matching
    private long durationSincePrevious;  // milliseconds
    private boolean isCrossBridge;
    private boolean isMixerHop;

    public enum TransactionDirection {
        OUTGOING,
        INCOMING,
        INTERNAL
    }

    public long getHoursSince(LocalDateTime other) {
        return java.time.Duration.between(other, timestamp).toHours();
    }
}
