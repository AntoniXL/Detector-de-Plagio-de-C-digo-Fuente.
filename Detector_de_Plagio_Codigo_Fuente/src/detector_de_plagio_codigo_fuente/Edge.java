package detector_de_plagio_codigo_fuente;

public class Edge {
    private int id;
    private Node source;
    private Node destination;
    private String label; // Etiqueta como "true", "false", etc.

    public Edge(int id, Node source, Node destination, String label) {
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.label = label;
    }

    // Getters
    public int getId() { return id; }
    public Node getSource() { return source; }
    public Node getDestination() { return destination; }
    public String getLabel() { return label; }
}