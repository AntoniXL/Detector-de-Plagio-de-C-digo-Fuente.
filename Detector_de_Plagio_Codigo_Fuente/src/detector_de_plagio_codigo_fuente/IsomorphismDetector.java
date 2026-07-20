/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package detector_de_plagio_codigo_fuente;

import java.util.*;

public class IsomorphismDetector {
    
    private double similarityThreshold = 0.7;
    
    public IsomorphismResult detectIsomorphism(Graph graph1, Graph graph2) {
        // 1. Comparación estructural básica
        double structuralSimilarity = compareStructuralFeatures(graph1, graph2);
        
        // 2. Comparación de subgrafos
        double subgraphSimilarity = compareSubgraphs(graph1, graph2);
        
        // 3. Comparación de secuencias de nodos
        double sequenceSimilarity = compareNodeSequences(graph1, graph2);
        
        // 4. Similitud final ponderada
        double finalSimilarity = structuralSimilarity * 0.4 + 
                                subgraphSimilarity * 0.35 + 
                                sequenceSimilarity * 0.25;
        
        boolean isIsomorphic = finalSimilarity >= similarityThreshold;
        
        return new IsomorphismResult(
            isIsomorphic,
            finalSimilarity,
            structuralSimilarity,
            subgraphSimilarity,
            sequenceSimilarity
        );
    }
    
    private double compareStructuralFeatures(Graph g1, Graph g2) {
        Map<String, Object> features1 = g1.getGraphFeatures();
        Map<String, Object> features2 = g2.getGraphFeatures();
        
        double similarity = 0.0;
        int featureCount = 0;
        
        // Comparar número de nodos
        int nodes1 = (int) features1.get("nodeCount");
        int nodes2 = (int) features2.get("nodeCount");
        similarity += 1.0 - Math.abs(nodes1 - nodes2) / (double) Math.max(nodes1, nodes2);
        featureCount++;
        
        // Comparar número de aristas
        int edges1 = (int) features1.get("edgeCount");
        int edges2 = (int) features2.get("edgeCount");
        similarity += 1.0 - Math.abs(edges1 - edges2) / (double) Math.max(edges1, edges2);
        featureCount++;
        
        // Comparar grados promedio
        double avgIn1 = (double) features1.get("avgInDegree");
        double avgIn2 = (double) features2.get("avgInDegree");
        if (Math.max(avgIn1, avgIn2) > 0) {
            similarity += 1.0 - Math.abs(avgIn1 - avgIn2) / Math.max(avgIn1, avgIn2);
            featureCount++;
        }
        
        return featureCount > 0 ? similarity / featureCount : 0.0;
    }
    
    private double compareSubgraphs(Graph g1, Graph g2) {
        // Identificar patrones de subgrafos comunes (por ejemplo, if-then-else)
        int commonPatterns = 0;
        int totalPatterns = 0;
        
        Map<String, Integer> patterns1 = extractPatterns(g1);
        Map<String, Integer> patterns2 = extractPatterns(g2);
        
        Set<String> allPatterns = new HashSet<>(patterns1.keySet());
        allPatterns.addAll(patterns2.keySet());
        
        for (String pattern : allPatterns) {
            int count1 = patterns1.getOrDefault(pattern, 0);
            int count2 = patterns2.getOrDefault(pattern, 0);
            commonPatterns += Math.min(count1, count2);
            totalPatterns += Math.max(count1, count2);
        }
        
        return totalPatterns > 0 ? (double) commonPatterns / totalPatterns : 0.0;
    }
    
    private Map<String, Integer> extractPatterns(Graph g) {
        Map<String, Integer> patterns = new HashMap<>();
        
        for (Node node : g.getNodes()) {
            String pattern = node.getType();
            if (node.getOutgoingEdges().size() == 2) {
                pattern += "_branch";
            } else if (node.getOutgoingEdges().size() > 1) {
                pattern += "_multi";
            }
            patterns.merge(pattern, 1, Integer::sum);
        }
        
        return patterns;
    }
    
    private double compareNodeSequences(Graph g1, Graph g2) {
        List<String> seq1 = extractNodeSequence(g1);
        List<String> seq2 = extractNodeSequence(g2);
        
        // Algoritmo LCS (Longest Common Subsequence)
        int lcsLength = longestCommonSubsequence(seq1, seq2);
        int maxLength = Math.max(seq1.size(), seq2.size());
        
        return maxLength > 0 ? (double) lcsLength / maxLength : 0.0;
    }
    
    private List<String> extractNodeSequence(Graph g) {
        List<String> sequence = new ArrayList<>();
        Set<Node> visited = new HashSet<>();
        
        for (Node node : g.getNodes()) {
            if (!visited.contains(node)) {
                extractSequenceDFS(node, visited, sequence);
            }
        }
        
        return sequence;
    }
    
    private void extractSequenceDFS(Node node, Set<Node> visited, List<String> sequence) {
        if (visited.contains(node)) return;
        visited.add(node);
        sequence.add(node.getType());
        
        for (var edge : node.getOutgoingEdges()) {
            extractSequenceDFS(edge.getDestination(), visited, sequence);
        }
    }
    
    private int longestCommonSubsequence(List<String> seq1, List<String> seq2) {
        int m = seq1.size();
        int n = seq2.size();
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (seq1.get(i - 1).equals(seq2.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }
    
    public void setSimilarityThreshold(double threshold) {
        this.similarityThreshold = threshold;
    }
}

class IsomorphismResult {
    private boolean isomorphic;
    private double finalSimilarity;
    private double structuralSimilarity;
    private double subgraphSimilarity;
    private double sequenceSimilarity;
    
    public IsomorphismResult(boolean isomorphic, double finalSimilarity, 
                           double structuralSimilarity, double subgraphSimilarity,
                           double sequenceSimilarity) {
        this.isomorphic = isomorphic;
        this.finalSimilarity = finalSimilarity;
        this.structuralSimilarity = structuralSimilarity;
        this.subgraphSimilarity = subgraphSimilarity;
        this.sequenceSimilarity = sequenceSimilarity;
    }
    
    // Getters
    public boolean isIsomorphic() { return isomorphic; }
    public double getFinalSimilarity() { return finalSimilarity; }
    public double getStructuralSimilarity() { return structuralSimilarity; }
    public double getSubgraphSimilarity() { return subgraphSimilarity; }
    public double getSequenceSimilarity() { return sequenceSimilarity; }
}