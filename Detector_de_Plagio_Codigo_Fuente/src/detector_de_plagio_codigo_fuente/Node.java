package detector_de_plagio_codigo_fuente;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private int id;
    private String label;
    private String type; // Tipo: ENTRY, EXIT, IF, WHILE, FOR, ASSIGNMENT, etc.
    private List<Edge> outgoingEdges;
    private List<Edge> incomingEdges;

    public Node(int id, String label, String type) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.outgoingEdges = new ArrayList<>();
        this.incomingEdges = new ArrayList<>();
    }

    public void addOutgoingEdge(Edge edge) {
        outgoingEdges.add(edge);
    }

    public void addIncomingEdge(Edge edge) {
        incomingEdges.add(edge);
    }

    // Getters y Setters
    public int getId() { return id; }
    public String getLabel() { return label; }
    public String getType() { return type; }
    public List<Edge> getOutgoingEdges() { return outgoingEdges; }
    public List<Edge> getIncomingEdges() { return incomingEdges; }
    public void setLabel(String label) { this.label = label; }
}