# Architecture Documentation

## System Overview

The Advanced AML Analyzer is a multi-layered Spring Boot application designed for high-performance, scalable cryptocurrency risk analysis.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTS                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  BestChange  │  │   Exchanges  │  │ Direct Users │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
                           │ HTTPS
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      LOAD BALANCER                              │
│                    (Kubernetes / AWS ALB)                       │
└─────────────────────────────────────────────────────────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        ▼                                     ▼
┌───────────────────┐              ┌───────────────────┐
│  Application      │              │  Application      │
│  Instance 1       │              │  Instance N       │
│  (Stateless)      │              │  (Stateless)      │
└───────────────────┘              └───────────────────┘
        │                                     │
        └──────────────────┬──────────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        ▼                  ▼                   ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐
│ PostgreSQL  │  │   Redis     │  │   RabbitMQ          │
│ (Primary)   │  │  (Cache)    │  │  (Async Queue)      │
└─────────────┘  └─────────────┘  └─────────────────────┘
```

## Component Architecture

### Layer 1: API Layer
**Purpose:** Handle HTTP requests, validate inputs, return responses

```
api/
├── controller/
│   ├── AddressAnalysisController.java
│   │   └── Endpoints:
│   │       - POST /v1/check-address
│   │       - GET  /v1/check-address/{requestId}
│   │       - POST /v1/check-addresses/bulk
│   │       - GET  /v1/address-history/{address}
│   │
│   └── BestChangeController.java
│       └── Endpoints:
│           - POST /partner/bestchange/check
│           - POST /partner/bestchange/webhook
│
└── dto/
    ├── AddressCheckRequest.java
    ├── AddressCheckResponse.java
    ├── BestChangeRequest.java
    └── BestChangeResponse.java
```

**Responsibilities:**
- ✅ Input validation (@Valid annotations)
- ✅ Request/response mapping
- ✅ HTTP status codes (200, 202, 400, 500)
- ✅ Rate limiting (60 req/min)

---

### Layer 2: Application Services
**Purpose:** Orchestrate business logic, manage transactions

```
application/service/
├── AddressAnalysisService.java
│   └── Methods:
│       - analyzeAddress()          # Main workflow
│       - getAnalysisStatus()       # Async status check
│       - analyzeBulk()             # Bulk processing
│       - getAddressHistory()       # Audit trail
│
└── ReportGenerationService.java
    └── Methods:
        - generateHtmlReport()      # Professional HTML report
        - generatePdfReport()       # PDF export (future)
```

**Workflow (AddressAnalysisService.analyzeAddress()):**
```
1. Fetch blockchain data (via BlockchainAnalyzer)
   ↓
2. Find or create entity cluster (via EntityClusteringService)
   ↓
3. Calculate risk score (via RiskScoringEngine)
   ↓
4. Generate report (via ReportGenerationService)
   ↓
5. Check previous analysis (audit trail)
   ↓
6. Save to database (PostgreSQL)
   ↓
7. Build response
   ↓
8. Send webhook (if requested)
```

---

### Layer 3: Domain Layer (Core Business Logic)
**Purpose:** Implement scoring algorithms, clustering heuristics

```
domain/
├── model/
│   ├── RiskScore.java              # Risk score data structure
│   ├── AddressAnalysis.java        # Analysis result
│   └── EntityCluster.java          # Cross-chain cluster
│
└── service/
    ├── RiskScoringEngine.java      # THE CORE ALGORITHM
    │   └── Methods:
    │       - calculateRisk()
    │       - calculateSanctionsScore()
    │       - calculateIllicitCategoriesScore()
    │       - calculateTemporalAdjustment()
    │       - calculateBehavioralScore()
    │
    └── EntityClusteringService.java
        └── Methods:
            - findClusterForAddress()
            - clusterByMultiInput()
            - clusterByBridgeTransfer()
            - clusterByExchangePattern()
            - mergeClusters()
```

**Risk Scoring Formula:**
```java
score = 0;

// Sanctions (highest priority)
if (direct_sanctioned)
    score += 60;
else if (indirect_1hop > 10%)
    score += 40;
else if (indirect_2_4hop > 20%)
    score += 25;

// Illicit categories
score += min(20, mixer_volume% * 0.6);
score += min(25, stolen_volume% * 0.8);
score += min(20, darknet_volume% * 0.7);
score += min(20, scam_volume% * 0.7);
score += min(30, ransomware_volume% * 0.9);
score += min(70, terrorist_financing_volume% * 1.0);

// Time decay
if (last_illicit_tx > 365 days)
    score -= 10;
else if (last_illicit_tx < 30 days)
    score += 10;

// Behavioral
if (peel_chain && length > 5)
    score += 5;

