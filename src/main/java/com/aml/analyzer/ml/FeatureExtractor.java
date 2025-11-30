package com.aml.analyzer.ml;

import com.aml.analyzer.domain.model.graph.EgoGraph;
import com.aml.analyzer.domain.model.graph.GraphEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ML Feature extractor - converts graph to feature vectors for ML models.
 *
 * Competitors use ML too, but we have BETTER features:
 * - Graph topology features (PageRank, clustering coefficient)
 * - Temporal features (velocity, acceleration)
 * - Pattern features (automata match results)
 * - Cross-chain features (unique to us)
 *
 * Features feed into:
 * - Random Forest (interpretable, good for compliance)
 * - XGBoost (high accuracy)
 * - Graph Neural Networks (state-of-the-art)
 */
@Slf4j
@Service
public class FeatureExtractor {

    /**
     * Extract all features from ego graph.
     */
    public FeatureVector extractFeatures(EgoGraph egoGraph) {
        String centerEntityId = egoGraph.getCenterEntityId();
        GraphEntity centerEntity = egoGraph.getEntities().get(centerEntityId);

        List<Double> features = new ArrayList<>();

        // 1. Topology features
        features.addAll(extractTopologyFeatures(egoGraph, centerEntity));

        // 2. Behavioral features
        features.addAll(extractBehavioralFeatures(egoGraph, centerEntity));

        // 3. Temporal features
        features.addAll(extractTemporalFeatures(egoGraph, centerEntity));

        // 4. Categorical features (one-hot encoded)
        features.addAll(extractCategoricalFeatures(egoGraph, centerEntity));

        // 5. Cross-chain features (unique!)
        features.addAll(extractCrossChainFeatures(egoGraph, centerEntity));

        return FeatureVector.builder()
                .entityId(centerEntityId)
                .features(features)
                .featureNames(getFeatureNames())
                .build();
    }

    /**
     * Topology features - graph structure.
     */
    private List<Double> extractTopologyFeatures(EgoGraph egoGraph, GraphEntity entity) {
        List<Double> features = new ArrayList<>();

        // Degree features
        features.add((double) entity.getInDegree());
        features.add((double) entity.getOutDegree());
        features.add(entity.getInDegree() + entity.getOutDegree() > 0 ?
                (double) entity.getInDegree() / (entity.getInDegree() + entity.getOutDegree()) : 0.0);

        // PageRank (importance in graph)
        features.add(entity.getPageRank());

        // Clustering coefficient (how connected are neighbors)
        features.add(entity.getClusteringCoefficient());

        // Ego graph size
        features.add((double) egoGraph.getEntities().size());
        features.add((double) egoGraph.getTransactions().size());

        // Distance to specific entity types
        features.add((double) countEntitiesByCategory(egoGraph, GraphEntity.EntityCategory.MIXER));
        features.add((double) countEntitiesByCategory(egoGraph, GraphEntity.EntityCategory.CEX_HIGH_RISK));
        features.add((double) countEntitiesByCategory(egoGraph, GraphEntity.EntityCategory.SANCTIONED));

        return features;
    }

    /**
     * Behavioral features - transaction patterns.
     */
    private List<Double> extractBehavioralFeatures(EgoGraph egoGraph, GraphEntity entity) {
        List<Double> features = new ArrayList<>();

        double totalVolume = egoGraph.getTotalVolume(entity.getId());

        // Volume features
        features.add(totalVolume);
        features.add(Math.log1p(totalVolume));  // Log scale

        // Transaction count features
        int txCount = egoGraph.getOutgoingTransactions(entity.getId()).size() +
                      egoGraph.getIncomingTransactions(entity.getId()).size();
        features.add((double) txCount);
        features.add(Math.log1p(txCount));

        // Average transaction size
        features.add(txCount > 0 ? totalVolume / txCount : 0.0);

        // Gini coefficient (concentration of value in few txs)
        features.add(calculateGiniCoefficient(egoGraph, entity.getId()));

        // Fan-in/out ratio
        double fanInOut = entity.getOutDegree() > 0 ?
                (double) entity.getInDegree() / entity.getOutDegree() : 0.0;
        features.add(fanInOut);

        return features;
    }

    /**
     * Temporal features - time-based patterns.
     */
    private List<Double> extractTemporalFeatures(EgoGraph egoGraph, GraphEntity entity) {
        List<Double> features = new ArrayList<>();

        // Transaction velocity (txs per day)
        // TODO: Calculate from timestamps
        features.add(0.0);

        // Transaction acceleration (change in velocity)
        features.add(0.0);

        // Time since first/last transaction
        features.add(0.0);
        features.add(0.0);

        // Active hours (how many hours of day entity transacts)
        features.add(0.0);

        // Weekend activity ratio
        features.add(0.0);

        return features;
    }

