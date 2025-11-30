package com.aml.analyzer.domain.model.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Ego graph - subgraph centered on a target entity.
 *
 * Contains all entities and transactions within N hops and T time window
 * from the target entity.
 *
 * This is the foundation for pattern matching:
 * - BFS/DFS traversal
 * - Automata state transitions
 * - Pattern detection
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EgoGraph {

    private String centerEntityId;

    private Map<String, GraphEntity> entities;  // entityId -> entity

    private List<GraphTransaction> transactions;

    // Adjacency structures for efficient traversal
    private Map<String, List<GraphTransaction>> outgoingEdges;  // entityId -> outgoing txs
    private Map<String, List<GraphTransaction>> incomingEdges;  // entityId -> incoming txs

    private int maxDepth;
    private long timeWindowDays;

    /**
     * Get all neighbors of an entity (1-hop).
     */
    public List<GraphEntity> getNeighbors(String entityId) {
        Set<String> neighborIds = new HashSet<>();

        if (outgoingEdges.containsKey(entityId)) {
            outgoingEdges.get(entityId).forEach(tx ->
                neighborIds.add(tx.getToEntityId())
            );
        }

        if (incomingEdges.containsKey(entityId)) {
            incomingEdges.get(entityId).forEach(tx ->
                neighborIds.add(tx.getFromEntityId())
            );
        }

        return neighborIds.stream()
                .map(entities::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get all outgoing transactions from an entity.
     */
    public List<GraphTransaction> getOutgoingTransactions(String entityId) {
        return outgoingEdges.getOrDefault(entityId, Collections.emptyList());
    }

    /**
     * Get all incoming transactions to an entity.
     */
    public List<GraphTransaction> getIncomingTransactions(String entityId) {
        return incomingEdges.getOrDefault(entityId, Collections.emptyList());
    }

    /**
     * Find all paths from source to target with max length.
     */
    public List<GraphPath> findPaths(String sourceId, String targetId, int maxLength) {
        List<GraphPath> paths = new ArrayList<>();
        List<GraphTransaction> currentPath = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        dfs(sourceId, targetId, maxLength, currentPath, visited, paths);

        return paths;
    }

    private void dfs(String current, String target, int remainingDepth,
                     List<GraphTransaction> currentPath, Set<String> visited,
                     List<GraphPath> results) {

        if (current.equals(target)) {
            results.add(GraphPath.builder()
                    .transactions(new ArrayList<>(currentPath))
                    .build());
            return;
        }

        if (remainingDepth <= 0) {
            return;
        }

        visited.add(current);

        for (GraphTransaction tx : getOutgoingTransactions(current)) {
            if (!visited.contains(tx.getToEntityId())) {
                currentPath.add(tx);
                dfs(tx.getToEntityId(), target, remainingDepth - 1, currentPath, visited, results);
                currentPath.remove(currentPath.size() - 1);
            }
        }

        visited.remove(current);
    }

    /**
     * Get all entities matching a category.
     */
    public List<GraphEntity> getEntitiesByCategory(GraphEntity.EntityCategory category) {
        return entities.values().stream()
                .filter(e -> e.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Get total volume through entity.
     */
    public double getTotalVolume(String entityId) {
        double total = 0.0;

        for (GraphTransaction tx : getOutgoingTransactions(entityId)) {
            total += tx.getAmount().doubleValue();
        }

        for (GraphTransaction tx : getIncomingTransactions(entityId)) {
            total += tx.getAmount().doubleValue();
        }

        return total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphPath {
        private List<GraphTransaction> transactions;

        public int getLength() {
            return transactions.size();
        }

        public double getTotalVolume() {
            return transactions.stream()
                    .mapToDouble(tx -> tx.getAmount().doubleValue())
                    .sum();
        }

        public List<String> getEntityIds() {
            List<String> ids = new ArrayList<>();
            if (!transactions.isEmpty()) {
                ids.add(transactions.get(0).getFromEntityId());
                transactions.forEach(tx -> ids.add(tx.getToEntityId()));
            }
            return ids;
        }
    }
}