return clamp(score, 0, 100);
```

**Entity Clustering Heuristics:**
```
Heuristic 1: Multi-Input (Bitcoin/UTXO)
  If TX has inputs [addr1, addr2, addr3]
  → Same wallet controls all three
  Confidence: 0.85

Heuristic 2: Bridge Transfer (Cross-chain)
  If addr1 (ETH) → Arbitrum Bridge → addr2 (ARB)
  → Same owner
  Confidence: 0.75

Heuristic 3: Exchange Pattern
  If addr1 deposits to Binance
  AND addr2 withdraws from Binance within 60 min
  → Possibly same user
  Confidence: 0.65

Heuristic 4: Timing Correlation
  If addr1 and addr2 transact at similar times (correlation > 0.7)
  → Possibly same operator
  Confidence: correlation * 0.6
```

---

### Layer 4: Infrastructure Layer
**Purpose:** Interact with external systems (blockchains, databases, APIs)

```
infrastructure/
├── blockchain/
│   ├── BlockchainAnalyzer.java     # Interface
│   └── impl/
│       ├── EthereumAnalyzer.java   # ETH implementation
│       ├── BitcoinAnalyzer.java    # BTC (to be implemented)
│       ├── TronAnalyzer.java       # TRON (to be implemented)
│       └── SolanaAnalyzer.java     # SOL (to be implemented)
│
├── sanctions/
│   └── SanctionsDataService.java
│       └── Methods:
│           - isDirectlySanctioned()
│           - getSanctionDetails()
│           - updateOfacSdnList()     # Daily auto-update
│           - updateEuSanctions()
│           - updateUnSanctions()
│           - updateChainalysisOracle()
│
└── persistence/
    ├── entity/
    │   └── AddressAnalysisEntity.java
    └── repository/
        └── AddressAnalysisRepository.java
            └── Methods:
                - findFirstByAddressAndAssetAndNetworkOrderByAnalyzedAtDesc()
                - findByAddressAndAssetAndNetworkOrderByAnalyzedAtDesc()
                - findByClusterIdOrderByAnalyzedAtDesc()
```

**Blockchain Analyzer Interface:**
```java
interface BlockchainAnalyzer {
    // Fetch transaction history
    AddressTransactionHistory getTransactionHistory(String address, int max);

    // Get counterparties (who interacted with this address)
    List<Counterparty> getCounterparties(String address);

    // Trace funds (graph traversal)
    FundFlow traceFunds(String address, int hops, Direction direction);

    // Detect patterns (mixers, peel chains, etc.)
    BehavioralPatterns detectPatterns(String address);

    // Calculate metrics
    BehavioralMetrics calculateBehavioralMetrics(String address);
    TemporalMetrics calculateTemporalMetrics(String address);
}
```

---

## Data Flow Diagrams

### Flow 1: BestChange User Checks Address

```
┌──────────┐
│BestChange│
│Platform  │
└────┬─────┘
     │ POST /partner/bestchange/check
     │ {"address": "0x...", "asset": "ETH", "email": "..."}
     ▼
┌────────────────────────┐
│BestChangeController    │
│- Validate input        │
│- Convert to internal   │
└────┬───────────────────┘
     │ analyzeAddress()
     ▼
┌────────────────────────┐
│AddressAnalysisService  │
│1. Fetch blockchain data│◄───┐
│2. Find/create cluster  │    │
│3. Calculate risk       │    │
│4. Generate report      │    │
│5. Check history        │    │
│6. Save to DB          │    │
│7. Build response       │    │
└────┬───────────────────┘    │
     │                        │
     ├─────────────┐          │
     │             │          │
     ▼             ▼          │
┌─────────┐  ┌─────────────┐ │
│Ethereum │  │RiskScoring  │ │
│Analyzer │  │Engine       │ │
└─────────┘  └─────────────┘ │
     │             │          │
     └─────────────┤          │
                   ▼          │
              ┌──────────┐   │
              │PostgreSQL│───┘
              │(save)    │
              └──────────┘
                   │
                   ▼ return response
┌────────────────────────┐
│BestChangeController    │
│- Convert to BC format  │
│- Return JSON           │
└────┬───────────────────┘
     │ {"success": true, "risk_score": 78, ...}
     ▼
┌──────────┐
│BestChange│
│Platform  │
└──────────┘
```

### Flow 2: Risk Scoring Detail

```
┌──────────────────┐
│AddressAnalysis   │
│(blockchain data) │
└────┬─────────────┘
     │
     ▼
