package com.aml.analyzer.domain.model.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Graph entity - node in the blockchain transaction graph.
 *
 * Represents an entity (address or cluster) with properties and relationships.
 * This is the foundation for graph-based pattern matching.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEntity {

    private String id;  // Entity ID (address or cluster ID)

    private EntityType type;

    private String chain;  // BTC, ETH, TRON, etc.

    private Set<String> tags;  // ["MIXER", "SANCTIONED", "SCAM", etc.]

    private EntityCategory category;

    // Graph properties
    private int inDegree;
    private int outDegree;
    private double pageRank;  // For importance
    private double clusteringCoefficient;

    // Risk properties
    private Integer riskScore;
    private List<String> riskFlags;

    public enum EntityType {
        EOA,              // Externally Owned Account
        CONTRACT,         // Smart contract
        CEX,              // Centralized exchange
        DEX,              // Decentralized exchange
        MIXER,            // Mixer/tumbler
        BRIDGE,           // Cross-chain bridge
        SCAM,             // Known scam
        DARKNET,          // Darknet market
        SANCTIONED,       // Sanctioned entity
        UNKNOWN
    }

    public enum EntityCategory {
        CLEAN,
        MIXER,
        BRIDGE,
        CEX_HIGH_RISK,
        CEX_COMPLIANT,
        DARKNET,
        SCAM,
        SANCTIONED,
        STOLEN,
        RANSOMWARE,
        TERRORIST_FINANCING
    }

    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }

    public boolean isCategory(EntityCategory cat) {
        return this.category == cat;
    }
}
