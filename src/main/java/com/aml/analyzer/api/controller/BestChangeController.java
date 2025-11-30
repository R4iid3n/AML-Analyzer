package com.aml.analyzer.api.controller;

import com.aml.analyzer.api.dto.AddressCheckRequest;
import com.aml.analyzer.api.dto.AddressCheckResponse;
import com.aml.analyzer.api.dto.BestChangeRequest;
import com.aml.analyzer.api.dto.BestChangeResponse;
import com.aml.analyzer.application.service.AddressAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * BestChange adapter endpoint.
 *
 * This is the compatibility layer that makes our analyzer a drop-in replacement
 * for Chainalysis, CoinKYT, GetBlock in the BestChange platform.
 *
 * Key advantages we bring to BestChange users:
 * - Support for 80+ chains (vs GetBlock's limited set)
 * - Transparent breakdown (vs Chainalysis OFAC-only free tier)
 * - Better explainability than CoinKYT
 * - Developer-friendly API (vs GetBlock's no-API approach)
 */
@Slf4j
@RestController
@RequestMapping("/partner/bestchange")
@RequiredArgsConstructor
public class BestChangeController {

    private final AddressAnalysisService analysisService;

    /**
     * Main BestChange integration endpoint.
     *
     * Example request from BestChange:
     * POST /partner/bestchange/check
     * {
     *   "address": "0x1234...",
     *   "asset": "ETH",
     *   "email": "user@example.com"
     * }
     *
     * Returns:
     * {
     *   "success": true,
     *   "risk_score": 78,
     *   "risk_level": "HIGH",
     *   "risk_tags": ["Sanctions", "Mixer", "Scam"],
     *   "report_url": "https://youraml.com/report/abc123"
     * }
     */
    @PostMapping("/check")
    public ResponseEntity<BestChangeResponse> checkAddress(
            @Valid @RequestBody BestChangeRequest request) {

        log.info("BestChange request for address: {} (asset: {}, user: {})",
                request.getAddress(), request.getAsset(), request.getEmail());

        try {
            // Convert BestChange request to our internal format
            AddressCheckRequest internalRequest = convertToInternalRequest(request);

            // Perform analysis using our core service
            AddressCheckResponse internalResponse = analysisService.analyzeAddress(internalRequest);

            // Convert back to BestChange format
            BestChangeResponse response = convertToBestChangeResponse(internalResponse);

            log.info("BestChange response: risk_score={}, risk_level={}",
                    response.getRiskScore(), response.getRiskLevel());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing BestChange request", e);

            return ResponseEntity.ok(BestChangeResponse.builder()
                    .success(false)
                    .message("Analysis failed: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Webhook callback for BestChange to receive async updates.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody BestChangeWebhookPayload payload) {
        log.info("Received webhook from BestChange: {}", payload);
        // Handle webhook if needed
        return ResponseEntity.ok().build();
    }

    private AddressCheckRequest convertToInternalRequest(BestChangeRequest bcRequest) {
        return AddressCheckRequest.builder()
                .address(bcRequest.getAddress())
                .asset(bcRequest.getAsset())
                .network(inferNetwork(bcRequest))
                .includeCluster(true)
                .generateReport(true)
                .build();
    }

    private String inferNetwork(BestChangeRequest request) {
        // Infer network from asset if not provided
        if (request.getNetwork() != null) {
            return request.getNetwork();
        }

        // Default network mappings
        String asset = request.getAsset().toUpperCase();
        return switch (asset) {
            case "BTC" -> "mainnet";
            case "ETH", "USDT", "USDC", "DAI" -> "ethereum";
            case "TRX" -> "tron";
            case "SOL" -> "solana";
            case "BNB" -> "bsc";
            case "MATIC" -> "polygon";
            case "ARB" -> "arbitrum";
            default -> "mainnet";
        };
    }

    private BestChangeResponse convertToBestChangeResponse(AddressCheckResponse internal) {
        return BestChangeResponse.builder()
                .success(internal.getStatus() == AddressCheckResponse.AnalysisStatus.COMPLETED)
                .riskScore(internal.getRiskScore())
                .riskLevel(internal.getRiskLevel())
                .riskTags(internal.getTags() != null ?
                        internal.getTags().stream()
                                .map(AddressCheckResponse.RiskTagDto::getCode)
                                .map(this::formatTagForBestChange)
                                .collect(Collectors.toList()) :
                        null)
                .reportUrl(internal.getReportHtmlUrl() != null ?
                        internal.getReportHtmlUrl() : internal.getReportUrl())
                .illicitVolumePct(internal.getIllicitVolumePct() != null ?
                        internal.getIllicitVolumePct().toString() + "%" : null)
                .lastIllicitActivity(null) // TODO: extract from temporal metrics
                .build();
    }

    private String formatTagForBestChange(String code) {
        // Convert our internal codes to user-friendly labels
        return switch (code) {
            case "DIRECT_SANCTIONS" -> "Sanctions";
            case "SANCTIONS_1HOP", "SANCTIONS_2_4HOP" -> "Sanctions";
            case "MIXER_USAGE" -> "Mixer";
            case "STOLEN_FUNDS" -> "Stolen coins";
            case "DARKNET" -> "Dark market";
            case "SCAM" -> "Scam";
            case "RANSOMWARE" -> "Ransomware";
            case "TERRORIST_FINANCING" -> "Terrorist financing";
            default -> code.replace("_", " ");
        };
    }

    // Inner class for webhook payload
    @Data
    public static class BestChangeWebhookPayload {
        private String requestId;
        private String status;
    }
}
