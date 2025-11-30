package com.aml.analyzer.domain.service;

import com.aml.analyzer.domain.model.AddressAnalysis;
import com.aml.analyzer.domain.model.RiskScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Core risk scoring engine that implements the decomposed risk model.
 *
 * This is the key differentiator from competitors:
 * - Transparent, explainable scoring with component breakdown
 * - Multi-dimensional risk assessment (sanctions, illicit categories, behavioral, temporal)
 * - Time decay for older illicit activity
 * - Volume-weighted scoring (not just binary "touched bad actor")
 * - Configurable weights for compliance vs false-positive optimization
 */
@Slf4j
@Service
public class RiskScoringEngine {

    // Scoring weights - configurable for different risk appetites
    private static final int MAX_SANCTIONS_SCORE = 60;
    private static final int MAX_MIXER_SCORE = 20;
    private static final int MAX_STOLEN_FUNDS_SCORE = 25;
    private static final int MAX_DARKNET_SCORE = 20;
    private static final int MAX_SCAM_SCORE = 20;
    private static final int MAX_RANSOMWARE_SCORE = 30;
    private static final int MAX_TERRORIST_FINANCING_SCORE = 70;
    private static final int TIME_DECAY_BONUS = -10;
    private static final int RECENT_ACTIVITY_PENALTY = 10;

    public RiskScore calculateRisk(AddressAnalysis analysis) {
        log.info("Calculating risk score for address: {}", analysis.getAddress());

        List<RiskScore.ScoreComponent> components = new ArrayList<>();
        List<RiskScore.RiskTag> tags = new ArrayList<>();
        int totalScore = 0;

        // 1. Sanctions dimension (highest priority)
        int sanctionsScore = calculateSanctionsScore(analysis, components, tags);
        totalScore += sanctionsScore;

        // 2. Illicit categories
        int illicitScore = calculateIllicitCategoriesScore(analysis, components, tags);
        totalScore += illicitScore;

        // 3. Temporal adjustments
        int temporalAdjustment = calculateTemporalAdjustment(analysis, components);
        totalScore += temporalAdjustment;

        // 4. Behavioral red flags (additional penalties)
        int behavioralScore = calculateBehavioralScore(analysis, components, tags);
        totalScore += behavioralScore;

        // Cap score at 0-100
        totalScore = Math.max(0, Math.min(100, totalScore));

        RiskScore.RiskLevel level = RiskScore.RiskLevel.fromScore(totalScore);

        return RiskScore.builder()
                .totalScore(totalScore)
                .riskLevel(level)
                .scoreBreakdown(components)
                .illicitVolumePct(calculateTotalIllicitVolume(analysis))
                .cleanVolumePct(BigDecimal.valueOf(100).subtract(calculateTotalIllicitVolume(analysis)))
                .tags(tags)
                .build();
    }

    private int calculateSanctionsScore(AddressAnalysis analysis,
                                       List<RiskScore.ScoreComponent> components,
                                       List<RiskScore.RiskTag> tags) {
        int score = 0;
        StringBuilder explanation = new StringBuilder();

        // Direct sanctions = critical
        if (analysis.getDirectSanctionedVolumePct() != null &&
            analysis.getDirectSanctionedVolumePct().compareTo(BigDecimal.ZERO) > 0) {
            score = MAX_SANCTIONS_SCORE;
            explanation.append("Direct sanctions exposure: ")
                      .append(analysis.getDirectSanctionedVolumePct())
                      .append("%");
            tags.add(RiskScore.RiskTag.builder()
                    .code("DIRECT_SANCTIONS")
                    .severity(RiskScore.RiskTag.Severity.CRITICAL)
                    .description("Address directly on sanctions list (OFAC/EU/UN)")
                    .build());
        }
        // 1-hop sanctions = high
        else if (analysis.getIndirectSanctionedVolumePct1Hop() != null &&
                 analysis.getIndirectSanctionedVolumePct1Hop().compareTo(BigDecimal.TEN) > 0) {
            score = 40;
            explanation.append("1-hop sanctions exposure: ")
                      .append(analysis.getIndirectSanctionedVolumePct1Hop())
                      .append("%");
            tags.add(RiskScore.RiskTag.builder()
                    .code("SANCTIONS_1HOP")
                    .severity(RiskScore.RiskTag.Severity.HIGH)
                    .description("Direct counterparty on sanctions list")
                    .build());
        }
        // 2-4 hop sanctions = medium
        else if (analysis.getIndirectSanctionedVolumePct2to4Hop() != null &&
                 analysis.getIndirectSanctionedVolumePct2to4Hop().compareTo(BigDecimal.valueOf(20)) > 0) {
            score = 25;
            explanation.append("2-4 hop sanctions exposure: ")
                      .append(analysis.getIndirectSanctionedVolumePct2to4Hop())
                      .append("%");
            tags.add(RiskScore.RiskTag.builder()
                    .code("SANCTIONS_2_4HOP")
                    .severity(RiskScore.RiskTag.Severity.MEDIUM)
                    .description("Indirect sanctions exposure (2-4 hops)")
                    .build());
        }

        if (score > 0) {
            components.add(RiskScore.ScoreComponent.builder()
                    .dimension("sanctions")
                    .value(score)
                    .explanation(explanation.toString())
                    .build());
        }

        return score;
    }

