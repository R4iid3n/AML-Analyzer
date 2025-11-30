package com.aml.analyzer.domain.service;

import com.aml.analyzer.domain.model.AddressAnalysis;
import com.aml.analyzer.domain.model.RiskScore;
import com.aml.analyzer.domain.model.graph.EgoGraph;
import com.aml.analyzer.domain.model.pattern.PatternAutomaton;
import com.aml.analyzer.ml.FeatureExtractor;
import com.aml.analyzer.ml.RiskPredictionModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Hybrid risk scoring engine - THE ULTIMATE COMPETITIVE ADVANTAGE.
 *
 * Combines THREE approaches:
 * 1. Rule-based scoring (original RiskScoringEngine)
 * 2. Pattern matching (automata on graphs)
 * 3. Machine learning (trained models)
 *
 * This is how to CRUSH competitors:
 *
 * Chainalysis: Likely uses rules + ML, but no pattern automata, no cross-chain
 * GetBlock: Basic rules only (% from categories)
 * CoinKYT: Rules + possibly basic ML
 *
 * We have: RULES + PATTERNS + ML + CROSS-CHAIN
 *
 * Scoring formula:
 * final_score = α * rule_score + β * pattern_score + γ * ml_score
 *
 * Where:
 * - rule_score = original component-based scoring (sanctions, categories, etc.)
 * - pattern_score = weighted sum of matched patterns
 * - ml_score = ML model prediction * 100
 * - α, β, γ = configurable weights (default: 0.4, 0.3, 0.3)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRiskScoringEngine {

    private final RiskScoringEngine ruleBasedEngine;
    private final PatternMatchingEngine patternEngine;
    private final PatternLibrary patternLibrary;
    private final FeatureExtractor featureExtractor;
    private final RiskPredictionModel mlModel;

    // Configurable weights for hybrid scoring
    private static final double RULE_WEIGHT = 0.4;
    private static final double PATTERN_WEIGHT = 0.3;
    private static final double ML_WEIGHT = 0.3;

    /**
     * Calculate risk score using hybrid approach.
     *
     * Pipeline:
     * 1. Build ego graph around target entity
     * 2. Run rule-based scoring (sanctions, categories, behavioral, temporal)
     * 3. Run pattern matching (automata on graph)
     * 4. Extract ML features from graph
     * 5. Run ML model prediction
     * 6. Combine all three scores
     * 7. Return enriched risk score with breakdown
     */
    public RiskScore calculateHybridRisk(AddressAnalysis analysis, EgoGraph egoGraph) {
        log.info("Calculating hybrid risk for address: {}", analysis.getAddress());

        // 1. Rule-based score
        RiskScore ruleBasedScore = ruleBasedEngine.calculateRisk(analysis);
        log.info("Rule-based score: {}", ruleBasedScore.getTotalScore());

        // 2. Pattern-based score
        List<PatternAutomaton> patterns = patternLibrary.getAllPatterns();
        List<PatternAutomaton.MatchResult> patternMatches =
                patternEngine.matchPatterns(egoGraph, patterns);

        int patternScore = calculatePatternScore(patternMatches);
        log.info("Pattern-based score: {} ({} patterns matched)",
                patternScore, patternMatches.stream().filter(PatternAutomaton.MatchResult::isMatched).count());

        // 3. ML-based score
        FeatureExtractor.FeatureVector features = featureExtractor.extractFeatures(egoGraph);
        RiskPredictionModel.PredictionResult mlPrediction = mlModel.predict(features);
        int mlScore = mlPrediction.getRiskScore();
        log.info("ML-based score: {} (probability: {}, confidence: {})",
                mlScore, mlPrediction.getRiskProbability(), mlPrediction.getConfidence());

        // 4. Combine scores
        int finalScore = combineScores(
                ruleBasedScore.getTotalScore(),
                patternScore,
                mlScore
        );

        // 5. Build enriched breakdown
        List<RiskScore.ScoreComponent> enrichedBreakdown = buildEnrichedBreakdown(
                ruleBasedScore,
                patternMatches,
                mlPrediction,
                finalScore
        );

        // 6. Build enriched tags
        List<RiskScore.RiskTag> enrichedTags = buildEnrichedTags(
                ruleBasedScore.getTags(),
                patternMatches
        );

        RiskScore.RiskLevel level = RiskScore.RiskLevel.fromScore(finalScore);

        return RiskScore.builder()
                .totalScore(finalScore)
                .riskLevel(level)
                .scoreBreakdown(enrichedBreakdown)
                .illicitVolumePct(ruleBasedScore.getIllicitVolumePct())
                .cleanVolumePct(ruleBasedScore.getCleanVolumePct())
                .tags(enrichedTags)
                .build();
    }

    /**
     * Calculate score from pattern matches.
     */
    private int calculatePatternScore(List<PatternAutomaton.MatchResult> matches) {
        double totalScore = 0.0;

        for (PatternAutomaton.MatchResult match : matches) {
            if (match.isMatched()) {
                // Weight * confidence (based on volume share)
                double confidence = Math.min(1.0, match.getVolumeShare() / 50.0);
                totalScore += match.getWeight() * confidence;
            }
        }

        return (int) Math.min(100, totalScore);
    }

    /**
     * Combine three scores with weights.
     */
    private int combineScores(int ruleScore, int patternScore, int mlScore) {
        double combined = RULE_WEIGHT * ruleScore +
                         PATTERN_WEIGHT * patternScore +
                         ML_WEIGHT * mlScore;

        return (int) Math.round(Math.max(0, Math.min(100, combined)));
    }

    /**
     * Build enriched breakdown showing all three components.
     */
    private List<RiskScore.ScoreComponent> buildEnrichedBreakdown(
            RiskScore ruleBasedScore,
            List<PatternAutomaton.MatchResult> patternMatches,
            RiskPredictionModel.PredictionResult mlPrediction,
            int finalScore) {

        List<RiskScore.ScoreComponent> breakdown = new ArrayList<>();

        // Original rule-based components
        breakdown.addAll(ruleBasedScore.getScoreBreakdown());

        // Pattern components
        for (PatternAutomaton.MatchResult match : patternMatches) {
            if (match.isMatched()) {
                breakdown.add(RiskScore.ScoreComponent.builder()
                        .dimension("pattern_" + match.getPatternId().toLowerCase())
                        .value((int) (match.getWeight() * Math.min(1.0, match.getVolumeShare() / 50.0)))
                        .explanation(match.getExplanation())
                        .build());
            }
        }

        // ML component
        breakdown.add(RiskScore.ScoreComponent.builder()
                .dimension("ml_prediction")
                .value(mlPrediction.getRiskScore())
                .explanation(String.format("ML model (%s) prediction: %.1f%% probability, %.1f%% confidence",
                        mlPrediction.getModelType(),
                        mlPrediction.getRiskProbability() * 100,
                        mlPrediction.getConfidence() * 100))
                .build());

        // Final hybrid component
        breakdown.add(RiskScore.ScoreComponent.builder()
                .dimension("hybrid_final")
                .value(finalScore)
                .explanation(String.format("Hybrid score: %.1f×rules + %.1f×patterns + %.1f×ML",
                        RULE_WEIGHT, PATTERN_WEIGHT, ML_WEIGHT))
                .build());

        // Add ML feature importance
        if (mlPrediction.getFeatureImportance() != null) {
            for (RiskPredictionModel.FeatureImportance.FeatureScore featureScore :
                    mlPrediction.getFeatureImportance().getTopFeatures()) {
                breakdown.add(RiskScore.ScoreComponent.builder()
                        .dimension("ml_feature_" + featureScore.getFeatureName())
                        .value((int) (featureScore.getImportance() * 100))
                        .explanation(String.format("ML top feature: %s (%.1f%% importance)",
                                featureScore.getFeatureName(),
                                featureScore.getImportance() * 100))
                        .build());
            }
        }

        return breakdown;
    }

    /**
     * Build enriched tags from rules + patterns.
     */
    private List<RiskScore.RiskTag> buildEnrichedTags(
            List<RiskScore.RiskTag> ruleTags,
            List<PatternAutomaton.MatchResult> patternMatches) {

        List<RiskScore.RiskTag> tags = new ArrayList<>(ruleTags);

        // Add pattern tags
        for (PatternAutomaton.MatchResult match : patternMatches) {
            if (match.isMatched()) {
                tags.add(RiskScore.RiskTag.builder()
                        .code("PATTERN_" + match.getPatternId())
                        .severity(convertSeverity(match.getSeverity()))
                        .description(match.getPatternName() + " detected")
                        .build());
            }
        }

        return tags;
    }

    private RiskScore.RiskTag.Severity convertSeverity(PatternAutomaton.PatternSeverity patternSeverity) {
        return switch (patternSeverity) {
            case LOW -> RiskScore.RiskTag.Severity.LOW;
            case MEDIUM -> RiskScore.RiskTag.Severity.MEDIUM;
            case HIGH -> RiskScore.RiskTag.Severity.HIGH;
            case CRITICAL -> RiskScore.RiskTag.Severity.CRITICAL;
        };
    }
}
