/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package detector_de_plagio_codigo_fuente;

import java.util.*;

public class Graph {
    private String name;
    private List<Node> nodes;
    private List<Edge> edges;
    private Map<Integer, Node> nodeMap;

    public Graph(String name) {
        this.name = name;
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.nodeMap = new HashMap<>();
    }

    public Node addNode(String label, String type) {
        int id = nodes.size() + 1;
        Node node = new Node(id, label, type);
        nodes.add(node);
        nodeMap.put(id, node);
        return node;
    }

    public Edge addEdge(Node source, Node destination, String label) {
        int id = edges.size() + 1;
        Edge edge = new Edge(id, source, destination, label);
        edges.add(edge);
        source.addOutgoingEdge(edge);
        destination.addIncomingEdge(edge);
        return edge;
    }

    // Obtener matriz de adyacencia
    public int[][] getAdjacencyMatrix() {
        int n = nodes.size();
        int[][] matrix = new int[n][n];
        for (Edge edge : edges) {
            int srcIndex = nodes.indexOf(edge.getSource());
            int destIndex = nodes.indexOf(edge.getDestination());
            matrix[srcIndex][destIndex] = 1;
        }
        return matrix;
    }

    // Calcular características del grafo
    public Map<String, Object> getGraphFeatures() {
        Map<String, Object> features = new HashMap<>();
        features.put("nodeCount", nodes.size());
        features.put("edgeCount", edges.size());
        
        // Distribución de tipos de nodos
        Map<String, Integer> typeDistribution = new HashMap<>();
        for (Node node : nodes) {
            typeDistribution.merge(node.getType(), 1, Integer::sum);
        }
        features.put("typeDistribution", typeDistribution);
        
        // Grados de entrada y salida promedio
        double avgInDegree = nodes.stream().mapToInt(n -> n.getIncomingEdges().size()).average().orElse(0);
        double avgOutDegree = nodes.stream().mapToInt(n -> n.getOutgoingEdges().size()).average().orElse(0);
        features.put("avgInDegree", avgInDegree);
        features.put("avgOutDegree", avgOutDegree);
        
        return features;
    }

    // Getters
    public String getName() { return name; }
    public List<Node> getNodes() { return nodes; }
    public List<Edge> getEdges() { return edges; }
    public Node getNodeById(int id) { return nodeMap.get(id); }
}