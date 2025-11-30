package com.aml.analyzer.api.controller;

import com.aml.analyzer.api.dto.AddressCheckRequest;
import com.aml.analyzer.api.dto.AddressCheckResponse;
import com.aml.analyzer.application.service.AddressAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Main API controller for address analysis.
 *
 * Key advantages over competitors:
 * - Rich, structured JSON response with full breakdown
 * - Async support with webhooks for heavy analysis
 * - Audit trail showing score changes
 * - Multiple report formats (JSON, HTML, PDF)
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class AddressAnalysisController {

    private final AddressAnalysisService analysisService;

    /**
     * Main endpoint - analyze an address and return risk score.
     *
     * Example request:
     * POST /v1/check-address
     * {
     *   "address": "0x1234...",
     *   "asset": "ETH",
     *   "network": "mainnet",
     *   "include_cluster": true,
     *   "webhook_url": "https://client.com/hooks/aml",
     *   "request_id": "client-uuid-123"
     * }
     */
    @PostMapping("/check-address")
    public ResponseEntity<AddressCheckResponse> checkAddress(
            @Valid @RequestBody AddressCheckRequest request) {

        log.info("Received address check request: {} on {}/{}",
                request.getAddress(), request.getAsset(), request.getNetwork());

        AddressCheckResponse response = analysisService.analyzeAddress(request);

        return ResponseEntity
                .status(response.getStatus() == AddressCheckResponse.AnalysisStatus.COMPLETED ?
                        HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(response);
    }

    /**
     * Get analysis status for async requests.
     */
    @GetMapping("/check-address/{requestId}")
    public ResponseEntity<AddressCheckResponse> getAnalysisStatus(
            @PathVariable String requestId) {

        log.info("Fetching analysis status for request: {}", requestId);

        AddressCheckResponse response = analysisService.getAnalysisStatus(requestId);

        return ResponseEntity.ok(response);
    }

    /**
     * Bulk analysis endpoint - for exchanges checking multiple addresses.
     */
    @PostMapping("/check-addresses/bulk")
    public ResponseEntity<BulkAnalysisResponse> checkAddressesBulk(
            @Valid @RequestBody BulkAnalysisRequest request) {

        log.info("Received bulk analysis request with {} addresses",
                request.getAddresses().size());

        BulkAnalysisResponse response = analysisService.analyzeBulk(request);

        return ResponseEntity.accepted().body(response);
    }

    /**
     * Get historical risk scores for an address - audit trail.
     */
    @GetMapping("/address-history/{address}")
    public ResponseEntity<AddressHistoryResponse> getAddressHistory(
            @PathVariable String address,
            @RequestParam String asset,
            @RequestParam String network) {

        log.info("Fetching history for address: {} on {}/{}", address, asset, network);

        AddressHistoryResponse response = analysisService.getAddressHistory(address, asset, network);

        return ResponseEntity.ok(response);
    }
}
