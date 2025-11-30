package com.aml.analyzer.domain.service;

import com.aml.analyzer.domain.model.graph.EgoGraph;
import com.aml.analyzer.domain.model.graph.GraphEntity;
import com.aml.analyzer.domain.model.graph.GraphTransaction;
import com.aml.analyzer.infrastructure.blockchain.BlockchainAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Ego graph builder - constructs subgraph around target entity.
 *
 * Algorithm:
 * 1. Start from target entity
 * 2. BFS/DFS to depth N (default: 3)
 * 3. Only include transactions within time window T (default: 180 days)
 * 4. Build adjacency structures for fast traversal
 * 5. Classify entities (mixer, CEX, sanctioned, etc.)
 * 6. Compute graph metrics (PageRank, clustering coefficient)
 *
 * This ego graph is then used for:
 * - Pattern matching (automata)
 * - ML feature extraction
 * - Risk scoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EgoGraphBuilder {

    private final BlockchainAnalyzer blockchainAnalyzer;  // Injected based on chain

    private static final int DEFAULT_MAX_DEPTH = 3;
    private static final long DEFAULT_TIME_WINDOW_DAYS = 180;

    /**
     * Build ego graph around target entity.
     */
    public EgoGraph buildEgoGraph(
            String entityId,
            String asset,
            String network) {

        return buildEgoGraph(entityId, asset, network, DEFAULT_MAX_DEPTH, DEFAULT_TIME_WINDOW_DAYS);
    }

    /**
     * Build ego graph with custom parameters.
     */
    public EgoGraph buildEgoGraph(
            String entityId,
            String asset,
            String network,
            int maxDepth,
            long timeWindowDays) {

        log.info("Building ego graph: entity={}, depth={}, window={} days",
                entityId, maxDepth, timeWindowDays);

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(timeWindowDays);

        Map<String, GraphEntity> entities = new HashMap<>();
        List<GraphTransaction> transactions = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        // Create center entity
        GraphEntity centerEntity = createEntity(entityId, asset, network);
        entities.put(entityId, centerEntity);

        // BFS to collect entities and transactions
        Queue<QueueItem> queue = new LinkedList<>();
        queue.add(new QueueItem(entityId, 0));
        visited.add(entityId);

        while (!queue.isEmpty()) {
            QueueItem item = queue.poll();
            String currentEntityId = item.entityId;
            int depth = item.depth;

            if (depth >= maxDepth) {
                continue;
            }

            // Get transaction history for current entity
            BlockchainAnalyzer.AddressTransactionHistory history =
                    blockchainAnalyzer.getTransactionHistory(currentEntityId, 1000);

            // Process transactions
            for (BlockchainAnalyzer.Transaction tx : history.getTransactions()) {

                // Filter by time window
                if (tx.getTimestamp().isBefore(cutoffTime)) {
                    continue;
                }

                // Create graph transaction
                GraphTransaction graphTx = GraphTransaction.builder()
                        .txHash(tx.getTxHash())
                        .fromEntityId(tx.getFrom())
                        .toEntityId(tx.getTo())
                        .amount(tx.getAmount())
                        .asset(asset)
                        .timestamp(tx.getTimestamp())
                        .direction(determineDirection(currentEntityId, tx))
                        .isCrossBridge(isBridgeTransaction(tx))
                        .isMixerHop(isMixerTransaction(tx))
                        .build();

                transactions.add(graphTx);

                // Add counterparty entity
                String counterpartyId = graphTx.getDirection() == GraphTransaction.TransactionDirection.OUTGOING ?
                        tx.getTo() : tx.getFrom();

                if (!visited.contains(counterpartyId)) {
                    GraphEntity counterpartyEntity = createEntity(counterpartyId, asset, network);
                    entities.put(counterpartyId, counterpartyEntity);
                    visited.add(counterpartyId);

                    // Add to queue for next level
                    queue.add(new QueueItem(counterpartyId, depth + 1));
                }
            }
        }

        log.info("Ego graph built: {} entities, {} transactions",
                entities.size(), transactions.size());

        // Build adjacency structures
        Map<String, List<GraphTransaction>> outgoingEdges = new HashMap<>();
        Map<String, List<GraphTransaction>> incomingEdges = new HashMap<>();

        for (GraphTransaction tx : transactions) {
            outgoingEdges.computeIfAbsent(tx.getFromEntityId(), k -> new ArrayList<>()).add(tx);
            incomingEdges.computeIfAbsent(tx.getToEntityId(), k -> new ArrayList<>()).add(tx);
        }

        // Compute graph metrics
        computeGraphMetrics(entities, outgoingEdges, incomingEdges);

        return EgoGraph.builder()
                .centerEntityId(entityId)
                .entities(entities)
                .transactions(transactions)
                .outgoingEdges(outgoingEdges)
                .incomingEdges(incomingEdges)
                .maxDepth(maxDepth)
                .timeWindowDays(timeWindowDays)
                .build();
    }

    /**
     * Create graph entity from address.
     */
    private GraphEntity createEntity(String entityId, String asset, String network) {
        // TODO: Query entity classification service
        // For now, use simple heuristics

        GraphEntity.EntityType type = classifyEntityType(entityId);
        GraphEntity.EntityCategory category = classifyEntityCategory(entityId);
        Set<String> tags = getEntityTags(entityId);

        return GraphEntity.builder()
                .id(entityId)
                .type(type)
                .chain(network)
                .category(category)
                .tags(tags)
                .inDegree(0)  // Will be computed
                .outDegree(0)
                .pageRank(0.0)
                .clusteringCoefficient(0.0)
                .build();
    }

    /**
     * Classify entity type.
     */
    private GraphEntity.EntityType classifyEntityType(String entityId) {
        // TODO: Query classification DB
        // Simple heuristics for now

        if (isKnownMixer(entityId)) return GraphEntity.EntityType.MIXER;
        if (isKnownCEX(entityId)) return GraphEntity.EntityType.CEX;
        if (isKnownBridge(entityId)) return GraphEntity.EntityType.BRIDGE;
        if (isContract(entityId)) return GraphEntity.EntityType.CONTRACT;

        return GraphEntity.EntityType.EOA;
    }

    /**
     * Classify entity category (for pattern matching).
     */
    private GraphEntity.EntityCategory classifyEntityCategory(String entityId) {
        if (isSanctioned(entityId)) return GraphEntity.EntityCategory.SANCTIONED;
        if (isKnownMixer(entityId)) return GraphEntity.EntityCategory.MIXER;
        if (isKnownBridge(entityId)) return GraphEntity.EntityCategory.BRIDGE;
        if (isHighRiskCEX(entityId)) return GraphEntity.EntityCategory.CEX_HIGH_RISK;
        if (isCompliantCEX(entityId)) return GraphEntity.EntityCategory.CEX_COMPLIANT;
        if (isDarknet(entityId)) return GraphEntity.EntityCategory.DARKNET;
        if (isScam(entityId)) return GraphEntity.EntityCategory.SCAM;

        return GraphEntity.EntityCategory.CLEAN;
    }

    /**
     * Get entity tags.
     */
    private Set<String> getEntityTags(String entityId) {
        Set<String> tags = new HashSet<>();

        if (isSanctioned(entityId)) tags.add("SANCTIONED");
        if (isKnownMixer(entityId)) tags.add("MIXER");
        if (isDarknet(entityId)) tags.add("DARKNET");
        if (isScam(entityId)) tags.add("SCAM");

        return tags;
    }

    /**
     * Compute graph metrics (degrees, PageRank, etc.).
     */
    private void computeGraphMetrics(
            Map<String, GraphEntity> entities,
            Map<String, List<GraphTransaction>> outgoingEdges,
            Map<String, List<GraphTransaction>> incomingEdges) {

        // Update degrees
        for (GraphEntity entity : entities.values()) {
            entity.setInDegree(incomingEdges.getOrDefault(entity.getId(), Collections.emptyList()).size());
            entity.setOutDegree(outgoingEdges.getOrDefault(entity.getId(), Collections.emptyList()).size());
        }

        // TODO: Compute PageRank (iterative algorithm)
        // TODO: Compute clustering coefficient
        // For now, set defaults
        for (GraphEntity entity : entities.values()) {
            entity.setPageRank(1.0 / entities.size());  // Uniform
            entity.setClusteringCoefficient(0.0);
        }
    }

    private GraphTransaction.TransactionDirection determineDirection(
            String entityId,
            BlockchainAnalyzer.Transaction tx) {
        if (tx.getFrom().equals(entityId)) {
            return GraphTransaction.TransactionDirection.OUTGOING;
        } else if (tx.getTo().equals(entityId)) {
            return GraphTransaction.TransactionDirection.INCOMING;
        } else {
            return GraphTransaction.TransactionDirection.INTERNAL;
        }
    }

    private boolean isBridgeTransaction(BlockchainAnalyzer.Transaction tx) {
        // TODO: Check if transaction goes through known bridge
        return false;
    }

    private boolean isMixerTransaction(BlockchainAnalyzer.Transaction tx) {
        // TODO: Check if transaction involves mixer
        return tx.getTags() != null && tx.getTags().contains("mixer");
    }

    // Entity classification helpers (connect to real DBs in production)
    private boolean isKnownMixer(String entityId) {
        // TODO: Query mixer DB
        return false;
    }

    private boolean isKnownCEX(String entityId) {
        return false;
    }

    private boolean isKnownBridge(String entityId) {
        return false;
    }

    private boolean isContract(String entityId) {
        // Ethereum: starts with 0x and is contract
        return false;
    }

    private boolean isSanctioned(String entityId) {
        // TODO: Query sanctions service
        return false;
    }

    private boolean isHighRiskCEX(String entityId) {
        return false;
    }

    private boolean isCompliantCEX(String entityId) {
        return false;
    }

    private boolean isDarknet(String entityId) {
        return false;
    }

    private boolean isScam(String entityId) {
        return false;
    }

    @lombok.AllArgsConstructor
    private static class QueueItem {
        String entityId;
        int depth;
    }
}