    /**
     * Categorical features - one-hot encoded.
     */
    private List<Double> extractCategoricalFeatures(EgoGraph egoGraph, GraphEntity entity) {
        List<Double> features = new ArrayList<>();

        // Entity type (one-hot)
        for (GraphEntity.EntityType type : GraphEntity.EntityType.values()) {
            features.add(entity.getType() == type ? 1.0 : 0.0);
        }

        // Entity category (one-hot)
        for (GraphEntity.EntityCategory cat : GraphEntity.EntityCategory.values()) {
            features.add(entity.getCategory() == cat ? 1.0 : 0.0);
        }

        // Has specific tags (binary)
        features.add(entity.hasTag("MIXER") ? 1.0 : 0.0);
        features.add(entity.hasTag("SANCTIONED") ? 1.0 : 0.0);
        features.add(entity.hasTag("SCAM") ? 1.0 : 0.0);
        features.add(entity.hasTag("DARKNET") ? 1.0 : 0.0);

        return features;
    }

    /**
     * Cross-chain features - UNIQUE to our analyzer.
     */
    private List<Double> extractCrossChainFeatures(EgoGraph egoGraph, GraphEntity entity) {
        List<Double> features = new ArrayList<>();

        // Number of chains this entity operates on (if clustered)
        // TODO: Implement based on cluster data
        features.add(1.0);

        // Cross-bridge transaction count
        long bridgeTxCount = egoGraph.getOutgoingTransactions(entity.getId()).stream()
                .filter(com.aml.analyzer.domain.model.graph.GraphTransaction::isCrossBridge)
                .count();
        features.add((double) bridgeTxCount);

        // Cross-bridge volume ratio
        double bridgeVolume = egoGraph.getOutgoingTransactions(entity.getId()).stream()
                .filter(com.aml.analyzer.domain.model.graph.GraphTransaction::isCrossBridge)
                .mapToDouble(tx -> tx.getAmount().doubleValue())
                .sum();
        double totalVolume = egoGraph.getTotalVolume(entity.getId());
        features.add(totalVolume > 0 ? bridgeVolume / totalVolume : 0.0);

        return features;
    }

    private int countEntitiesByCategory(EgoGraph egoGraph, GraphEntity.EntityCategory category) {
        return (int) egoGraph.getEntities().values().stream()
                .filter(e -> e.getCategory() == category)
                .count();
    }

    private double calculateGiniCoefficient(EgoGraph egoGraph, String entityId) {
        // Simplified Gini - measure inequality in transaction sizes
        // TODO: Implement proper Gini calculation
        return 0.5;
    }

    private List<String> getFeatureNames() {
        // Return feature names for interpretability
        List<String> names = new ArrayList<>();

        // Topology (10)
        names.add("in_degree");
        names.add("out_degree");
        names.add("degree_ratio");
        names.add("pagerank");
        names.add("clustering_coef");
        names.add("ego_graph_nodes");
        names.add("ego_graph_edges");
        names.add("mixer_count");
        names.add("high_risk_cex_count");
        names.add("sanctioned_count");

        // Behavioral (7)
        names.add("total_volume");
        names.add("log_volume");
        names.add("tx_count");
        names.add("log_tx_count");
        names.add("avg_tx_size");
        names.add("gini_coefficient");
        names.add("fan_in_out_ratio");

        // Temporal (6)
        names.add("tx_velocity");
        names.add("tx_acceleration");
        names.add("time_since_first");
        names.add("time_since_last");
        names.add("active_hours");
        names.add("weekend_ratio");

        // Categorical (entity types + categories + tags)
        for (GraphEntity.EntityType type : GraphEntity.EntityType.values()) {
            names.add("type_" + type.name().toLowerCase());
        }
        for (GraphEntity.EntityCategory cat : GraphEntity.EntityCategory.values()) {
            names.add("category_" + cat.name().toLowerCase());
        }
        names.add("has_mixer_tag");
        names.add("has_sanctioned_tag");
        names.add("has_scam_tag");
        names.add("has_darknet_tag");

        // Cross-chain (3)
        names.add("num_chains");
        names.add("bridge_tx_count");
        names.add("bridge_volume_ratio");

        return names;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureVector {
        private String entityId;
        private List<Double> features;
        private List<String> featureNames;

        public double[] toArray() {
            return features.stream().mapToDouble(Double::doubleValue).toArray();
        }

        public int size() {
            return features.size();
        }
    }
}
