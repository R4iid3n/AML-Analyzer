package com.aml.analyzer.application.service;

import com.aml.analyzer.api.dto.AddressCheckRequest;
import com.aml.analyzer.api.dto.AddressCheckResponse;
import com.aml.analyzer.domain.model.AddressAnalysis;
import com.aml.analyzer.domain.model.EntityCluster;
import com.aml.analyzer.domain.model.RiskScore;
import com.aml.analyzer.domain.service.EntityClusteringService;
import com.aml.analyzer.domain.service.RiskScoringEngine;
import com.aml.analyzer.infrastructure.persistence.entity.AddressAnalysisEntity;
import com.aml.analyzer.infrastructure.persistence.repository.AddressAnalysisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Main application service for address analysis.
 *
 * Orchestrates:
 * - Blockchain data fetching
 * - Risk scoring
 * - Entity clustering
 * - Report generation
 * - Audit trail
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressAnalysisService {

    private final RiskScoringEngine riskScoringEngine;
    private final EntityClusteringService clusteringService;
    private final ReportGenerationService reportService;
    private final AddressAnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;

    /**
     * Analyze an address and return risk assessment.
     *
     * This is the main entry point.
     */
    @Transactional
    public AddressCheckResponse analyzeAddress(AddressCheckRequest request) {
        log.info("Analyzing address: {} on {}/{}", request.getAddress(), request.getAsset(), request.getNetwork());

        String requestId = request.getRequestId() != null ?
                request.getRequestId() : UUID.randomUUID().toString();

        try {
            // 1. Fetch blockchain data
            AddressAnalysis analysis = fetchBlockchainData(request);

            // 2. Find or create entity cluster
            EntityCluster cluster = null;
            if (request.isIncludeCluster()) {
                cluster = clusteringService.findClusterForAddress(
                        request.getAddress(),
                        request.getAsset(),
                        request.getNetwork()
                ).orElse(null);
            }

            // 3. Calculate risk score
            RiskScore riskScore = riskScoringEngine.calculateRisk(analysis);

            // 4. Generate report
            String reportHtml = null;
            if (request.isGenerateReport()) {
                reportHtml = reportService.generateHtmlReport(analysis, riskScore, cluster);
            }

            // 5. Check for previous analysis (audit trail)
            Optional<AddressAnalysisEntity> previousAnalysis =
                    analysisRepository.findFirstByAddressAndAssetAndNetworkOrderByAnalyzedAtDesc(
                            request.getAddress(),
                            request.getAsset(),
                            request.getNetwork()
                    );

            AddressCheckResponse.ScoreHistory scoreHistory = null;
            if (previousAnalysis.isPresent()) {
                scoreHistory = buildScoreHistory(previousAnalysis.get(), riskScore);
            }

            // 6. Save analysis
            AddressAnalysisEntity entity = saveAnalysis(request, analysis, riskScore, reportHtml);

            // 7. Build response
            AddressCheckResponse response = buildResponse(
                    requestId,
                    request,
                    analysis,
                    riskScore,
                    entity,
                    scoreHistory,
                    cluster
            );

            // 8. Send webhook if requested
            if (request.getWebhookUrl() != null) {
                sendWebhook(request.getWebhookUrl(), response);
            }

            log.info("Analysis completed for {}: risk_score={}, risk_level={}",
                    request.getAddress(), riskScore.getTotalScore(), riskScore.getRiskLevel());

            return response;

        } catch (Exception e) {
            log.error("Error analyzing address: {}", request.getAddress(), e);

            return AddressCheckResponse.builder()
                    .requestId(requestId)
                    .status(AddressCheckResponse.AnalysisStatus.FAILED)
                    .address(request.getAddress())
                    .asset(request.getAsset())
                    .network(request.getNetwork())
                    .build();
        }
    }

    /**
     * Get analysis status for async requests.
     */
    public AddressCheckResponse getAnalysisStatus(String requestId) {
        // TODO: Implement async status tracking
        throw new UnsupportedOperationException("Async analysis not yet implemented");
    }

    /**
     * Bulk analysis - for exchanges.
     */
    public BulkAnalysisResponse analyzeBulk(BulkAnalysisRequest request) {
        // TODO: Implement bulk analysis with queue
        throw new UnsupportedOperationException("Bulk analysis not yet implemented");
    }

    /**
     * Get address history - audit trail.
     */
    public AddressHistoryResponse getAddressHistory(String address, String asset, String network) {
        log.info("Fetching history for: {} on {}/{}", address, asset, network);

        List<AddressAnalysisEntity> history =
                analysisRepository.findByAddressAndAssetAndNetworkOrderByAnalyzedAtDesc(
                        address, asset, network
                );

        List<AddressHistoryResponse.HistoryEntry> entries = history.stream()
                .map(this::toHistoryEntry)
                .collect(Collectors.toList());

        return AddressHistoryResponse.builder()
                .address(address)
                .asset(asset)
                .network(network)
                .totalChecks(entries.size())
                .history(entries)
                .build();
    }

    private AddressAnalysis fetchBlockchainData(AddressCheckRequest request) {
        // TODO: Use appropriate blockchain analyzer based on asset/network
        // For now, return stub data

        return AddressAnalysis.builder()
                .address(request.getAddress())
                .asset(request.getAsset())
                .network(request.getNetwork())
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    private AddressAnalysisEntity saveAnalysis(
            AddressCheckRequest request,
            AddressAnalysis analysis,
            RiskScore riskScore,
            String reportHtml) {

        try {
            AddressAnalysisEntity entity = AddressAnalysisEntity.builder()
                    .address(request.getAddress())
                    .asset(request.getAsset())
                    .network(request.getNetwork())
                    .clusterId(analysis.getClusterId())
                    .riskScore(riskScore.getTotalScore())
                    .riskLevel(riskScore.getRiskLevel().name())
                    .illicitVolumePct(riskScore.getIllicitVolumePct() != null ?
                            riskScore.getIllicitVolumePct().toString() : null)
                    .scoreBreakdown(objectMapper.writeValueAsString(riskScore.getScoreBreakdown()))
                    .riskTags(objectMapper.writeValueAsString(riskScore.getTags()))
                    .analysisData(objectMapper.writeValueAsString(analysis))
                    .analyzedAt(LocalDateTime.now())
                    .reportUrl(reportHtml != null ? "/reports/" + UUID.randomUUID() : null)
                    .build();

            return analysisRepository.save(entity);

        } catch (Exception e) {
            log.error("Error saving analysis", e);
            throw new RuntimeException("Failed to save analysis", e);
        }
    }

    private AddressCheckResponse buildResponse(
            String requestId,
            AddressCheckRequest request,
            AddressAnalysis analysis,
            RiskScore riskScore,
            AddressAnalysisEntity entity,
            AddressCheckResponse.ScoreHistory scoreHistory,
            EntityCluster cluster) {

        return AddressCheckResponse.builder()
                .requestId(requestId)
                .status(AddressCheckResponse.AnalysisStatus.COMPLETED)
                .checkedAt(LocalDateTime.now())
                .address(request.getAddress())
                .asset(request.getAsset())
                .network(request.getNetwork())
                .clusterId(cluster != null ? cluster.getClusterId() : null)
                .riskScore(riskScore.getTotalScore())
                .riskLevel(riskScore.getRiskLevel().name())
                .scoreBreakdown(riskScore.getScoreBreakdown().stream()
                        .map(c -> AddressCheckResponse.ScoreBreakdownDto.builder()
                                .dimension(c.getDimension())
                                .value(c.getValue())
                                .explanation(c.getExplanation())
                                .build())
                        .collect(Collectors.toList()))
                .tags(riskScore.getTags().stream()
                        .map(t -> AddressCheckResponse.RiskTagDto.builder()
                                .code(t.getCode())
                                .severity(t.getSeverity().name())
                                .description(t.getDescription())
                                .build())
                        .collect(Collectors.toList()))
                .illicitVolumePct(riskScore.getIllicitVolumePct())
                .cleanVolumePct(riskScore.getCleanVolumePct())
                .reportUrl(entity.getReportUrl())
                .reportHtmlUrl(entity.getReportUrl())
                .scoreHistory(scoreHistory)
                .build();
    }

    private AddressCheckResponse.ScoreHistory buildScoreHistory(
            AddressAnalysisEntity previous,
            RiskScore current) {

        int scoreDelta = current.getTotalScore() - previous.getRiskScore();
        long daysSinceLastCheck = ChronoUnit.DAYS.between(
                previous.getAnalyzedAt(),
                LocalDateTime.now()
        );

        String changeReason = determineChangeReason(previous, current, scoreDelta);

        return AddressCheckResponse.ScoreHistory.builder()
                .previousScore(previous.getRiskScore())
                .previousRiskLevel(previous.getRiskLevel())
                .previousCheckAt(previous.getAnalyzedAt())
                .scoreDelta(scoreDelta)
                .changeReason(changeReason)
                .build();
    }

    private String determineChangeReason(
            AddressAnalysisEntity previous,
            RiskScore current,
            int scoreDelta) {

        if (scoreDelta == 0) {
            return "No change in risk score";
        } else if (scoreDelta > 0) {
            return "Risk increased: new illicit activity detected";
        } else {
            return "Risk decreased: time decay applied to old activity";
        }
    }

    private AddressHistoryResponse.HistoryEntry toHistoryEntry(AddressAnalysisEntity entity) {
        return AddressHistoryResponse.HistoryEntry.builder()
                .id(entity.getId())
                .riskScore(entity.getRiskScore())
                .riskLevel(entity.getRiskLevel())
                .analyzedAt(entity.getAnalyzedAt())
                .reportUrl(entity.getReportUrl())
                .build();
    }

    private void sendWebhook(String webhookUrl, AddressCheckResponse response) {
        // TODO: Implement webhook sending
        log.info("Would send webhook to: {}", webhookUrl);
    }

    // Placeholder classes - define these in separate files
    public static class BulkAnalysisRequest {
        private List<AddressCheckRequest> addresses;

        public List<AddressCheckRequest> getAddresses() {
            return addresses;
        }
    }

    public static class BulkAnalysisResponse {
        // TODO
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AddressHistoryResponse {
        private String address;
        private String asset;
        private String network;
        private int totalChecks;
        private List<HistoryEntry> history;

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class HistoryEntry {
            private String id;
            private Integer riskScore;
            private String riskLevel;
            private LocalDateTime analyzedAt;
            private String reportUrl;
        }
    }
}