    private int calculateIllicitCategoriesScore(AddressAnalysis analysis,
                                               List<RiskScore.ScoreComponent> components,
                                               List<RiskScore.RiskTag> tags) {
        int totalScore = 0;

        if (analysis.getIllicitCategoryVolumes() == null) {
            return 0;
        }

        // Mixers - volume-weighted
        BigDecimal mixerVol = analysis.getIllicitCategoryVolumes()
                .getOrDefault(AddressAnalysis.IllicitCategory.MIXERS_PRIVACY, BigDecimal.ZERO);
        if (mixerVol.compareTo(BigDecimal.ZERO) > 0) {
            int mixerScore = (int) Math.min(MAX_MIXER_SCORE, mixerVol.doubleValue() * 0.6);
            totalScore += mixerScore;
            components.add(RiskScore.ScoreComponent.builder()
                    .dimension("mixers")
                    .value(mixerScore)
                    .explanation("Mixer/privacy tool usage: " + mixerVol + "%")
                    .build());
            tags.add(RiskScore.RiskTag.builder()
                    .code("MIXER_USAGE")
                    .severity(mixerVol.compareTo(BigDecimal.valueOf(50)) > 0 ?
                             RiskScore.RiskTag.Severity.HIGH : RiskScore.RiskTag.Severity.MEDIUM)
                    .description("Transactions through mixers or privacy protocols")
                    .build());
        }

        // Stolen funds - high weight
        BigDecimal stolenVol = analysis.getIllicitCategoryVolumes()
                .getOrDefault(AddressAnalysis.IllicitCategory.STOLEN_FUNDS, BigDecimal.ZERO);
        if (stolenVol.compareTo(BigDecimal.ZERO) > 0) {
            int stolenScore = (int) Math.min(MAX_STOLEN_FUNDS_SCORE, stolenVol.doubleValue() * 0.8);
            totalScore += stolenScore;
            components.add(RiskScore.ScoreComponent.builder()
                    .dimension("stolen_funds")
                    .value(stolenScore)
                    .explanation("Stolen/hacked funds exposure: " + stolenVol + "%")
                    .build());
            tags.add(RiskScore.RiskTag.builder()
                    .code("STOLEN_FUNDS")
                    .severity(RiskScore.RiskTag.Severity.HIGH)
                    .description("Linked to hacks, exploits, or stolen cryptocurrency")
                    .build());
        }

        // Darknet markets
        BigDecimal darknetVol = analysis.getIllicitCategoryVolumes()
                .getOrDefault(AddressAnalysis.IllicitCategory.DARKNET_MARKETS, BigDecimal.ZERO);
        if (darknetVol.compareTo(BigDecimal.ZERO) > 0) {
            int darknetScore = (int) Math.min(MAX_DARKNET_SCORE, darknetVol.doubleValue() * 0.7);
            totalScore += darknetScore;
            components.add(RiskScore.ScoreComponent.builder()
                    .dimension("darknet")
                    .value(darknetScore)
                    .explanation("Darknet market activity: " + darknetVol + "%")
                    .build());
            tags.add(RiskScore.RiskTag.builder()
                    .code("DARKNET")
                    .severity(RiskScore.RiskTag.Severity.HIGH)
                    .description("Darknet marketplace transactions")
                    .build());
        }

        // Scams
        BigDecimal scamVol = analysis.getIllicitCategoryVolumes()
                .getOrDefault(AddressAnalysis.IllicitCategory.SCAMS_FRAUD, BigDecimal.ZERO);
        if (scamVol.compareTo(BigDecimal.ZERO) > 0) {
            int scamScore = (int) Math.min(MAX_SCAM_SCORE, scamVol.doubleValue() * 0.7);
            totalScore += scamScore;
            components.add(RiskScore.ScoreComponent.builder()
                    .dimension("scams")
                    .value(scamScore)
                    .explanation("Scam/fraud exposure: " + scamVol + "%")
                    .build());
            tags.add(RiskScore.RiskTag.builder()
                    .code("SCAM")
                    .severity(RiskScore.RiskTag.Severity.MEDIUM)
                    .description("Associated with scams or fraudulent schemes")
                    .build());
        }

        // Ransomware - very high weight
        BigDecimal ransomwareVol = analysis.getIllicitCategoryVolumes()
                .getOrDefault(AddressAnalysis.IllicitCategory.RANSOMWARE, BigDecimal.ZERO);
        if (ransomwareVol.compareTo(BigDecimal.ZERO) > 0) {
            int ransomwareScore = (int) Math.min(MAX_RANSOMWARE_SCORE, ransomwareVol.doubleValue() * 0.9);
            totalScore += ransomwareScore;
            components.add(RiskScore.ScoreComponent.builder()
                    .dimension("ransomware")
                    .value(ransomwareScore)
                    .explanation("Ransomware activity: " + ransomwareVol + "%")
                    .build());
            tags.add(RiskScore.RiskTag.builder()
                    .code("RANSOMWARE")
                    .severity(RiskScore.RiskTag.Severity.CRITICAL)
                    .description("Ransomware payment or distribution")
                    .build());
        }

        // Terrorist financing - critical
        BigDecimal terroristVol = analysis.getIllicitCategoryVolumes()
                .getOrDefault(AddressAnalysis.IllicitCategory.TERRORIST_FINANCING, BigDecimal.ZERO);
        if (terroristVol.compareTo(BigDecimal.ZERO) > 0) {
            int terroristScore = (int) Math.min(MAX_TERRORIST_FINANCING_SCORE, terroristVol.doubleValue() * 1.0);
            totalScore += terroristScore;
            components.add(RiskScore.ScoreComponent.builder()
                    .dimension("terrorist_financing")
                    .value(terroristScore)
                    .explanation("Terrorist financing exposure: " + terroristVol + "%")
                    .build());
            tags.add(RiskScore.RiskTag.builder()
                    .code("TERRORIST_FINANCING")
                    .severity(RiskScore.RiskTag.Severity.CRITICAL)
                    .description("Terrorist financing activity")
                    .build());
        }

        return totalScore;
    }

