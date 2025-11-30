package com.aml.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Advanced AML Analyzer - Main Application
 *
 * A next-generation AML/KYC compliance tool that outperforms competitors:
 *
 * KEY DIFFERENTIATORS:
 *
 * 1. Cross-Chain Entity Intelligence
 *    - Tracks entities across BTC, ETH, TRON, SOL, etc.
 *    - Competitors analyze addresses in isolation
 *
 * 2. Transparent, Explainable Scoring
 *    - Component-by-component breakdown
 *    - "Why did score change?" audit trail
 *    - Competitors give opaque scores
 *
 * 3. Superior Coverage
 *    - 80+ cryptocurrencies & tokens
 *    - GetBlock: only BTC, ZEC, LTC, BCH, DASH
 *    - Chainalysis free: OFAC sanctions only
 *
 * 4. Developer-Friendly
 *    - Clean REST API with full JSON responses
 *    - GetBlock has NO API
 *    - Webhooks, bulk analysis, async support
 *
 * 5. Compliance-Ready Reports
 *    - Professional HTML/PDF reports
 *    - Audit trail of score changes
 *    - Legal disclaimers & recommendations
 *    - Better than CoinKYT/GetBlock PDFs
 *
 * 6. Multi-Dimensional Risk Model
 *    - Sanctions (direct, 1-hop, 2-4 hop)
 *    - Illicit categories (FATF taxonomy)
 *    - Behavioral patterns (peel chains, mixers)
 *    - Temporal analysis (time decay)
 *    - Volume-weighted scoring
 *
 * 7. False-Positive Control
 *    - Time decay for old activity
 *    - Hop distance weighting
 *    - Not just "touched mixer once = red forever"
 *
 * INTEGRATION:
 * - BestChange adapter endpoint: /partner/bestchange/check
 * - Main API: /v1/check-address
 * - Audit trail: /v1/address-history/{address}
 *
 * TECH STACK:
 * - Spring Boot 3.2
 * - PostgreSQL (analysis storage, audit trail)
 * - Redis (caching)
 * - RabbitMQ (async processing)
 * - Web3j (Ethereum)
 * - Bitcoin RPC (Bitcoin)
 * - Public APIs (TRON, Solana, BSC, etc.)
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class AmlAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmlAnalyzerApplication.class, args);

        System.out.println("""

                ╔═══════════════════════════════════════════════════════════════╗
                ║                                                               ║
                ║         Advanced AML Analyzer - Started Successfully         ║
                ║                                                               ║
                ║  API Endpoints:                                               ║
                ║    - Main API:       POST /v1/check-address                   ║
                ║    - BestChange:     POST /partner/bestchange/check           ║
                ║    - Audit Trail:    GET  /v1/address-history/{address}       ║
                ║    - Health:         GET  /actuator/health                    ║
                ║                                                               ║
                ║  Key Features:                                                ║
                ║    ✓ Cross-chain entity clustering                            ║
                ║    ✓ Transparent, explainable risk scoring                    ║
                ║    ✓ 80+ cryptocurrencies supported                           ║
                ║    ✓ Audit trail (score change history)                       ║
                ║    ✓ Professional HTML/PDF reports                            ║
                ║    ✓ Multi-dimensional risk model                             ║
                ║    ✓ FATF-compliant illicit categories                        ║
                ║    ✓ Time decay & false-positive control                      ║
                ║                                                               ║
                ║  Advantages over competitors:                                 ║
                ║    → Chainalysis:  Free tier = OFAC only; we have full model ║
                ║    → GetBlock:     No API; limited chains; we support 80+    ║
                ║    → CoinKYT:      No cross-chain; we have entity clusters   ║
                ║                                                               ║
                ╚═══════════════════════════════════════════════════════════════╝
                """);
    }
}
