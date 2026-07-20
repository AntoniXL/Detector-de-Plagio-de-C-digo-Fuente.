/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package detector_de_plagio_codigo_fuente;

import java.util.*;

public class CodeParser {
    
    public Graph parseCode(String code, String graphName) {
        Graph graph = new Graph(graphName);
        
        // Dividir en líneas y filtrar vacías/comentarios
        String[] lines = code.split("\\n");
        List<String> validLines = new ArrayList<>();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                validLines.add(trimmed);
            }
        }
        
        if (validLines.isEmpty()) {
            // Si no hay código válido, crear grafo mínimo
            Node entry = graph.addNode("INICIO", "ENTRY");
            Node exit = graph.addNode("FIN", "EXIT");
            graph.addEdge(entry, exit, "seq");
            return graph;
        }
        
        // Crear nodos rápidamente
        Node entryNode = graph.addNode("INICIO", "ENTRY");
        Node currentNode = entryNode;
        
        int lineCount = validLines.size();
        for (int i = 0; i < lineCount; i++) {
            String line = validLines.get(i);
            
            // Detectar tipo de línea rápidamente
            String type = detectLineType(line);
            String label = createLabel(line, i);
            
            Node newNode = graph.addNode(label, type);
            graph.addEdge(currentNode, newNode, "seq");
            currentNode = newNode;
        }
        
        // Nodo final
        Node exitNode = graph.addNode("FIN", "EXIT");
        graph.addEdge(currentNode, exitNode, "seq");
        
        return graph;
    }
    
    private String detectLineType(String line) {
        // Detección rápida con contains
        if (line.contains("if") && (line.contains("(") || line.contains(")"))) {
            return "IF";
        }
        if (line.contains("else")) {
            return "ELSE";
        }
        if (line.contains("while") && (line.contains("(") || line.contains(")"))) {
            return "WHILE";
        }
        if (line.contains("for") && (line.contains("(") || line.contains(")"))) {
            return "FOR";
        }
        if (line.contains("switch")) {
            return "SWITCH";
        }
        if (line.contains("case")) {
            return "CASE";
        }
        if (line.contains("return")) {
            return "RETURN";
        }
        if (line.contains("break") || line.contains("continue")) {
            return "BREAK";
        }
        if (line.contains("{")) {
            return "BLOCK_START";
        }
        if (line.contains("}")) {
            return "BLOCK_END";
        }
        if (line.contains("=") || line.contains("+") || line.contains("-") || 
            line.contains("*") || line.contains("/")) {
            return "ASSIGNMENT";
        }
        if (line.contains(".") && line.contains("(")) {
            return "METHOD_CALL";
        }
        
        return "STATEMENT";
    }
    
    private String createLabel(String line, int index) {
        // Crear etiqueta corta y significativa
        String cleaned = line.replaceAll("\\s+", " ").trim();
        
        if (cleaned.length() <= 25) {
            return cleaned;
        }
        
        // Truncar inteligentemente
        if (cleaned.contains("(")) {
            int parenIndex = cleaned.indexOf("(");
            String prefix = cleaned.substring(0, Math.min(parenIndex, 20));
            return prefix + "(...)";
        }
        
        return cleaned.substring(0, 22) + "...";
    }
}