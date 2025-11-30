package com.aml.analyzer.domain.model.pattern;

import com.aml.analyzer.domain.model.graph.GraphEntity;
import com.aml.analyzer.domain.model.graph.GraphTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.Predicate;

/**
 * Pattern automaton - finite state machine for detecting AML patterns.
 *
 * Example: "Mixer → Bridge → High-Risk CEX within 6 hours"
 *
 * States: S0 (start) → S1 (mixer) → S2 (bridge) → S3 (high-risk CEX) → ACCEPT
 *
 * This allows encoding complex regulatory narratives:
 * - Placement (initial entry into crypto)
 * - Layering (obfuscation through mixers/chains)
 * - Integration (back to fiat via CEX)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatternAutomaton {

    private String patternId;
    private String name;
    private String description;

    private State initialState;
    private List<State> states;

    private int weight;  // How bad is this pattern? 1-100
    private PatternSeverity severity;

    public enum PatternSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * State in the automaton.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class State {
        private String id;
        private StateType type;
        private List<Transition> transitions;

        public enum StateType {
            START,      // Initial state
            NORMAL,     // Intermediate state
            ACCEPT,     // Pattern matched
            FAIL        // Pattern failed
        }

        public boolean isAccept() {
            return type == StateType.ACCEPT;
        }

        public boolean isFail() {
            return type == StateType.FAIL;
        }
    }

    /**
     * Transition between states.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Transition {
        private String targetStateId;
        private List<TransitionCondition> conditions;  // All must be true (AND)

        public boolean canTransition(TransitionContext context) {
            return conditions.stream().allMatch(c -> c.test(context));
        }
    }

    /**
     * Condition for transition.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitionCondition {
        private ConditionType type;
        private String parameter;
        private Object value;

        public enum ConditionType {
            ENTITY_CATEGORY,      // Entity must be category X
            ENTITY_TAG,           // Entity must have tag X
            TIME_WINDOW,          // Time since path start < X hours
            HOP_COUNT,            // Number of hops < X
            VOLUME_THRESHOLD,     // Volume > X
            BRIDGE_CROSSING,      // Is cross-bridge transaction
            MIXER_HOP             // Is mixer transaction
        }

        public boolean test(TransitionContext context) {
            return switch (type) {
                case ENTITY_CATEGORY -> {
                    GraphEntity.EntityCategory required = GraphEntity.EntityCategory.valueOf(parameter);
                    yield context.getCurrentEntity().getCategory() == required;
                }
                case ENTITY_TAG -> context.getCurrentEntity().hasTag(parameter);
                case TIME_WINDOW -> {
                    long maxHours = Long.parseLong(parameter);
                    yield context.getHoursSinceStart() <= maxHours;
                }
                case HOP_COUNT -> {
                    int maxHops = Integer.parseInt(parameter);
                    yield context.getHopCount() <= maxHops;
                }
                case VOLUME_THRESHOLD -> {
                    double minVolume = Double.parseDouble(parameter);
                    yield context.getTotalVolume() >= minVolume;
                }
                case BRIDGE_CROSSING -> context.getCurrentTransaction() != null &&
                        context.getCurrentTransaction().isCrossBridge();
                case MIXER_HOP -> context.getCurrentTransaction() != null &&
                        context.getCurrentTransaction().isMixerHop();
            };
        }
    }

    /**
     * Context for transition evaluation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitionContext {
        private GraphEntity currentEntity;
        private GraphTransaction currentTransaction;
        private List<GraphTransaction> path;
        private long hoursSinceStart;
        private int hopCount;
        private double totalVolume;

        public static TransitionContext fromPath(List<GraphTransaction> path,
                                                 GraphEntity currentEntity,
                                                 GraphTransaction currentTx) {
            long hours = 0;
            double volume = 0;

            if (!path.isEmpty()) {
                hours = path.get(path.size() - 1).getHoursSince(path.get(0).getTimestamp());
                volume = path.stream()
                        .mapToDouble(tx -> tx.getAmount().doubleValue())
                        .sum();
            }

            return TransitionContext.builder()
                    .currentEntity(currentEntity)
                    .currentTransaction(currentTx)
                    .path(new ArrayList<>(path))
                    .hoursSinceStart(hours)
                    .hopCount(path.size())
                    .totalVolume(volume)
                    .build();
        }
    }

    /**
     * Match result - contains details about a pattern match.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchResult {
        private boolean matched;
        private String patternId;
        private String patternName;
        private int weight;
        private PatternSeverity severity;
        private double volumeShare;  // % of total volume in pattern
        private List<GraphTransaction> matchedPath;
        private String explanation;

        public static MatchResult noMatch(String patternId) {
            return MatchResult.builder()
                    .matched(false)
                    .patternId(patternId)
                    .build();
        }
    }
}
