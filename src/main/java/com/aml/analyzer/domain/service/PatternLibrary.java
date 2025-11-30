package com.aml.analyzer.domain.service;

import com.aml.analyzer.domain.model.graph.GraphEntity;
import com.aml.analyzer.domain.model.pattern.PatternAutomaton;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Pattern library - predefined AML patterns based on regulatory narratives.
 *
 * Patterns encode FATF money laundering stages:
 * 1. PLACEMENT - Initial entry into crypto ecosystem
 * 2. LAYERING - Obfuscation through mixers, bridges, chain-hopping
 * 3. INTEGRATION - Back to fiat via exchanges
 *
 * Each pattern is an automaton that can detect complex sequences.
 */
@Service
public class PatternLibrary {

    /**
     * Get all predefined patterns.
     */
    public List<PatternAutomaton> getAllPatterns() {
        List<PatternAutomaton> patterns = new ArrayList<>();

        patterns.add(mixerBridgeCexPattern());
        patterns.add(rapidMixerChainPattern());
        patterns.add(peelChainPattern());
        patterns.add(structuringPattern());
        patterns.add(chainHoppingPattern());
        patterns.add(sanctionsProximityPattern());
        patterns.add(darknetCashOutPattern());
        patterns.add(ransomwareLaunderingPattern());

        return patterns;
    }

    /**
     * Pattern 1: Mixer → Bridge → High-Risk CEX (within 6h)
     *
     * Classic layering + integration:
     * - Use mixer to obfuscate
     * - Bridge to different chain
     * - Cash out via high-risk exchange
     *
     * States: START → MIXER → BRIDGE → HIGH_RISK_CEX → ACCEPT
     */
    private PatternAutomaton mixerBridgeCexPattern() {
        // States
        PatternAutomaton.State start = PatternAutomaton.State.builder()
                .id("S0")
                .type(PatternAutomaton.State.StateType.START)
                .transitions(List.of(
                        PatternAutomaton.Transition.builder()
                                .targetStateId("S1")
                                .conditions(List.of(
                                        PatternAutomaton.TransitionCondition.builder()
                                                .type(PatternAutomaton.TransitionCondition.ConditionType.ENTITY_CATEGORY)
                                                .parameter(GraphEntity.EntityCategory.MIXER.name())
                                                .build()
                                ))
                                .build()
                ))
                .build();

        PatternAutomaton.State mixer = PatternAutomaton.State.builder()
                .id("S1")
                .type(PatternAutomaton.State.StateType.NORMAL)
                .transitions(List.of(
                        PatternAutomaton.Transition.builder()
                                .targetStateId("S2")
                                .conditions(List.of(
                                        PatternAutomaton.TransitionCondition.builder()
                                                .type(PatternAutomaton.TransitionCondition.ConditionType.ENTITY_CATEGORY)
                                                .parameter(GraphEntity.EntityCategory.BRIDGE.name())
                                                .build(),
                                        PatternAutomaton.TransitionCondition.builder()
                                                .type(PatternAutomaton.TransitionCondition.ConditionType.TIME_WINDOW)
                                                .parameter("4")  // 4 hours
                                                .build()
                                ))
                                .build(),
                        // Fail if too much time or too many hops
                        PatternAutomaton.Transition.builder()
                                .targetStateId("FAIL")
                                .conditions(List.of(
                                        PatternAutomaton.TransitionCondition.builder()
                                                .type(PatternAutomaton.TransitionCondition.ConditionType.TIME_WINDOW)
                                                .parameter("4")
                                                .build()
                                ))
                                .build()
                ))
                .build();

        PatternAutomaton.State bridge = PatternAutomaton.State.builder()
                .id("S2")
                .type(PatternAutomaton.State.StateType.NORMAL)
                .transitions(List.of(
                        PatternAutomaton.Transition.builder()
                                .targetStateId("ACCEPT")
                                .conditions(List.of(
                                        PatternAutomaton.TransitionCondition.builder()
                                                .type(PatternAutomaton.TransitionCondition.ConditionType.ENTITY_CATEGORY)
                                                .parameter(GraphEntity.EntityCategory.CEX_HIGH_RISK.name())
                                                .build(),
                                        PatternAutomaton.TransitionCondition.builder()
                                                .type(PatternAutomaton.TransitionCondition.ConditionType.TIME_WINDOW)
                                                .parameter("6")  // Total 6 hours
                                                .build(),
                                        PatternAutomaton.TransitionCondition.builder()
                                                .type(PatternAutomaton.TransitionCondition.ConditionType.HOP_COUNT)
                                                .parameter("9")  // Max 9 hops
                                                .build()
                                ))
                                .build()
                ))
                .build();

        PatternAutomaton.State accept = PatternAutomaton.State.builder()
                .id("ACCEPT")
                .type(PatternAutomaton.State.StateType.ACCEPT)
                .transitions(List.of())
                .build();

        PatternAutomaton.State fail = PatternAutomaton.State.builder()
                .id("FAIL")
                .type(PatternAutomaton.State.StateType.FAIL)
                .transitions(List.of())
                .build();

        return PatternAutomaton.builder()
                .patternId("MIXER_BRIDGE_CEX")
                .name("Mixer → Bridge → High-Risk CEX")
                .description("Layering via mixer, cross-chain bridge, then cash-out via high-risk exchange")
                .initialState(start)
                .states(List.of(start, mixer, bridge, accept, fail))
                .weight(85)
                .severity(PatternAutomaton.PatternSeverity.HIGH)
                .build();
    }

