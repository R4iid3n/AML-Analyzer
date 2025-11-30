package com.aml.analyzer.domain.service;

import com.aml.analyzer.domain.model.graph.EgoGraph;
import com.aml.analyzer.domain.model.graph.GraphEntity;
import com.aml.analyzer.domain.model.graph.GraphTransaction;
import com.aml.analyzer.domain.model.pattern.PatternAutomaton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Pattern matching engine - executes automata on ego graphs.
 *
 * This is the KILLER FEATURE that crushes competitors:
 * - Graph-based pattern detection (not just feature aggregation)
 * - Automata for complex sequences (mixer → bridge → CEX)
 * - Regulatory narratives (placement, layering, integration)
 * - Much more expressive than "% from mixer"
 *
 * Algorithm:
 * 1. Build ego graph around target entity
 * 2. For each pattern automaton:
 *    - Start BFS/DFS from target
 *    - Carry automaton state along each path
 *    - When path reaches ACCEPT state → pattern matched
 * 3. Aggregate all matched patterns for scoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatternMatchingEngine {

    /**
     * Run all patterns on ego graph and return matches.
     */
    public List<PatternAutomaton.MatchResult> matchPatterns(
            EgoGraph egoGraph,
            List<PatternAutomaton> patterns) {

        log.info("Running {} patterns on ego graph for entity: {}",
                patterns.size(), egoGraph.getCenterEntityId());

        List<PatternAutomaton.MatchResult> results = new ArrayList<>();

        for (PatternAutomaton pattern : patterns) {
            PatternAutomaton.MatchResult result = matchPattern(egoGraph, pattern);
            if (result.isMatched()) {
                log.info("Pattern matched: {} (weight: {}, severity: {})",
                        result.getPatternName(), result.getWeight(), result.getSeverity());
            }
            results.add(result);
        }

        return results;
    }

    /**
     * Match a single pattern on ego graph.
     */
    private PatternAutomaton.MatchResult matchPattern(
            EgoGraph egoGraph,
            PatternAutomaton pattern) {

        String centerEntityId = egoGraph.getCenterEntityId();
        GraphEntity centerEntity = egoGraph.getEntities().get(centerEntityId);

        if (centerEntity == null) {
            return PatternAutomaton.MatchResult.noMatch(pattern.getPatternId());
        }

        // Start automaton traversal from center entity
        List<AutomatonPath> acceptedPaths = new ArrayList<>();

        exploreWithAutomaton(
                egoGraph,
                pattern,
                centerEntity,
                pattern.getInitialState(),
                new ArrayList<>(),
                new HashSet<>(),
                acceptedPaths
        );

        if (acceptedPaths.isEmpty()) {
            return PatternAutomaton.MatchResult.noMatch(pattern.getPatternId());
        }

        // Pick best path (highest volume)
        AutomatonPath bestPath = acceptedPaths.stream()
                .max(Comparator.comparingDouble(AutomatonPath::getTotalVolume))
                .orElseThrow();

        double totalVolume = egoGraph.getTotalVolume(centerEntityId);
        double volumeShare = totalVolume > 0 ? (bestPath.getTotalVolume() / totalVolume) * 100 : 0;

        return PatternAutomaton.MatchResult.builder()
                .matched(true)
                .patternId(pattern.getPatternId())
                .patternName(pattern.getName())
                .weight(pattern.getWeight())
                .severity(pattern.getSeverity())
                .volumeShare(volumeShare)
                .matchedPath(bestPath.getTransactions())
                .explanation(buildExplanation(pattern, bestPath, volumeShare))
                .build();
    }

    /**
     * Explore graph with automaton using DFS.
     */
    private void exploreWithAutomaton(
            EgoGraph egoGraph,
            PatternAutomaton pattern,
            GraphEntity currentEntity,
            PatternAutomaton.State currentState,
            List<GraphTransaction> currentPath,
            Set<String> visitedEntities,
            List<AutomatonPath> acceptedPaths) {

        // Check if we've reached an accept state
        if (currentState.isAccept()) {
            acceptedPaths.add(new AutomatonPath(
                    new ArrayList<>(currentPath),
                    currentPath.stream()
                            .mapToDouble(tx -> tx.getAmount().doubleValue())
                            .sum()
            ));
            return;
        }

        // Check if we've reached a fail state
        if (currentState.isFail()) {
            return;
        }

        // Mark current entity as visited
        visitedEntities.add(currentEntity.getId());

        // Try all transitions from current state
        for (PatternAutomaton.Transition transition : currentState.getTransitions()) {

            // For each outgoing transaction from current entity
            for (GraphTransaction tx : egoGraph.getOutgoingTransactions(currentEntity.getId())) {

                GraphEntity nextEntity = egoGraph.getEntities().get(tx.getToEntityId());
                if (nextEntity == null || visitedEntities.contains(nextEntity.getId())) {
                    continue;
                }

                // Build transition context
                PatternAutomaton.TransitionContext context =
                        PatternAutomaton.TransitionContext.fromPath(
                                currentPath,
                                nextEntity,
                                tx
                        );

                // Check if transition conditions are met
                if (transition.canTransition(context)) {
                    // Find next state
                    PatternAutomaton.State nextState = pattern.getStates().stream()
                            .filter(s -> s.getId().equals(transition.getTargetStateId()))
                            .findFirst()
                            .orElse(null);

                    if (nextState != null) {
                        // Add transaction to path
                        currentPath.add(tx);

                        // Recurse
                        exploreWithAutomaton(
                                egoGraph,
                                pattern,
                                nextEntity,
                                nextState,
                                currentPath,
                                new HashSet<>(visitedEntities),
                                acceptedPaths
                        );

                        // Backtrack
                        currentPath.remove(currentPath.size() - 1);
                    }
                }
            }
        }

        visitedEntities.remove(currentEntity.getId());
    }

    private String buildExplanation(
            PatternAutomaton pattern,
            AutomatonPath path,
            double volumeShare) {

        return String.format("%s detected: %d hops, %.2f%% of volume, total amount: %.2f",
                pattern.getName(),
                path.getTransactions().size(),
                volumeShare,
                path.getTotalVolume());
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class AutomatonPath {
        private List<GraphTransaction> transactions;
        private double totalVolume;
    }
}