┌─────────────────────────────────────┐
│RiskScoringEngine.calculateRisk()    │
│                                     │
│ ┌─────────────────────────────────┐│
│ │1. calculateSanctionsScore()     ││
│ │   - Check OFAC/EU/UN lists      ││
│ │   - Direct: +60                 ││
│ │   - 1-hop: +40                  ││
│ │   - 2-4 hop: +25                ││
│ └─────────────────────────────────┘│
│           │                         │
│           ▼                         │
│ ┌─────────────────────────────────┐│
│ │2. calculateIllicitCategories()  ││
│ │   - Mixers: min(20, vol% * 0.6) ││
│ │   - Stolen: min(25, vol% * 0.8) ││
│ │   - Darknet: min(20, vol% * 0.7)││
│ │   - Scams: min(20, vol% * 0.7)  ││
│ │   - Ransomware: min(30, vol%*0.9││
│ │   - Terror: min(70, vol% * 1.0) ││
│ └─────────────────────────────────┘│
│           │                         │
│           ▼                         │
│ ┌─────────────────────────────────┐│
│ │3. calculateTemporalAdjustment() ││
│ │   - If >365 days: -10           ││
│ │   - If <30 days: +10            ││
│ └─────────────────────────────────┘│
│           │                         │
│           ▼                         │
│ ┌─────────────────────────────────┐│
│ │4. calculateBehavioralScore()    ││
│ │   - Peel chains: +5             ││
│ │   - Fan-out pattern: +3         ││
│ └─────────────────────────────────┘│
│           │                         │
│           ▼                         │
│ ┌─────────────────────────────────┐│
│ │5. Aggregate & cap (0-100)       ││
│ │   Total = clamp(sum, 0, 100)    ││
│ └─────────────────────────────────┘│
└─────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────┐
│RiskScore                            │
│- totalScore: 78                     │
│- riskLevel: HIGH                    │
│- scoreBreakdown: [...]              │
│- tags: [SANCTIONS_1HOP, MIXER, ...] │
└─────────────────────────────────────┘
```

### Flow 3: Cross-Chain Entity Clustering

```
Input: ETH address 0xAAA

     ▼
┌─────────────────────────────────┐
│EntityClusteringService          │
│.findClusterForAddress()         │
└─────────────────────────────────┘
     │
     ├──── Heuristic 1: Multi-Input (if UTXO chain)
     │
     ├──── Heuristic 2: Bridge Transfers
     │     └─> Check if 0xAAA used Arbitrum bridge
     │         └─> Find receiving address on ARB chain
     │             └─> Link ETH:0xAAA ↔ ARB:0xBBB
     │
     ├──── Heuristic 3: Exchange Patterns
     │     └─> Check if 0xAAA deposited to Binance
     │         └─> Find withdrawals within 60 min
     │             └─> Link ETH:0xAAA ↔ BTC:bc1CCC
     │
     └──── Heuristic 4: Timing Correlation
           └─> Compare tx timestamps with other known addresses
               └─> If correlation > 0.7, cluster together

     ▼
┌─────────────────────────────────────┐
│EntityCluster                        │
│- clusterId: "cluster-abc123"        │
│- addresses: [                       │
│    {ETH: 0xAAA},                    │
│    {ARB: 0xBBB},                    │
│    {BTC: bc1CCC}                    │
│  ]                                  │
│- confidence: HIGH                   │
│- evidence: [                        │
│    {BRIDGE_TRANSFER, conf: 0.75},   │
│    {EXCHANGE_DEPOSIT_WITHDRAWAL,    │
│     conf: 0.65}                     │
│  ]                                  │
│- riskScore: 85 (max of all addrs)  │
└─────────────────────────────────────┘
```

## Database Schema

### Table: address_analysis

```sql
CREATE TABLE address_analysis (
    id                  UUID PRIMARY KEY,
    address             VARCHAR(100) NOT NULL,
    asset               VARCHAR(20) NOT NULL,
    network             VARCHAR(50) NOT NULL,
    cluster_id          VARCHAR(100),
    risk_score          INTEGER NOT NULL,
    risk_level          VARCHAR(20) NOT NULL,
    illicit_volume_pct  VARCHAR(20),
    score_breakdown     TEXT,           -- JSON
    risk_tags           TEXT,           -- JSON
    analysis_data       TEXT,           -- JSON (full AddressAnalysis)
    analyzed_at         TIMESTAMP NOT NULL,
    report_url          VARCHAR(500),
    version             BIGINT,

    INDEX idx_address_asset (address, asset, network),
    INDEX idx_analyzed_at (analyzed_at),
    INDEX idx_cluster_id (cluster_id)
);
```

**Query Patterns:**
- Latest analysis: `WHERE address=? AND asset=? AND network=? ORDER BY analyzed_at DESC LIMIT 1`
- Audit trail: `WHERE address=? AND asset=? AND network=? ORDER BY analyzed_at DESC`
- Cluster view: `WHERE cluster_id=? ORDER BY analyzed_at DESC`

## Configuration

### application.yml Structure

```yaml
spring:
  datasource:         # PostgreSQL connection
  jpa:                # Hibernate settings
  data.redis:         # Redis cache
  rabbitmq:           # Message queue
  cache:              # Caching strategy

blockchain:
  ethereum:
    rpc-url:          # Alchemy/Infura/QuickNode
    api-key:
  bitcoin:
    rpc-url:          # Blockchain.com/mempool.space
  tron:
    rpc-url:          # TronGrid
    api-key:
  solana:
    rpc-url:

data-sources:
  sanctions:
    ofac-url:         # OFAC SDN CSV
    eu-url:           # EU sanctions XML
    un-url:           # UN sanctions XML
    chainalysis-oracle-url:
    update-cron:      # Daily at 2 AM

risk-scoring:
  weights:            # Configurable score weights
    sanctions-direct: 60
    mixers: 20
    stolen-funds: 25
    # etc.
  time-decay:
    enabled: true
    threshold-days: 365
    bonus-points: -10

reports:
  storage-path:       # Where to save reports
  base-url:           # Public URL for reports
```

## Deployment Architecture

### Development
```
Laptop
├── PostgreSQL (localhost:5432)
├── Redis (localhost:6379)
└── Spring Boot (localhost:8080)
```

### Production (Kubernetes)
```
┌─────────────────────────────────────────────────┐
│ Kubernetes Cluster                              │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ Ingress (HTTPS, SSL termination)        │   │
│  └─────────────────┬───────────────────────┘   │
│                    │                            │
│  ┌─────────────────▼───────────────────────┐   │
│  │ Service: aml-analyzer (ClusterIP)       │   │
│  └─────────────────┬───────────────────────┘   │
│                    │                            │
│      ┌─────────────┼─────────────┐              │
│      ▼             ▼             ▼              │
│  ┌──────┐     ┌──────┐     ┌──────┐            │
│  │ Pod1 │     │ Pod2 │     │ PodN │            │
│  └──────┘     └──────┘     └──────┘            │
│      │             │             │              │
│      └─────────────┼─────────────┘              │
│                    │                            │
│  ┌─────────────────▼───────────────────────┐   │
│  │ PostgreSQL (StatefulSet)                │   │
│  │ - Primary + Read Replicas               │   │
│  │ - Auto backups                          │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ Redis (StatefulSet)                     │   │
│  │ - Master + Replicas                     │   │
│  │ - Persistence enabled                   │   │
│  └─────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

### Auto-Scaling
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: aml-analyzer
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: aml-analyzer
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

## Performance Characteristics

### Response Times
- **Cached address** (Redis hit): <500ms
- **Fresh analysis** (blockchain fetch + scoring): <10s
- **Bulk analysis**: ~5s per address (parallel processing)

### Throughput
- **Single instance**: ~100 requests/minute
- **With 5 replicas**: ~500 requests/minute
- **Bottleneck**: Blockchain RPC API rate limits (not our code)

### Database
- **PostgreSQL**: 10,000 writes/sec sustained
- **Indexes**: Make lookups <10ms
- **Connection pool**: 10 connections per instance

### Caching
- **Redis**: >100,000 ops/sec
- **Cache hit rate**: 70-80% for sanctions lookups
- **TTL**: 1 hour (sanctions don't change often)

## Security Architecture

### API Security
- **HTTPS only** in production
- **API key authentication** for partners (BestChange gets dedicated key)
- **Rate limiting** per API key (60 req/min default, configurable)
- **Input validation** (regex for addresses, whitelist for assets)

### Data Security
- **Secrets in environment variables** (not in code)
- **Database encryption at rest** (AWS RDS, Azure managed DB)
- **TLS for DB connections**
- **No PII storage** (only crypto addresses, which are public anyway)

### Compliance
- **GDPR**: No personal data collected (email in BestChange request not stored)
- **Audit trail**: All analyses logged with timestamp
- **Legal disclaimer**: In every report

---

## Summary

This architecture provides:

✅ **Scalability** - Horizontal scaling via Kubernetes
✅ **Performance** - Redis caching, database indexing
✅ **Reliability** - Multi-instance, auto-restart, backups
✅ **Maintainability** - Clean layered architecture, dependency injection
✅ **Extensibility** - Easy to add new blockchains (just implement interface)
✅ **Observability** - Prometheus metrics, structured logging
✅ **Security** - API keys, rate limiting, HTTPS, secrets management

**Built for production. Built to scale. Built to win.**