    /**
     * Pattern 2: Rapid Mixer Chain (multiple mixers in sequence)
     *
     * Heavy obfuscation - multiple mixer hops in short time.
     *
     * States: START → MIXER1 → MIXER2 → MIXER3 → ACCEPT
     */
    private PatternAutomaton rapidMixerChainPattern() {
        PatternAutomaton.State start = PatternAutomaton.State.builder()
                .id("S0")
                .type(PatternAutomaton.State.StateType.START)
                .transitions(List.of(
                        PatternAutomaton.Transition.builder()
                                .targetStateId("S1")
                                .conditions(List.of(
                                        PatternAutomaton.TransitionCondition.builder()
                                                .type(PatternAutomaton.TransitionCondition.ConditionType.MIXER_HOP)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        PatternAutomaton.State mixer1 = PatternAutomaton.State.builder()
                .id("S1")
                .type(PatternAutomaton.State.StateType.NORMAL)
                .transitions(List.of(
                        PatternAutomaton.Transition.builder()
                                .targetStateId("S2")
                                .conditions(List.of(
                                        PatternAutomaton.TransitionCondition.builder()
                                                .type(PatternAutomaton.TransitionCondition.ConditionType.MIXER_HOP)
                                                .build(),
                                        PatternAutomaton.TransitionCondition.builder()
                                                .type(PatternAutomaton.TransitionCondition.ConditionType.TIME_WINDOW)
                                                .parameter("12")
                                                .build()
                                ))
                                .build()
                ))
                .build();

        PatternAutomaton.State mixer2 = PatternAutomaton.State.builder()
                .id("S2")
                .type(PatternAutomaton.State.StateType.NORMAL)
                .transitions(List.of(
                        PatternAutomaton.Transition.builder()
                                .targetStateId("ACCEPT")
                                .conditions(List.of(
                                        PatternAutomaton.TransitionCondition.builder()
                                                .type(PatternAutomaton.TransitionCondition.ConditionType.MIXER_HOP)
                                                .build(),
                                        PatternAutomaton.TransitionCondition.builder()
                                                .type(PatternAutomaton.TransitionCondition.ConditionType.TIME_WINDOW)
                                                .parameter("24")
                                                .build()
                                ))
                                .build()
                ))
                .build();

        PatternAutomaton.State accept = PatternAutomaton.State.builder()
                .id("ACCEPT")
                .type(PatternAutomaton.State.StateType.ACCEPT)
                .transitions(List.of())
                .build();

        return PatternAutomaton.builder()
                .patternId("RAPID_MIXER_CHAIN")
                .name("Rapid Mixer Chain")
                .description("Multiple mixer hops in sequence - heavy obfuscation")
                .initialState(start)
                .states(List.of(start, mixer1, mixer2, accept))
                .weight(75)
                .severity(PatternAutomaton.PatternSeverity.HIGH)
                .build();
    }

    /**
     * Pattern 3: Peel Chain (Bitcoin layering)
     *
     * Classic Bitcoin obfuscation:
     * - Send large amount
     * - Keep peeling off small amounts
     * - Creates long chain
     */
    private PatternAutomaton peelChainPattern() {
        // Simplified - in real implementation, track decreasing amounts
        return PatternAutomaton.builder()
                .patternId("PEEL_CHAIN")
                .name("Peel Chain")
                .description("Bitcoin peel chain - sequential small withdrawals")
                .weight(45)
                .severity(PatternAutomaton.PatternSeverity.MEDIUM)
                .build();
    }

    /**
     * Pattern 4: Structuring (Smurfing)
     *
     * Breaking large amounts into small transactions to avoid detection.
     * Multiple deposits <$10k to same destination.
     */
    private PatternAutomaton structuringPattern() {
        return PatternAutomaton.builder()
                .patternId("STRUCTURING")
                .name("Structuring/Smurfing")
                .description("Multiple small transactions to avoid reporting thresholds")
                .weight(60)
                .severity(PatternAutomaton.PatternSeverity.MEDIUM)
                .build();
    }

    /**
     * Pattern 5: Chain Hopping (BTC → ETH → TRON → SOL)
     *
     * Rapid cross-chain hops to obfuscate trail.
     */
    private PatternAutomaton chainHoppingPattern() {
        return PatternAutomaton.builder()
                .patternId("CHAIN_HOPPING")
                .name("Chain Hopping")
                .description("Rapid cross-chain transfers via bridges")
                .weight(55)
                .severity(PatternAutomaton.PatternSeverity.MEDIUM)
                .build();
    }

    /**
     * Pattern 6: Sanctions Proximity (close to sanctioned entity)
     *
     * Within 2 hops of sanctioned address.
     */
    private PatternAutomaton sanctionsProximityPattern() {
        return PatternAutomaton.builder()
                .patternId("SANCTIONS_PROXIMITY")
                .name("Sanctions Proximity")
                .description("Within 2 hops of OFAC/EU sanctioned entity")
                .weight(90)
                .severity(PatternAutomaton.PatternSeverity.CRITICAL)
                .build();
    }

    /**
     * Pattern 7: Darknet Cash-Out
     *
     * Darknet market → mixer → CEX
     */
    private PatternAutomaton darknetCashOutPattern() {
        return PatternAutomaton.builder()
                .patternId("DARKNET_CASHOUT")
                .name("Darknet Cash-Out")
                .description("Darknet market proceeds laundered via mixer to exchange")
                .weight(80)
                .severity(PatternAutomaton.PatternSeverity.HIGH)
                .build();
    }

    /**
     * Pattern 8: Ransomware Laundering
     *
     * Ransomware payment → mixer chain → multiple CEXs (distribution)
     */
    private PatternAutomaton ransomwareLaunderingPattern() {
        return PatternAutomaton.builder()
                .patternId("RANSOMWARE_LAUNDERING")
                .name("Ransomware Laundering")
                .description("Ransomware proceeds laundered via mixers and distributed to exchanges")
                .weight(95)
                .severity(PatternAutomaton.PatternSeverity.CRITICAL)
                .build();
    }
}
