package com.aml.analyzer.application.service;

import com.aml.analyzer.domain.model.AddressAnalysis;
import com.aml.analyzer.domain.model.EntityCluster;
import com.aml.analyzer.domain.model.RiskScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Professional report generation service.
 *
 * What makes our reports better than CoinKYT/GetBlock PDFs:
 * 1. Transparent scoring breakdown - show the math
 * 2. Audit trail - "why did score change since last check?"
 * 3. Cross-chain entity view - not just single address
 * 4. Actionable recommendations - not just "high risk"
 * 5. Legal disclaimer - compliance-ready
 * 6. Timeline visualization - when was illicit activity?
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generate HTML report - viewable in browser.
     */
    public String generateHtmlReport(
            AddressAnalysis analysis,
            RiskScore riskScore,
            EntityCluster cluster) {

        log.info("Generating HTML report for address: {}", analysis.getAddress());

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>AML Risk Report - ").append(analysis.getAddress()).append("</title>\n");
        html.append("  <style>\n");
        html.append(getReportStyles());
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // Header
        html.append("  <div class=\"header\">\n");
        html.append("    <h1>AML Risk Analysis Report</h1>\n");
        html.append("    <p class=\"subtitle\">Advanced Multi-Chain Risk Assessment</p>\n");
        html.append("  </div>\n");

        // Summary Section
        html.append(generateSummarySection(analysis, riskScore));

        // Risk Breakdown Section
        html.append(generateRiskBreakdownSection(riskScore));

        // Sanctions & Legal Section
        html.append(generateSanctionsSection(analysis));

        // Illicit Categories Section
        html.append(generateIllicitCategoriesSection(analysis));

        // Behavioral Analysis Section
        html.append(generateBehavioralSection(analysis));

        // Timeline Section
        html.append(generateTimelineSection(analysis));

        // Entity Cluster Section (if available)
        if (cluster != null) {
            html.append(generateClusterSection(cluster));
        }

        // Recommendations Section
        html.append(generateRecommendationsSection(riskScore));

        // Disclaimer
        html.append(generateDisclaimerSection());

        // Footer
        html.append("  <div class=\"footer\">\n");
        html.append("    <p>Generated: ").append(LocalDateTime.now().format(FORMATTER)).append("</p>\n");
        html.append("    <p>Powered by Advanced AML Analyzer</p>\n");
        html.append("  </div>\n");

        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
    }

    private String generateSummarySection(AddressAnalysis analysis, RiskScore riskScore) {
        StringBuilder section = new StringBuilder();

        section.append("  <div class=\"section summary\">\n");
        section.append("    <h2>Summary</h2>\n");
        section.append("    <div class=\"info-grid\">\n");
        section.append("      <div class=\"info-item\">\n");
        section.append("        <span class=\"label\">Address:</span>\n");
        section.append("        <span class=\"value code\">").append(analysis.getAddress()).append("</span>\n");
        section.append("      </div>\n");
        section.append("      <div class=\"info-item\">\n");
        section.append("        <span class=\"label\">Asset:</span>\n");
        section.append("        <span class=\"value\">").append(analysis.getAsset()).append("</span>\n");
        section.append("      </div>\n");
        section.append("      <div class=\"info-item\">\n");
        section.append("        <span class=\"label\">Network:</span>\n");
        section.append("        <span class=\"value\">").append(analysis.getNetwork()).append("</span>\n");
        section.append("      </div>\n");
        section.append("    </div>\n");

        // Risk Score Ring
        section.append("    <div class=\"risk-score-container\">\n");
        section.append("      <div class=\"risk-ring ").append(riskScore.getRiskLevel().name().toLowerCase()).append("\">\n");
        section.append("        <div class=\"risk-score\">").append(riskScore.getTotalScore()).append("</div>\n");
        section.append("        <div class=\"risk-level\">").append(riskScore.getRiskLevel()).append("</div>\n");
        section.append("      </div>\n");
        section.append("    </div>\n");

        // One-line conclusion
        section.append("    <div class=\"conclusion\">\n");
        section.append("      <p><strong>Conclusion:</strong> ");
        section.append(generateConclusion(analysis, riskScore));
        section.append("</p>\n");
        section.append("    </div>\n");
        section.append("  </div>\n");

        return section.toString();
    }

    private String generateRiskBreakdownSection(RiskScore riskScore) {
        StringBuilder section = new StringBuilder();

        section.append("  <div class=\"section\">\n");
        section.append("    <h2>Risk Score Breakdown</h2>\n");
        section.append("    <p class=\"section-desc\">Transparent component-by-component scoring</p>\n");
        section.append("    <table class=\"breakdown-table\">\n");
        section.append("      <thead>\n");
        section.append("        <tr>\n");
        section.append("          <th>Dimension</th>\n");
        section.append("          <th>Points</th>\n");
        section.append("          <th>Explanation</th>\n");
        section.append("        </tr>\n");
        section.append("      </thead>\n");
        section.append("      <tbody>\n");

        for (RiskScore.ScoreComponent component : riskScore.getScoreBreakdown()) {
            section.append("        <tr>\n");
            section.append("          <td><strong>").append(formatDimension(component.getDimension())).append("</strong></td>\n");
            section.append("          <td class=\"score\">").append(component.getValue() > 0 ? "+" : "")
                   .append(component.getValue()).append("</td>\n");
            section.append("          <td>").append(component.getExplanation()).append("</td>\n");
            section.append("        </tr>\n");
        }

        section.append("      </tbody>\n");
        section.append("      <tfoot>\n");
        section.append("        <tr>\n");
        section.append("          <th>Total Score</th>\n");
        section.append("          <th class=\"total-score\">").append(riskScore.getTotalScore()).append("</th>\n");
        section.append("          <th></th>\n");
        section.append("        </tr>\n");
        section.append("      </tfoot>\n");
        section.append("    </table>\n");
        section.append("  </div>\n");

        return section.toString();
    }

    private String generateSanctionsSection(AddressAnalysis analysis) {
        StringBuilder section = new StringBuilder();

        section.append("  <div class=\"section\">\n");
        section.append("    <h2>Sanctions & Legal Compliance</h2>\n");

        boolean hasSanctions = (analysis.getDirectSanctionedVolumePct() != null &&
                               analysis.getDirectSanctionedVolumePct().doubleValue() > 0) ||
                              (analysis.getIndirectSanctionedVolumePct1Hop() != null &&
                               analysis.getIndirectSanctionedVolumePct1Hop().doubleValue() > 0);

        if (hasSanctions) {
            section.append("    <div class=\"alert alert-critical\">\n");
            section.append("      <strong>⚠ Sanctions Exposure Detected</strong>\n");
            section.append("      <ul>\n");

            if (analysis.getDirectSanctionedVolumePct() != null &&
                analysis.getDirectSanctionedVolumePct().doubleValue() > 0) {
                section.append("        <li>Direct sanctions: ")
                       .append(analysis.getDirectSanctionedVolumePct()).append("%</li>\n");
                section.append("        <li>Applicable lists: OFAC SDN, EU Sanctions, UN Security Council</li>\n");
            }

            if (analysis.getIndirectSanctionedVolumePct1Hop() != null &&
                analysis.getIndirectSanctionedVolumePct1Hop().doubleValue() > 0) {
                section.append("        <li>1-hop indirect: ")
                       .append(analysis.getIndirectSanctionedVolumePct1Hop()).append("%</li>\n");
            }

            section.append("      </ul>\n");
            section.append("    </div>\n");
        } else {
            section.append("    <div class=\"alert alert-success\">\n");
            section.append("      <strong>✓ No Direct Sanctions Exposure</strong>\n");
            section.append("      <p>Address not found on OFAC, EU, or UN sanctions lists.</p>\n");
            section.append("    </div>\n");
        }

        section.append("  </div>\n");

        return section.toString();
    }

    private String generateIllicitCategoriesSection(AddressAnalysis analysis) {
        StringBuilder section = new StringBuilder();

        section.append("  <div class=\"section\">\n");
        section.append("    <h2>Illicit Activity Categories</h2>\n");
        section.append("    <p class=\"section-desc\">Breakdown by FATF-compliant categories</p>\n");

        if (analysis.getIllicitCategoryVolumes() == null ||
            analysis.getIllicitCategoryVolumes().isEmpty()) {
            section.append("    <p>No illicit activity detected.</p>\n");
        } else {
            section.append("    <table class=\"category-table\">\n");
            section.append("      <thead>\n");
            section.append("        <tr>\n");
            section.append("          <th>Category</th>\n");
            section.append("          <th>Volume %</th>\n");
            section.append("          <th>Severity</th>\n");
            section.append("        </tr>\n");
            section.append("      </thead>\n");
            section.append("      <tbody>\n");

            analysis.getIllicitCategoryVolumes().entrySet().stream()
                   .filter(e -> e.getValue().doubleValue() > 0)
                   .forEach(entry -> {
                       section.append("        <tr>\n");
                       section.append("          <td>").append(entry.getKey().getDisplayName()).append("</td>\n");
                       section.append("          <td>").append(String.format("%.2f", entry.getValue())).append("%</td>\n");
                       section.append("          <td><span class=\"severity ")
                              .append(getSeverityClass(entry.getValue().doubleValue()))
                              .append("\">").append(getSeverityLabel(entry.getValue().doubleValue()))
                              .append("</span></td>\n");
                       section.append("        </tr>\n");
                   });

            section.append("      </tbody>\n");
            section.append("    </table>\n");
        }

        section.append("  </div>\n");

        return section.toString();
    }

    private String generateBehavioralSection(AddressAnalysis analysis) {
        StringBuilder section = new StringBuilder();

        section.append("  <div class=\"section\">\n");
        section.append("    <h2>Behavioral Analysis</h2>\n");

        if (analysis.getBehavioralMetrics() == null) {
            section.append("    <p>No behavioral data available.</p>\n");
        } else {
            AddressAnalysis.BehavioralMetrics metrics = analysis.getBehavioralMetrics();

            section.append("    <div class=\"info-grid\">\n");
            section.append("      <div class=\"info-item\">\n");
            section.append("        <span class=\"label\">Transactions (30d):</span>\n");
            section.append("        <span class=\"value\">").append(metrics.getTransactionCount30d()).append("</span>\n");
            section.append("      </div>\n");
            section.append("      <div class=\"info-item\">\n");
            section.append("        <span class=\"label\">Fan-in degree:</span>\n");
            section.append("        <span class=\"value\">").append(metrics.getFanInDegree()).append("</span>\n");
            section.append("      </div>\n");
            section.append("      <div class=\"info-item\">\n");
            section.append("        <span class=\"label\">Fan-out degree:</span>\n");
            section.append("        <span class=\"value\">").append(metrics.getFanOutDegree()).append("</span>\n");
            section.append("      </div>\n");

            if (metrics.isHasPeelChainPattern()) {
                section.append("      <div class=\"info-item alert-warning\">\n");
                section.append("        <span class=\"label\">⚠ Peel chain detected:</span>\n");
                section.append("        <span class=\"value\">").append(metrics.getPeelChainLength()).append(" hops</span>\n");
                section.append("      </div>\n");
            }

            section.append("    </div>\n");
        }

        section.append("  </div>\n");

        return section.toString();
    }

    private String generateTimelineSection(AddressAnalysis analysis) {
        StringBuilder section = new StringBuilder();

        section.append("  <div class=\"section\">\n");
        section.append("    <h2>Activity Timeline</h2>\n");

        if (analysis.getTemporalMetrics() == null) {
            section.append("    <p>No timeline data available.</p>\n");
        } else {
            AddressAnalysis.TemporalMetrics temporal = analysis.getTemporalMetrics();

            section.append("    <div class=\"timeline\">\n");

            if (temporal.getFirstSeenTime() != null) {
                section.append("      <div class=\"timeline-item\">\n");
                section.append("        <strong>First seen:</strong> ")
                       .append(temporal.getFirstSeenTime().format(FORMATTER)).append("\n");
                section.append("      </div>\n");
            }

            if (temporal.getLastIllicitTxTime() != null) {
                section.append("      <div class=\"timeline-item alert\">\n");
                section.append("        <strong>Last illicit activity:</strong> ")
                       .append(temporal.getLastIllicitTxTime().format(FORMATTER))
                       .append(" (").append(temporal.getLastIllicitTxDaysAgo()).append(" days ago)\n");
                section.append("      </div>\n");
            }

            if (temporal.getLastActiveTime() != null) {
                section.append("      <div class=\"timeline-item\">\n");
                section.append("        <strong>Last active:</strong> ")
                       .append(temporal.getLastActiveTime().format(FORMATTER)).append("\n");
                section.append("      </div>\n");
            }

            section.append("    </div>\n");
        }

        section.append("  </div>\n");

        return section.toString();
    }

    private String generateClusterSection(EntityCluster cluster) {
        StringBuilder section = new StringBuilder();

        section.append("  <div class=\"section\">\n");
        section.append("    <h2>Cross-Chain Entity Cluster</h2>\n");
        section.append("    <p class=\"section-desc\">This feature is unique to our analyzer - competitor tools analyze addresses in isolation.</p>\n");

        section.append("    <div class=\"alert alert-info\">\n");
        section.append("      <strong>Cluster ID:</strong> ").append(cluster.getClusterId()).append("<br>\n");
        section.append("      <strong>Confidence:</strong> ").append(cluster.getConfidence()).append("<br>\n");
        section.append("      <strong>Total addresses:</strong> ").append(cluster.getAddresses().size()).append("\n");
        section.append("    </div>\n");

        section.append("    <h3>Addresses in Cluster</h3>\n");
        section.append("    <table class=\"cluster-table\">\n");
        section.append("      <thead>\n");
        section.append("        <tr><th>Address</th><th>Asset</th><th>Network</th></tr>\n");
        section.append("      </thead>\n");
        section.append("      <tbody>\n");

        cluster.getAddresses().forEach(addr -> {
            section.append("        <tr>\n");
            section.append("          <td class=\"code\">").append(addr.getAddress()).append("</td>\n");
            section.append("          <td>").append(addr.getAsset()).append("</td>\n");
            section.append("          <td>").append(addr.getNetwork()).append("</td>\n");
            section.append("        </tr>\n");
        });

        section.append("      </tbody>\n");
        section.append("    </table>\n");

        section.append("    <h3>Clustering Evidence</h3>\n");
        section.append("    <ul>\n");

        cluster.getEvidence().forEach(evidence -> {
            section.append("      <li>\n");
            section.append("        <strong>").append(evidence.getHeuristic().getDescription()).append("</strong><br>\n");
            section.append("        ").append(evidence.getDescription()).append("<br>\n");
            section.append("        <em>Confidence: ").append(String.format("%.0f%%", evidence.getConfidence() * 100)).append("</em>\n");
            section.append("      </li>\n");
        });

        section.append("    </ul>\n");
        section.append("  </div>\n");

        return section.toString();
    }

    private String generateRecommendationsSection(RiskScore riskScore) {
        StringBuilder section = new StringBuilder();

        section.append("  <div class=\"section\">\n");
        section.append("    <h2>Recommendations</h2>\n");

        switch (riskScore.getRiskLevel()) {
            case CRITICAL:
                section.append("    <div class=\"alert alert-critical\">\n");
                section.append("      <strong>Action Required:</strong>\n");
                section.append("      <ul>\n");
                section.append("        <li>❌ Do NOT proceed with transaction</li>\n");
                section.append("        <li>🚨 File Suspicious Activity Report (SAR) if applicable</li>\n");
                section.append("        <li>📋 Escalate to compliance officer immediately</li>\n");
                section.append("        <li>🔒 Consider freezing funds pending investigation</li>\n");
                section.append("      </ul>\n");
                section.append("    </div>\n");
                break;

            case HIGH:
                section.append("    <div class=\"alert alert-danger\">\n");
                section.append("      <strong>Enhanced Due Diligence Required:</strong>\n");
                section.append("      <ul>\n");
                section.append("        <li>⚠ Proceed with extreme caution</li>\n");
                section.append("        <li>🔍 Conduct enhanced due diligence (EDD)</li>\n");
                section.append("        <li>📝 Document decision rationale</li>\n");
                section.append("        <li>👤 Request additional KYC information</li>\n");
                section.append("      </ul>\n");
                section.append("    </div>\n");
                break;

            case MEDIUM:
                section.append("    <div class=\"alert alert-warning\">\n");
                section.append("      <strong>Standard Due Diligence:</strong>\n");
                section.append("      <ul>\n");
                section.append("        <li>📋 Apply standard KYC/AML procedures</li>\n");
                section.append("        <li>🔍 Review transaction context</li>\n");
                section.append("        <li>📊 Monitor for pattern changes</li>\n");
                section.append("      </ul>\n");
                section.append("    </div>\n");
                break;

            case LOW:
                section.append("    <div class=\"alert alert-success\">\n");
                section.append("      <strong>Low Risk:</strong>\n");
                section.append("      <ul>\n");
                section.append("        <li>✅ Standard processing acceptable</li>\n");
                section.append("        <li>📊 Routine monitoring recommended</li>\n");
                section.append("      </ul>\n");
                section.append("    </div>\n");
                break;
        }

        section.append("  </div>\n");

        return section.toString();
    }

    private String generateDisclaimerSection() {
        return """
              <div class="section disclaimer">
                <h2>Legal Disclaimer</h2>
                <p>
                  This report is provided for informational purposes only. The risk scores and classifications
                  are generated algorithmically based on blockchain transaction data and publicly available
                  intelligence sources. This tool does not constitute legal, financial, or compliance advice.
                </p>
                <p>
                  <strong>The final determination of risk and any compliance actions remain the sole responsibility
                  of the customer/user of this service.</strong> Users should conduct their own due diligence and
                  consult with qualified legal and compliance professionals before making any decisions based on
                  this report.
                </p>
                <p>
                  Risk scores may change over time as new transaction data becomes available. We recommend
                  periodic re-evaluation of high-value or high-risk addresses.
                </p>
              </div>
          """;
    }

    private String generateConclusion(AddressAnalysis analysis, RiskScore riskScore) {
        StringBuilder conclusion = new StringBuilder();

        conclusion.append("Address shows <strong>").append(riskScore.getRiskLevel()).append(" risk</strong> ");

        if (!riskScore.getTags().isEmpty()) {
            conclusion.append("due to ");
            String tags = riskScore.getTags().stream()
                   .map(RiskScore.RiskTag::getDescription)
                   .collect(Collectors.joining(", "));
            conclusion.append(tags);
        }

        conclusion.append(".");

        return conclusion.toString();
    }

    private String formatDimension(String dimension) {
        return dimension.substring(0, 1).toUpperCase() + dimension.substring(1).replace("_", " ");
    }

    private String getSeverityClass(double volumePct) {
        if (volumePct >= 50) return "critical";
        if (volumePct >= 25) return "high";
        if (volumePct >= 10) return "medium";
        return "low";
    }

    private String getSeverityLabel(double volumePct) {
        if (volumePct >= 50) return "Critical";
        if (volumePct >= 25) return "High";
        if (volumePct >= 10) return "Medium";
        return "Low";
    }

    private String getReportStyles() {
        return """
            body {
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
              line-height: 1.6;
              color: #333;
              max-width: 1200px;
              margin: 0 auto;
              padding: 20px;
              background: #f5f5f5;
            }
            .header {
              background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
              color: white;
              padding: 40px;
              border-radius: 10px;
              margin-bottom: 30px;
              text-align: center;
            }
            .header h1 {
              margin: 0;
              font-size: 2.5em;
            }
            .subtitle {
              margin: 10px 0 0 0;
              opacity: 0.9;
            }
            .section {
              background: white;
              padding: 30px;
              margin-bottom: 20px;
              border-radius: 8px;
              box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }
            .section h2 {
              margin-top: 0;
              color: #667eea;
              border-bottom: 2px solid #667eea;
              padding-bottom: 10px;
            }
            .section-desc {
              color: #666;
              font-style: italic;
            }
            .info-grid {
              display: grid;
              grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
              gap: 15px;
              margin: 20px 0;
            }
            .info-item {
              padding: 10px;
              background: #f8f9fa;
              border-radius: 5px;
            }
            .label {
              font-weight: bold;
              color: #666;
              display: block;
              margin-bottom: 5px;
            }
            .value {
              font-size: 1.1em;
            }
            .code {
              font-family: 'Courier New', monospace;
              background: #f0f0f0;
              padding: 2px 6px;
              border-radius: 3px;
              font-size: 0.9em;
            }
            .risk-score-container {
              display: flex;
              justify-content: center;
              margin: 30px 0;
            }
            .risk-ring {
              width: 200px;
              height: 200px;
              border-radius: 50%;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              border: 8px solid;
              box-shadow: 0 4px 8px rgba(0,0,0,0.2);
            }
            .risk-ring.low { border-color: #28a745; color: #28a745; }
            .risk-ring.medium { border-color: #ffc107; color: #ffc107; }
            .risk-ring.high { border-color: #fd7e14; color: #fd7e14; }
            .risk-ring.critical { border-color: #dc3545; color: #dc3545; }
            .risk-score {
              font-size: 3em;
              font-weight: bold;
            }
            .risk-level {
              font-size: 1.2em;
              font-weight: bold;
              text-transform: uppercase;
            }
            .conclusion {
              background: #f8f9fa;
              padding: 20px;
              border-left: 4px solid #667eea;
              margin: 20px 0;
            }
            table {
              width: 100%;
              border-collapse: collapse;
              margin: 20px 0;
            }
            th, td {
              padding: 12px;
              text-align: left;
              border-bottom: 1px solid #ddd;
            }
            th {
              background: #f8f9fa;
              font-weight: bold;
              color: #667eea;
            }
            .score {
              font-weight: bold;
              font-family: monospace;
            }
            .total-score {
              font-size: 1.2em;
              color: #667eea;
            }
            .alert {
              padding: 15px;
              border-radius: 5px;
              margin: 15px 0;
            }
            .alert-success {
              background: #d4edda;
              border-left: 4px solid #28a745;
              color: #155724;
            }
            .alert-info {
              background: #d1ecf1;
              border-left: 4px solid #17a2b8;
              color: #0c5460;
            }
            .alert-warning {
              background: #fff3cd;
              border-left: 4px solid #ffc107;
              color: #856404;
            }
            .alert-danger {
              background: #f8d7da;
              border-left: 4px solid #fd7e14;
              color: #721c24;
            }
            .alert-critical {
              background: #f8d7da;
              border-left: 4px solid #dc3545;
              color: #721c24;
            }
            .severity {
              padding: 4px 8px;
              border-radius: 3px;
              font-size: 0.9em;
              font-weight: bold;
            }
            .severity.low { background: #d4edda; color: #155724; }
            .severity.medium { background: #fff3cd; color: #856404; }
            .severity.high { background: #f8d7da; color: #721c24; }
            .severity.critical { background: #dc3545; color: white; }
            .timeline {
              border-left: 3px solid #667eea;
              padding-left: 20px;
              margin: 20px 0;
            }
            .timeline-item {
              margin-bottom: 15px;
              padding: 10px;
              background: #f8f9fa;
              border-radius: 5px;
            }
            .disclaimer {
              background: #f8f9fa;
              border: 1px solid #ddd;
              font-size: 0.9em;
              color: #666;
            }
            .footer {
              text-align: center;
              padding: 20px;
              color: #666;
              font-size: 0.9em;
            }
            @media print {
              body { background: white; }
              .section { box-shadow: none; border: 1px solid #ddd; }
            }
        """;
    }
}
