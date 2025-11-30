package com.aml.analyzer.infrastructure.persistence.repository;

import com.aml.analyzer.infrastructure.persistence.entity.AddressAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressAnalysisRepository extends JpaRepository<AddressAnalysisEntity, String> {

    /**
     * Find latest analysis for an address.
     */
    Optional<AddressAnalysisEntity> findFirstByAddressAndAssetAndNetworkOrderByAnalyzedAtDesc(
            String address, String asset, String network);

    /**
     * Get historical analyses for an address - audit trail.
     */
    List<AddressAnalysisEntity> findByAddressAndAssetAndNetworkOrderByAnalyzedAtDesc(
            String address, String asset, String network);

    /**
     * Find all analyses in a cluster.
     */
    List<AddressAnalysisEntity> findByClusterIdOrderByAnalyzedAtDesc(String clusterId);

    /**
     * Find high-risk addresses analyzed recently.
     */
    @Query("SELECT a FROM AddressAnalysisEntity a WHERE a.riskScore >= :minScore " +
           "AND a.analyzedAt >= CURRENT_TIMESTAMP - :daysAgo DAY ORDER BY a.riskScore DESC")
    List<AddressAnalysisEntity> findHighRiskAddresses(
            @Param("minScore") int minScore,
            @Param("daysAgo") int daysAgo);
}
