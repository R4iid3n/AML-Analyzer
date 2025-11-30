package com.aml.analyzer.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for storing address analysis results.
 *
 * Key feature: Audit trail
 * - Every analysis is saved with timestamp
 * - Can compare current vs previous scores
 * - Show "why did score change?" - competitors don't have this
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "address_analysis", indexes = {
        @Index(name = "idx_address_asset", columnList = "address, asset, network"),
        @Index(name = "idx_analyzed_at", columnList = "analyzed_at")
})
public class AddressAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String address;

    @Column(nullable = false, length = 20)
    private String asset;

    @Column(nullable = false, length = 50)
    private String network;

    @Column(name = "cluster_id", length = 100)
    private String clusterId;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "illicit_volume_pct")
    private String illicitVolumePct;

    @Column(name = "score_breakdown", columnDefinition = "TEXT")
    private String scoreBreakdown;  // JSON

    @Column(name = "risk_tags", columnDefinition = "TEXT")
    private String riskTags;  // JSON

    @Column(name = "analysis_data", columnDefinition = "TEXT")
    private String analysisData;  // Full JSON of AddressAnalysis

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @Column(name = "report_url", length = 500)
    private String reportUrl;

    @Version
    private Long version;
}
