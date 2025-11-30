package com.aml.analyzer.ml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ML-based risk prediction model.
 *
 * Supports multiple model types:
 * - Random Forest (interpretable, good for compliance)
 * - XGBoost (high accuracy, industry standard)
 * - Neural Network (deep learning)
 * - Graph Neural Network (state-of-the-art for graphs)
 *
 * Models trained on:
 * - Known illicit addresses (positive labels)
 * - Clean addresses (negative labels)
 * - Features from FeatureExtractor
 *
 * Output: Risk probability [0, 1]
 *
 * Integration options:
 * - ONNX Runtime (load pretrained models)
 * - TensorFlow Java
 * - DL4J (DeepLearning4J)
 * - H2O.ai
 * - External API (Python microservice)
 */
@Slf4j
@Service
public class RiskPredictionModel {

    private ModelType currentModelType = ModelType.RANDOM_FOREST;

    /**
     * Predict risk probability from feature vector.
     *
     * Returns probability in [0, 1] where:
     * - 0.0 = clean
     * - 1.0 = definitely illicit
     */
    public PredictionResult predict(FeatureExtractor.FeatureVector features) {
        log.info("Running ML prediction for entity: {}", features.getEntityId());

        // TODO: Load actual trained model
        // For now, return stub prediction based on simple heuristics

        double riskProbability = calculateStubProbability(features);

        return PredictionResult.builder()
                .entityId(features.getEntityId())
                .riskProbability(riskProbability)
                .riskScore((int) (riskProbability * 100))
                .confidence(0.85)  // Model confidence
                .modelType(currentModelType)
                .featureImportance(calculateFeatureImportance(features))
                .build();
    }

    /**
     * Stub probability calculation (replace with real ML model).
     */
    private double calculateStubProbability(FeatureExtractor.FeatureVector features) {
        // Simple heuristic based on key features
        double[] featureArray = features.toArray();

        double score = 0.0;

        // High mixer count → high risk
        if (featureArray.length > 7) {
            score += featureArray[7] * 0.3;  // mixer_count
        }

        // High sanctioned count → very high risk
        if (featureArray.length > 9) {
            score += featureArray[9] * 0.5;  // sanctioned_count
        }

        // High volume → higher risk (up to a point)
        if (featureArray.length > 10) {
            double logVolume = featureArray[11];
            score += Math.min(0.2, logVolume / 100.0);
        }

        return Math.min(1.0, score);
    }

    /**
     * Calculate feature importance (for explainability).
     *
     * Random Forest and XGBoost provide feature importance natively.
     * For neural networks, use SHAP or LIME.
     */
    private FeatureImportance calculateFeatureImportance(FeatureExtractor.FeatureVector features) {
        // TODO: Get from actual model
        return FeatureImportance.builder()
                .topFeatures(java.util.List.of(
                        FeatureImportance.FeatureScore.builder()
                                .featureName("sanctioned_count")
                                .importance(0.35)
                                .build(),
                        FeatureImportance.FeatureScore.builder()
                                .featureName("mixer_count")
                                .importance(0.25)
                                .build(),
                        FeatureImportance.FeatureScore.builder()
                                .featureName("pagerank")
                                .importance(0.15)
                                .build()
                ))
                .build();
    }

    public enum ModelType {
        RANDOM_FOREST,      // Scikit-learn RandomForest (ONNX export)
        XGBOOST,            // XGBoost (ONNX export)
        NEURAL_NETWORK,     // TensorFlow/PyTorch (ONNX export)
        GRAPH_NEURAL_NET,   // GNN (PyG/DGL → ONNX)
        ENSEMBLE            // Ensemble of multiple models
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictionResult {
        private String entityId;
        private double riskProbability;  // [0, 1]
        private int riskScore;           // [0, 100]
        private double confidence;       // Model confidence
        private ModelType modelType;
        private FeatureImportance featureImportance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureImportance {
        private java.util.List<FeatureScore> topFeatures;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FeatureScore {
            private String featureName;
            private double importance;  // [0, 1]
        }
    }
}
