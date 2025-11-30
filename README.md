# Graph-Based AML Risk Analyzer

A research implementation exploring graph theory and finite automata for blockchain transaction analysis. This project investigates whether graph-based pattern matching can improve AML (Anti-Money Laundering) detection compared to traditional feature aggregation methods.

## Research Motivation

Traditional AML systems aggregate features (e.g., "35% volume from mixers") but miss temporal relationships and complex transaction sequences. This project implements:

- **Graph automata** for detecting transaction patterns (e.g., Mixer → Bridge → Exchange within 6 hours)
- **Cross-chain entity clustering** using heuristic algorithms
- **Hybrid risk scoring** combining rule-based systems, pattern matching, and machine learning
- **Explainable risk assessment** with full component breakdowns

## Architecture

### Three-Layer Hybrid System

```
┌─────────────────────────────────────────────────────────┐
│                   Hybrid Scoring Engine                 │
│  - Rule-based (40%): Sanctions, categories, temporal   │
│  - Pattern matching (30%): Graph automata              │
│  - ML prediction (30%): XGBoost/Random Forest          │
└─────────────────────────────────────────────────────────┘
         ↓                    ↓                    ↓
┌──────────────┐   ┌──────────────────┐   ┌──────────────┐
│ Risk Scoring │   │ Pattern Matching │   │ ML Feature   │
│ Engine       │   │ (Automata)       │   │ Extraction   │
└──────────────┘   └──────────────────┘   └──────────────┘
         ↓                    ↓                    ↓
┌─────────────────────────────────────────────────────────┐
│              Ego Graph Construction                     │
│  - BFS traversal (configurable depth & time window)    │
│  - Entity classification (mixer, CEX, bridge, etc.)    │
│  - Graph metrics (PageRank, clustering coefficient)    │
└─────────────────────────────────────────────────────────┘
```

### Graph Pattern Matching

Implements finite state automata to detect temporal transaction sequences:

**Example: Mixer → Bridge → Exchange Pattern**

```
States: S0 → S1 (MIXER) → S2 (BRIDGE) → ACCEPT (CEX)
              ↓                ↓
            FAIL             FAIL (timeout/hop limit)

Transitions:
- S0 → S1: next entity.category = MIXER
- S1 → S2: next entity.category = BRIDGE AND elapsed_time < 4h
- S2 → ACCEPT: next entity.category = CEX_HIGH_RISK AND elapsed_time < 6h
```

This captures temporal relationships that simple feature aggregation misses.

## Key Features

### 1. Graph-Based Pattern Detection

Predefined patterns for regulatory scenarios:
- **Layering**: Mixer → Bridge → Exchange sequences
- **Structuring**: Multiple small transactions (smurfing)
- **Chain Hopping**: Rapid cross-chain transfers
- **Sanctions Proximity**: N-hop distance to sanctioned entities

### 2. Cross-Chain Entity Clustering

Links addresses across blockchains using:
- Multi-input transactions (common ownership)
- Bridge transfer tracking
- Exchange deposit-withdrawal patterns
- Temporal correlation analysis

### 3. Explainable Scoring

Full transparency into risk calculations:

```json
{
  "risk_score": 75,
  "score_breakdown": [
    {
      "dimension": "sanctions",
      "value": 40,
      "explanation": "1-hop exposure: 15.5% volume from OFAC-listed address"
    },
    {
      "dimension": "pattern_mixer_bridge_cex",
      "value": 59,
      "explanation": "Detected: Mixer → Bridge → CEX (3 hops, 35% volume, <6h)"
    },
    {
      "dimension": "ml_prediction",
      "value": 82,
      "explanation": "XGBoost: 82% probability (top features: sanctioned_count, mixer_count)"
    }
  ]
}
```

### 4. ML Feature Extraction

Extracts 50+ features from graph structure:

- **Topology**: in/out degree, PageRank, clustering coefficient
- **Behavioral**: transaction volume, Gini coefficient, fan-in/out ratio
- **Temporal**: velocity, acceleration, time-of-day patterns
- **Cross-chain**: bridge transaction count, multi-chain indicators

## Technical Implementation

### Graph Construction

```java
// Build ego graph centered on target address
EgoGraph graph = egoGraphBuilder.buildEgoGraph(
    address,
    maxDepth = 3,        // 3-hop neighborhood
    timeWindow = 180     // 180 day lookback
);
```

### Pattern Detection

```java
// Define pattern as automaton
PatternAutomaton pattern = PatternAutomaton.builder()
    .states(List.of(start, mixer, bridge, accept, fail))
    .initialState(start)
    .build();

// Match pattern on graph using DFS
List<MatchResult> matches = patternEngine.matchPatterns(egoGraph, List.of(pattern));
```

### ML Integration

```java
// Extract features
FeatureVector features = featureExtractor.extractFeatures(egoGraph);

// Predict risk
PredictionResult prediction = mlModel.predict(features);
```

### Hybrid Scoring

```java
RiskScore score = hybridEngine.calculateHybridRisk(analysis, egoGraph);
// Combines: rules (40%) + patterns (30%) + ML (30%)
```