    private int calculateTemporalAdjustment(AddressAnalysis analysis,
                                           List<RiskScore.ScoreComponent> components) {
        if (analysis.getTemporalMetrics() == null ||
            analysis.getTemporalMetrics().getLastIllicitTxDaysAgo() == null) {
            return 0;
        }

        long daysAgo = analysis.getTemporalMetrics().getLastIllicitTxDaysAgo();
        int adjustment = 0;
        String explanation = "";

        // Old activity (>1 year) - reduce score
        if (daysAgo > 365) {
            adjustment = TIME_DECAY_BONUS;
            explanation = "Last illicit activity over 1 year ago - time decay applied";
        }
        // Recent activity (<30 days) - increase score
        else if (daysAgo < 30) {
            adjustment = RECENT_ACTIVITY_PENALTY;
            explanation = "Recent illicit activity within 30 days";
        }

        if (adjustment != 0) {
            components.add(RiskScore.ScoreComponent.builder()
                    .dimension("time_adjustment")
                    .value(adjustment)
                    .explanation(explanation)
                    .build());
        }

        return adjustment;
    }

    private int calculateBehavioralScore(AddressAnalysis analysis,
                                        List<RiskScore.ScoreComponent> components,
                                        List<RiskScore.RiskTag> tags) {
        int score = 0;

        if (analysis.getBehavioralMetrics() == null) {
            return 0;
        }

        // Peel chain pattern (UTXO-based chains) - suspicious
        if (analysis.getBehavioralMetrics().isHasPeelChainPattern() &&
            analysis.getBehavioralMetrics().getPeelChainLength() > 5) {
            score += 5;
            tags.add(RiskScore.RiskTag.builder()
                    .code("PEEL_CHAIN")
                    .severity(RiskScore.RiskTag.Severity.MEDIUM)
                    .description("Peel chain pattern detected - potential obfuscation")
                    .build());
        }

        // High fan-out (>50) with low fan-in - potential distribution
        if (analysis.getBehavioralMetrics().getFanOutDegree() > 50 &&
            analysis.getBehavioralMetrics().getFanInOutRatio() < 0.2) {
            score += 3;
            tags.add(RiskScore.RiskTag.builder()
                    .code("DISTRIBUTION_PATTERN")
                    .severity(RiskScore.RiskTag.Severity.LOW)
                    .description("Distribution pattern - funds splitting")
                    .build());
        }

        if (score > 0) {
            components.add(RiskScore.ScoreComponent.builder()
                    .dimension("behavioral")
                    .value(score)
                    .explanation("Behavioral red flags detected")
                    .build());
        }

        return score;
    }

    private BigDecimal calculateTotalIllicitVolume(AddressAnalysis analysis) {
        if (analysis.getIllicitCategoryVolumes() == null) {
            return BigDecimal.ZERO;
        }

        return analysis.getIllicitCategoryVolumes().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
