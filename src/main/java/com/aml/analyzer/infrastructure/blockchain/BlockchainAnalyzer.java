package com.aml.analyzer.infrastructure.blockchain;

import com.aml.analyzer.domain.model.AddressAnalysis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface for blockchain-specific analyzers.
 *
 * Competitive advantage:
 * - Multi-chain support (BTC, ETH, TRON, SOL, BSC, ARB, etc.)
 * - GetBlock only supports BTC, ZEC, LTC, BCH, DASH
 * - We cover 80+ assets like BestChange wants
 *
 * Each chain has its own implementation:
 * - BitcoinAnalyzer (UTXO-based analysis, peel chains)
 * - EthereumAnalyzer (account-based, ERC-20 tokens, DeFi)
 * - TronAnalyzer (TRC-20 tokens)
 * - SolanaAnalyzer (SPL tokens)
 */
public interface BlockchainAnalyzer {

    /**
     * Analyze address and return transaction history with risk signals.
     */
    AddressTransactionHistory getTransactionHistory(String address, int maxTransactions);

    /**
     * Get counterparties (addresses that sent/received from target).
     */
    List<Counterparty> getCounterparties(String address);

    /**
     * Trace funds - follow the money forward/backward N hops.
     */
    FundFlow traceFunds(String address, int hops, Direction direction);

    /**
     * Detect behavioral patterns (peel chains, mixers, etc.).
     */
    BehavioralPatterns detectPatterns(String address);

    /**
     * Calculate behavioral metrics.
     */
    AddressAnalysis.BehavioralMetrics calculateBehavioralMetrics(String address);

    /**
     * Get temporal metrics.
     */
    AddressAnalysis.TemporalMetrics calculateTemporalMetrics(String address);

    enum Direction {
        FORWARD,   // Where did funds go?
        BACKWARD   // Where did funds come from?
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AddressTransactionHistory {
        private String address;
        private List<Transaction> transactions;
        private BigDecimal totalReceived;
        private BigDecimal totalSent;
        private BigDecimal currentBalance;
        private LocalDateTime firstSeen;
        private LocalDateTime lastActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class Transaction {
        private String txHash;
        private LocalDateTime timestamp;
        private BigDecimal amount;
        private String from;
        private String to;
        private TransactionType type;
        private List<String> tags;  // "mixer", "exchange", "darknet", etc.

        public enum TransactionType {
            RECEIVED, SENT, INTERNAL
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class Counterparty {
        private String address;
        private int transactionCount;
        private BigDecimal totalVolume;
        private List<String> tags;  // Risk tags
        private LocalDateTime firstInteraction;
        private LocalDateTime lastInteraction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class FundFlow {
        private String startAddress;
        private Direction direction;
        private int hops;
        private List<FlowNode> nodes;
        private BigDecimal totalVolume;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class FlowNode {
        private String address;
        private int hopDistance;
        private BigDecimal volume;
        private List<String> tags;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class BehavioralPatterns {
        private boolean hasPeelChain;
        private int peelChainLength;
        private boolean hasMixerUsage;
        private List<String> mixersUsed;
        private boolean hasRoundAmounts;  // Suspicious: always .0000 amounts
        private boolean hasHighFrequency;  // Many txs in short time
        private boolean hasFanOutPattern;  // 1-to-many distribution
    }
}