## Setup

### Prerequisites

- Java 17+
- PostgreSQL 14+
- Redis 6+
- Maven 3.8+

### Quick Start

```bash
# Clone repository
git clone https://github.com/yourusername/aml-analyzer.git
cd aml-analyzer

# Configure database
psql -U postgres
CREATE DATABASE aml_analyzer;
CREATE USER aml_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE aml_analyzer TO aml_user;

# Set environment variables
export DB_PASSWORD=your_password
export ETH_RPC_URL=https://eth-mainnet.g.alchemy.com/v2/YOUR_KEY

# Build and run
mvn clean install
mvn spring-boot:run
```

### API Usage

```bash
# Analyze an address
curl -X POST http://localhost:8080/v1/check-address \
  -H "Content-Type: application/json" \
  -d '{
    "address": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
    "asset": "ETH",
    "network": "ethereum",
    "include_cluster": true
  }'
```

## Research Questions

1. **Can graph patterns outperform feature aggregation?**
   - Hypothesis: Temporal sequences (A → B → C within time T) capture more signal
   - Approach: Compare pattern-based vs feature-based classification accuracy

2. **How effective is cross-chain entity clustering?**
   - Hypothesis: Multi-chain tracking catches evasion techniques
   - Approach: Evaluate heuristics on known multi-chain entities

3. **What is the optimal hybrid weighting?**
   - Current: 40% rules, 30% patterns, 30% ML
   - Approach: Grid search on labeled dataset, optimize F1 score

4. **Can ML models maintain explainability?**
   - Approach: Feature importance (XGBoost), SHAP values
   - Goal: Regulatory compliance requires transparency

## Performance

| Operation | Time |
|-----------|------|
| Ego graph construction (depth 3) | 2-5s |
| Pattern matching (8 patterns) | 100-500ms |
| ML feature extraction | 100ms |
| ML inference (XGBoost) | <10ms |
| **Total (fresh analysis)** | **2.5-5.5s** |
| **Total (cached graph)** | **<1s** |

## Project Structure

```
src/main/java/com/aml/analyzer/
├── domain/
│   ├── model/
│   │   ├── graph/              # Graph data structures
│   │   ├── pattern/            # Pattern automata
│   │   └── RiskScore.java
│   └── service/
│       ├── RiskScoringEngine.java
│       ├── PatternMatchingEngine.java
│       ├── HybridRiskScoringEngine.java
│       ├── EntityClusteringService.java
│       ├── PatternLibrary.java
│       └── EgoGraphBuilder.java
├── ml/
│   ├── FeatureExtractor.java
│   └── RiskPredictionModel.java
├── infrastructure/
│   ├── blockchain/             # Blockchain adapters
│   ├── sanctions/              # Sanctions data integration
│   └── persistence/            # Database repositories
└── api/
    ├── controller/
    └── dto/
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
# Adjust scoring weights
risk-scoring:
  weights:
    sanctions-direct: 60
    mixers: 20
    stolen-funds: 25

# Hybrid scoring coefficients
hybrid:
  rule-weight: 0.4
  pattern-weight: 0.3
  ml-weight: 0.3
```

## Extending the System

### Adding a New Pattern

```java
public PatternAutomaton customPattern() {
    return PatternAutomaton.builder()
        .patternId("CUSTOM_PATTERN")
        .name("My Custom Pattern")
        .initialState(startState)
        .states(List.of(start, intermediate, accept))
        .weight(70)
        .build();
}
```

### Adding a New Blockchain

```java
@Service
public class SolanaAnalyzer implements BlockchainAnalyzer {
    @Override
    public AddressTransactionHistory getTransactionHistory(String address, int max) {
        // Implement Solana-specific logic
    }
}
```

## Data Sources

- **Sanctions lists**: OFAC SDN, EU Sanctions, UN Security Council
- **Blockchain data**: Public RPC endpoints (Ethereum, Bitcoin, TRON)
- **Entity classification**: Mixer databases, exchange addresses, bridge contracts

## Future Research Directions

1. **Graph Neural Networks**: Replace hand-crafted features with learned representations
2. **Temporal Graph Models**: Dynamic graphs evolving over time
3. **Cross-chain atomic patterns**: Patterns spanning multiple chains simultaneously
4. **Privacy-preserving analysis**: Zero-knowledge proofs for AML without revealing transaction details
5. **Adversarial robustness**: Analyze evasion techniques and defenses

## Documentation

- [Architecture Details](ARCHITECTURE.md) - System design and data flows
- [Graph-Based Approach](GRAPH_BASED_ARCHITECTURE.md) - Deep dive into graph algorithms

## License

MIT License

## Academic References

This work builds on research in:
- Graph theory and pattern matching
- Blockchain analytics
- Machine learning for financial crime detection
- Finite automata theory

---

**Note**: This is a research prototype demonstrating novel approaches to blockchain transaction analysis. Production deployment requires comprehensive testing, integration with production data sources, and model training on labeled datasets.
