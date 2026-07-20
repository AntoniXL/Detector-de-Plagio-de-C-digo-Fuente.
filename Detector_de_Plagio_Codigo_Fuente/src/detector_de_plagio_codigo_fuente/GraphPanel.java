/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package detector_de_plagio_codigo_fuente;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class GraphPanel extends JPanel {
    private Graph graph;
    private Map<Integer, Point2D.Double> nodePositions;
    private Map<Integer, Rectangle2D.Double> nodeBounds;
    private double scale = 1.0;
    private double translateX = 0;
    private double translateY = 0;
    private Point lastMousePoint;
    private Node selectedNode = null;
    private Node hoveredNode = null;
    private Set<Node> highlightedNodes = new HashSet<>();
    private boolean isCalculating = false;
    private boolean hasGraph = false;
    
    public enum LayoutStyle {
        JERARQUICO,
        CIRCULAR,
        ESPIRAL,
        HEXAGONAL,
        ORGANICO,
        FLUJO_VERTICAL
    }
    
    private LayoutStyle currentStyle = LayoutStyle.JERARQUICO;
    
    private static final double NODE_WIDTH = 140;
    private static final double NODE_HEIGHT = 45;
    private static final double HORIZONTAL_SPACING = 180;
    private static final double VERTICAL_SPACING = 90;
    private static final double MARGIN = 70;
    
    // Colores
    private static final Color[] PALETTE_ENTRY = {new Color(46, 160, 67), new Color(35, 120, 50)};
    private static final Color[] PALETTE_EXIT = {new Color(220, 70, 55), new Color(185, 50, 40)};
    private static final Color[] PALETTE_CONDITION = {new Color(240, 190, 20), new Color(230, 150, 15)};
    private static final Color[] PALETTE_BRANCH = {new Color(50, 150, 220), new Color(40, 125, 185)};
    private static final Color[] PALETTE_LOOP = {new Color(150, 85, 180), new Color(135, 70, 165)};
    private static final Color[] PALETTE_DEFAULT = {new Color(145, 160, 165), new Color(120, 135, 140)};
    
    private static final Color COLOR_EDGE = new Color(90, 90, 90, 180);
    private static final Color COLOR_BACKGROUND = new Color(250, 250, 252);
    private static final Color COLOR_GRID = new Color(235, 235, 240);
    
    public GraphPanel() {
        this.graph = null;
        this.nodePositions = new HashMap<>();
        this.nodeBounds = new HashMap<>();
        setBackground(COLOR_BACKGROUND);
        setPreferredSize(new Dimension(800, 600));
        
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isCalculating || !hasGraph) return;
                lastMousePoint = e.getPoint();
                handleNodeSelection(e.getPoint());
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isCalculating || !hasGraph) return;
                if (selectedNode != null && nodePositions.containsKey(selectedNode.getId())) {
                    Point2D.Double pos = nodePositions.get(selectedNode.getId());
                    pos.x = (e.getX() - translateX) / scale;
                    pos.y = (e.getY() - translateY) / scale;
                    updateNodeBounds();
                    repaint();
                } else {
                    int dx = e.getX() - lastMousePoint.x;
                    int dy = e.getY() - lastMousePoint.y;
                    translateX += dx;
                    translateY += dy;
                    lastMousePoint = e.getPoint();
                    repaint();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                selectedNode = null;
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                if (isCalculating || !hasGraph) return;
                handleNodeHover(e.getPoint());
            }
            
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (isCalculating || !hasGraph) return;
                double oldScale = scale;
                double scaleFactor = 0.1;
                if (e.getWheelRotation() < 0) {
                    scale *= (1 + scaleFactor);
                } else {
                    scale *= (1 - scaleFactor);
                }
                scale = Math.max(0.1, Math.min(5.0, scale));
                double scaleChange = scale / oldScale;
                translateX = e.getX() - scaleChange * (e.getX() - translateX);
                translateY = e.getY() - scaleChange * (e.getY() - translateY);
                repaint();
            }
        };
        
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        addMouseWheelListener(mouseAdapter);
    }
    
    public void setLayoutStyle(LayoutStyle style) {
        this.currentStyle = style;
        if (graph != null && hasGraph) {
            recalculateLayout();
        }
    }
    
    public LayoutStyle getLayoutStyle() {
        return currentStyle;
    }
    
    public void setGraph(Graph graph) {
        this.graph = graph;
        this.hasGraph = false;
        this.isCalculating = false;
        if (graph != null && !graph.getNodes().isEmpty()) {
            recalculateLayout();
        } else {
            nodePositions.clear();
            nodeBounds.clear();
            hasGraph = false;
            repaint();
        }
    }
    
    private void recalculateLayout() {
        isCalculating = true;
        hasGraph = false;
        repaint();
        
        final List<Node> nodes = new ArrayList<>(graph.getNodes());
        final List<Edge> edges = new ArrayList<>(graph.getEdges());
        final LayoutStyle style = this.currentStyle;
        
        SwingWorker<Map<Integer, Point2D.Double>, Void> worker = new SwingWorker<Map<Integer, Point2D.Double>, Void>() {
            @Override
            protected Map<Integer, Point2D.Double> doInBackground() throws Exception {
                switch (style) {
                    case CIRCULAR: return createCircularLayout(nodes, edges);
                    case ESPIRAL: return createSpiralLayout(nodes, edges);
                    case HEXAGONAL: return createHexagonalLayout(nodes, edges);
                    case ORGANICO: return createOrganicLayout(nodes, edges);
                    case FLUJO_VERTICAL: return createVerticalFlowLayout(nodes, edges);
                    default: return createHierarchicalLayout(nodes, edges);
                }
            }
            
            @Override
            protected void done() {
                try {
                    nodePositions = get();
                    updateNodeBounds();
                    centerView();
                    isCalculating = false;
                    hasGraph = true;
                } catch (Exception e) {
                    createEmergencyLayout(nodes);
                    isCalculating = false;
                    hasGraph = true;
                }
                repaint();
            }
        };
        worker.execute();
    }
    
    // ==================== LAYOUTS MEJORADOS Y ORDENADOS ====================
    
    /**
     * Layout Jerarquico - Arbol ordenado de arriba hacia abajo
     */
    private Map<Integer, Point2D.Double> createHierarchicalLayout(List<Node> nodes, List<Edge> edges) {
        Map<Integer, Point2D.Double> positions = new HashMap<>();
        if (nodes.isEmpty()) return positions;
        
        // Calcular niveles por BFS
        Map<Node, Integer> levels = new HashMap<>();
        Map<Node, Integer> inDegree = new HashMap<>();
        
        for (Node node : nodes) inDegree.put(node, 0);
        for (Edge edge : edges) inDegree.merge(edge.getDestination(), 1, Integer::sum);
        
        Queue<Node> queue = new LinkedList<>();
        for (Node node : nodes) {
            if (inDegree.get(node) == 0) {
                levels.put(node, 0);
                queue.offer(node);
            }
        }
        if (queue.isEmpty() && !nodes.isEmpty()) {
            levels.put(nodes.get(0), 0);
            queue.offer(nodes.get(0));
        }
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            int currentLevel = levels.get(current);
            for (Edge edge : edges) {
                if (edge.getSource() == current) {
                    Node next = edge.getDestination();
                    int newLevel = currentLevel + 1;
                    if (!levels.containsKey(next) || levels.get(next) < newLevel) {
                        levels.put(next, newLevel);
                        if (!queue.contains(next)) queue.offer(next);
                    }
                }
            }
        }
        for (Node node : nodes) levels.putIfAbsent(node, 0);
        
        // Agrupar por nivel y ordenar
        Map<Integer, List<Node>> byLevel = new TreeMap<>();
        for (Node node : nodes) {
            byLevel.computeIfAbsent(levels.get(node), k -> new ArrayList<>()).add(node);
        }
        
        // Ordenar nodos dentro de cada nivel para minimizar cruces
        for (List<Node> levelNodes : byLevel.values()) {
            levelNodes.sort((a, b) -> {
                double avgPosA = getAverageParentPosition(a, positions, edges);
                double avgPosB = getAverageParentPosition(b, positions, edges);
                return Double.compare(avgPosA, avgPosB);
            });
        }
        
        // Posicionar nodos centrados
        int maxNodes = byLevel.values().stream().mapToInt(List::size).max().orElse(1);
        
        for (Map.Entry<Integer, List<Node>> entry : byLevel.entrySet()) {
            int level = entry.getKey();
            List<Node> levelNodes = entry.getValue();
            int n = levelNodes.size();
            
            double levelWidth = (n - 1) * HORIZONTAL_SPACING;
            double maxWidth = (maxNodes - 1) * HORIZONTAL_SPACING;
            double startX = (maxWidth - levelWidth) / 2.0;
            double y = MARGIN + level * VERTICAL_SPACING;
            
            for (int i = 0; i < n; i++) {
                Node node = levelNodes.get(i);
                double x = startX + i * HORIZONTAL_SPACING;
                positions.put(node.getId(), new Point2D.Double(x, y));
            }
        }
        
        return positions;
    }
    
    private double getAverageParentPosition(Node node, Map<Integer, Point2D.Double> positions, List<Edge> edges) {
        double sum = 0;
        int count = 0;
        for (Edge edge : edges) {
            if (edge.getDestination() == node) {
                Point2D.Double parentPos = positions.get(edge.getSource().getId());
                if (parentPos != null) {
                    sum += parentPos.x;
                    count++;
                }
            }
        }
        return count > 0 ? sum / count : 0;
    }
    
    /**
     * Layout Circular - Circulos concentricos ordenados por nivel
     */
    private Map<Integer, Point2D.Double> createCircularLayout(List<Node> nodes, List<Edge> edges) {
        Map<Integer, Point2D.Double> positions = new HashMap<>();
        if (nodes.isEmpty()) return positions;
        
        // Asignar niveles
        Map<Node, Integer> levels = assignLevelsByDistance(nodes, edges);
        Map<Integer, List<Node>> byLevel = new TreeMap<>();
        for (Node node : nodes) {
            byLevel.computeIfAbsent(levels.get(node), k -> new ArrayList<>()).add(node);
        }
        
        int numLevels = byLevel.size();
        double baseRadius = Math.max(250, nodes.size() * 12);
        double radiusStep = Math.max(100, baseRadius / Math.max(1, numLevels));
        
        for (Map.Entry<Integer, List<Node>> entry : byLevel.entrySet()) {
            int level = entry.getKey();
            List<Node> levelNodes = entry.getValue();
            int n = levelNodes.size();
            
            double radius = baseRadius + level * radiusStep;
            
            // Ordenar nodos por conexiones para mejor distribucion
            levelNodes.sort((a, b) -> {
                int connA = a.getIncomingEdges().size() + a.getOutgoingEdges().size();
                int connB = b.getIncomingEdges().size() + b.getOutgoingEdges().size();
                return Integer.compare(connB, connA);
            });
            
            double startAngle = -Math.PI / 2;
            
            for (int i = 0; i < n; i++) {
                Node node = levelNodes.get(i);
                double angle = startAngle + (2 * Math.PI * i) / n;
                double x = radius * Math.cos(angle);
                double y = radius * Math.sin(angle);
                positions.put(node.getId(), new Point2D.Double(x, y));
            }
        }
        
        return positions;
    }
    
    private Map<Node, Integer> assignLevelsByDistance(List<Node> nodes, List<Edge> edges) {
        Map<Node, Integer> levels = new HashMap<>();
        Set<Node> visited = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();
        
        // Encontrar nodos raiz
        for (Node node : nodes) {
            boolean hasParent = false;
            for (Edge edge : edges) {
                if (edge.getDestination() == node) {
                    hasParent = true;
                    break;
                }
            }
            if (!hasParent) {
                levels.put(node, 0);
                queue.offer(node);
                visited.add(node);
            }
        }
        
        if (queue.isEmpty() && !nodes.isEmpty()) {
            levels.put(nodes.get(0), 0);
            queue.offer(nodes.get(0));
            visited.add(nodes.get(0));
        }
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            int currentLevel = levels.get(current);
            
            for (Edge edge : edges) {
                if (edge.getSource() == current) {
                    Node next = edge.getDestination();
                    if (!visited.contains(next)) {
                        levels.put(next, currentLevel + 1);
                        visited.add(next);
                        queue.offer(next);
                    }
                }
            }
        }
        
        for (Node node : nodes) {
            levels.putIfAbsent(node, 0);
        }
        
        return levels;
    }
    
    /**
     * Layout Espiral - Espiral ordenada con espaciado uniforme
     */
    private Map<Integer, Point2D.Double> createSpiralLayout(List<Node> nodes, List<Edge> edges) {
        Map<Integer, Point2D.Double> positions = new HashMap<>();
        if (nodes.isEmpty()) return positions;
        
        // Ordenar nodos por nivel y conexiones
        Map<Node, Integer> levels = assignLevelsByDistance(nodes, edges);
        List<Node> sortedNodes = new ArrayList<>(nodes);
        sortedNodes.sort((a, b) -> {
            int levelCompare = Integer.compare(levels.get(a), levels.get(b));
            if (levelCompare != 0) return levelCompare;
            int connA = a.getIncomingEdges().size() + a.getOutgoingEdges().size();
            int connB = b.getIncomingEdges().size() + b.getOutgoingEdges().size();
            return Integer.compare(connB, connA);
        });
        
        double spiralGrowth = 30;
        double angleStep = 0.5;
        
        for (int i = 0; i < sortedNodes.size(); i++) {
            Node node = sortedNodes.get(i);
            double angle = i * angleStep;
            double radius = 50 + i * spiralGrowth;
            double x = radius * Math.cos(angle);
            double y = radius * Math.sin(angle);
            positions.put(node.getId(), new Point2D.Double(x, y));
        }
        
        return positions;
    }
    
    /**
     * Layout Hexagonal - Patron de panal ordenado por filas y columnas
     */
    private Map<Integer, Point2D.Double> createHexagonalLayout(List<Node> nodes, List<Edge> edges) {
        Map<Integer, Point2D.Double> positions = new HashMap<>();
        if (nodes.isEmpty()) return positions;
        
        // Ordenar nodos por nivel
        Map<Node, Integer> levels = assignLevelsByDistance(nodes, edges);
        List<Node> sortedNodes = new ArrayList<>(nodes);
        sortedNodes.sort((a, b) -> Integer.compare(levels.get(a), levels.get(b)));
        
        int totalNodes = sortedNodes.size();
        int cols = (int) Math.ceil(Math.sqrt(totalNodes * 1.3));
        
        double hexWidth = HORIZONTAL_SPACING;
        double hexHeight = VERTICAL_SPACING * 1.1;
        
        for (int i = 0; i < totalNodes; i++) {
            Node node = sortedNodes.get(i);
            int row = i / cols;
            int col = i % cols;
            
            double x = col * hexWidth;
            double y = row * hexHeight;
            
            // Desplazar filas pares para efecto hexagonal
            if (row % 2 == 1) {
                x += hexWidth / 2;
            }
            
            positions.put(node.getId(), new Point2D.Double(x, y));
        }
        
        return positions;
    }
    
    /**
     * Layout Organico - Disposicion natural con fuerzas ordenadas
     */
    private Map<Integer, Point2D.Double> createOrganicLayout(List<Node> nodes, List<Edge> edges) {
        Map<Integer, Point2D.Double> positions = new HashMap<>();
        if (nodes.isEmpty()) return positions;
        
        // Posiciones iniciales en circulo (mas ordenado que aleatorio)
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            double angle = (2 * Math.PI * i) / nodes.size();
            double radius = 200 + (i % 3) * 50;
            double x = radius * Math.cos(angle);
            double y = radius * Math.sin(angle);
            positions.put(node.getId(), new Point2D.Double(x, y));
        }
        
        // Simulacion de fuerzas (pocas iteraciones para mantener orden)
        for (int iter = 0; iter < 8; iter++) {
            Map<Integer, Point2D.Double> forces = new HashMap<>();
            for (int id : positions.keySet()) {
                forces.put(id, new Point2D.Double(0, 0));
            }
            
            // Repulsion entre nodos cercanos
            List<Integer> ids = new ArrayList<>(positions.keySet());
            for (int i = 0; i < ids.size(); i++) {
                for (int j = i + 1; j < ids.size(); j++) {
                    Point2D.Double p1 = positions.get(ids.get(i));
                    Point2D.Double p2 = positions.get(ids.get(j));
                    double dx = p1.x - p2.x;
                    double dy = p1.y - p2.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    double minDist = HORIZONTAL_SPACING * 0.7;
                    
                    if (dist < minDist && dist > 0) {
                        double force = (minDist - dist) * 2;
                        forces.get(ids.get(i)).x += force * dx / dist;
                        forces.get(ids.get(i)).y += force * dy / dist;
                        forces.get(ids.get(j)).x -= force * dx / dist;
                        forces.get(ids.get(j)).y -= force * dy / dist;
                    }
                }
            }
            
            // Atraccion por aristas
            for (Edge edge : edges) {
                Point2D.Double p1 = positions.get(edge.getSource().getId());
                Point2D.Double p2 = positions.get(edge.getDestination().getId());
                if (p1 != null && p2 != null) {
                    double dx = p2.x - p1.x;
                    double dy = p2.y - p1.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist > HORIZONTAL_SPACING && dist > 0) {
                        double force = (dist - HORIZONTAL_SPACING) * 0.05;
                        forces.get(edge.getSource().getId()).x += force * dx / dist;
                        forces.get(edge.getSource().getId()).y += force * dy / dist;
                        forces.get(edge.getDestination().getId()).x -= force * dx / dist;
                        forces.get(edge.getDestination().getId()).y -= force * dy / dist;
                    }
                }
            }
            
            // Aplicar fuerzas con amortiguacion
            double damping = 0.5 * (1 - (double) iter / 8);
            for (int id : positions.keySet()) {
                Point2D.Double pos = positions.get(id);
                Point2D.Double f = forces.get(id);
                pos.x += f.x * damping;
                pos.y += f.y * damping;
            }
        }
        
        return positions;
    }
    
    /**
     * Layout Flujo Vertical - Columnas ordenadas por cadenas de ejecucion
     */
    private Map<Integer, Point2D.Double> createVerticalFlowLayout(List<Node> nodes, List<Edge> edges) {
        Map<Integer, Point2D.Double> positions = new HashMap<>();
        if (nodes.isEmpty()) return positions;
        
        // Encontrar cadenas de nodos
        List<List<Node>> chains = findOrderedChains(nodes, edges);
        
        // Si no hay cadenas, crear una por nodo
        if (chains.isEmpty()) {
            for (Node node : nodes) {
                List<Node> chain = new ArrayList<>();
                chain.add(node);
                chains.add(chain);
            }
        }
        
        // Ordenar cadenas por tamano
        chains.sort((a, b) -> Integer.compare(b.size(), a.size()));
        
        int numColumns = chains.size();
        double totalWidth = (numColumns - 1) * HORIZONTAL_SPACING;
        double startX = -totalWidth / 2.0;
        
        for (int col = 0; col < chains.size(); col++) {
            List<Node> chain = chains.get(col);
            double x = startX + col * HORIZONTAL_SPACING;
            
            for (int row = 0; row < chain.size(); row++) {
                Node node = chain.get(row);
                double y = MARGIN + row * VERTICAL_SPACING;
                positions.put(node.getId(), new Point2D.Double(x, y));
            }
        }
        
        return positions;
    }
    
    private List<List<Node>> findOrderedChains(List<Node> nodes, List<Edge> edges) {
        List<List<Node>> chains = new ArrayList<>();
        Set<Node> visited = new HashSet<>();
        Map<Node, Node> nextMap = new HashMap<>();
        
        // Construir mapa de siguientes
        for (Edge edge : edges) {
            if (!nextMap.containsKey(edge.getSource())) {
                nextMap.put(edge.getSource(), edge.getDestination());
            }
        }
        
        // Encontrar nodos iniciales (sin predecesores)
        for (Node node : nodes) {
            if (visited.contains(node)) continue;
            
            boolean hasPredecessor = false;
            for (Edge edge : edges) {
                if (edge.getDestination() == node && !visited.contains(edge.getSource())) {
                    hasPredecessor = true;
                    break;
                }
            }
            
            if (!hasPredecessor) {
                List<Node> chain = new ArrayList<>();
                Node current = node;
                while (current != null && !visited.contains(current)) {
                    chain.add(current);
                    visited.add(current);
                    current = nextMap.get(current);
                }
                if (!chain.isEmpty()) {
                    chains.add(chain);
                }
            }
        }
        
        // Nodos restantes
        for (Node node : nodes) {
            if (!visited.contains(node)) {
                List<Node> chain = new ArrayList<>();
                chain.add(node);
                visited.add(node);
                chains.add(chain);
            }
        }
        
        return chains;
    }
    
    private void createEmergencyLayout(List<Node> nodes) {
        nodePositions.clear();
        int cols = (int) Math.ceil(Math.sqrt(nodes.size()));
        
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            int row = i / cols;
            int col = i % cols;
            double x = col * HORIZONTAL_SPACING;
            double y = row * VERTICAL_SPACING;
            nodePositions.put(node.getId(), new Point2D.Double(x, y));
        }
        updateNodeBounds();
        centerView();
    }
    
    private void updateNodeBounds() {
        nodeBounds.clear();
        if (nodePositions == null) return;
        for (Map.Entry<Integer, Point2D.Double> entry : nodePositions.entrySet()) {
            int nodeId = entry.getKey();
            Point2D.Double pos = entry.getValue();
            nodeBounds.put(nodeId, new Rectangle2D.Double(
                pos.x - NODE_WIDTH/2, pos.y - NODE_HEIGHT/2, 
                NODE_WIDTH, NODE_HEIGHT));
        }
    }
    
    private void centerView() {
        if (nodePositions.isEmpty()) return;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (Point2D.Double pos : nodePositions.values()) {
            minX = Math.min(minX, pos.x - NODE_WIDTH/2);
            minY = Math.min(minY, pos.y - NODE_HEIGHT/2);
            maxX = Math.max(maxX, pos.x + NODE_WIDTH/2);
            maxY = Math.max(maxY, pos.y + NODE_HEIGHT/2);
        }
        
        double graphWidth = maxX - minX + MARGIN * 2;
        double graphHeight = maxY - minY + MARGIN * 2;
        
        if (getWidth() > 0 && getHeight() > 0) {
            scale = Math.min(
                (getWidth() - 80) / graphWidth,
                (getHeight() - 80) / graphHeight
            );
            scale = Math.max(0.3, Math.min(1.5, scale));
        }
        
        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;
        translateX = getWidth() / 2.0 - centerX * scale;
        translateY = getHeight() / 2.0 - centerY * scale;
    }
    
    private void handleNodeSelection(Point point) {
        selectedNode = null;
        if (nodeBounds.isEmpty() || graph == null) return;
        double mouseX = (point.x - translateX) / scale;
        double mouseY = (point.y - translateY) / scale;
        for (Map.Entry<Integer, Rectangle2D.Double> entry : nodeBounds.entrySet()) {
            if (entry.getValue().contains(mouseX, mouseY)) {
                selectedNode = graph.getNodeById(entry.getKey());
                break;
            }
        }
    }
    
    private void handleNodeHover(Point point) {
        hoveredNode = null;
        highlightedNodes.clear();
        if (nodeBounds.isEmpty() || graph == null) { repaint(); return; }
        
        double mouseX = (point.x - translateX) / scale;
        double mouseY = (point.y - translateY) / scale;
        
        for (Map.Entry<Integer, Rectangle2D.Double> entry : nodeBounds.entrySet()) {
            if (entry.getValue().contains(mouseX, mouseY)) {
                hoveredNode = graph.getNodeById(entry.getKey());
                if (hoveredNode != null) {
                    highlightedNodes.add(hoveredNode);
                    for (Edge edge : hoveredNode.getOutgoingEdges()) {
                        highlightedNodes.add(edge.getDestination());
                    }
                    for (Edge edge : hoveredNode.getIncomingEdges()) {
                        highlightedNodes.add(edge.getSource());
                    }
                }
                break;
            }
        }
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        
        GradientPaint bgGradient = new GradientPaint(0, 0, COLOR_BACKGROUND, getWidth(), getHeight(), new Color(240, 240, 245));
        g2d.setPaint(bgGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        if (isCalculating) {
            drawLoadingState(g2d);
            return;
        }
        
        if (!hasGraph || nodePositions.isEmpty()) {
            drawPlaceholder(g2d);
            return;
        }
        
        drawGrid(g2d);
        
        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(translateX, translateY);
        g2d.scale(scale, scale);
        
        drawEdges(g2d);
        drawNodes(g2d);
        
        g2d.setTransform(oldTransform);
        
        drawStyleIndicator(g2d);
        drawZoomInfo(g2d);
        drawLegend(g2d);
    }
    
    private void drawLoadingState(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 20));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        
        g2d.setColor(new Color(52, 152, 219, 60));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval(cx - 40, cy - 40, 80, 80);
        
        g2d.setColor(new Color(41, 128, 185));
        g2d.fillOval(cx - 15, cy - 15, 30, 30);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("G", cx - 5, cy + 6);
        
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        String msg = "Creando visualizacion del grafo...";
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(msg);
        
        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.fillRoundRect(cx - textW/2 - 15, cy + 45, textW + 30, 26, 10, 10);
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawRoundRect(cx - textW/2 - 15, cy + 45, textW + 30, 26, 10, 10);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString(msg, cx - textW/2, cy + 62);
    }
    
    private void drawStyleIndicator(Graphics2D g2d) {
        if (graph == null) return;
        String styleName = getStyleName();
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(styleName);
        int x = getWidth() - textW - 20;
        int y = 20;
        
        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.fillRoundRect(x - 8, y - 14, textW + 16, 22, 10, 10);
        g2d.setColor(new Color(150, 150, 150));
        g2d.drawRoundRect(x - 8, y - 14, textW + 16, 22, 10, 10);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString(styleName, x, y + 2);
    }
    
    // Nombres de estilos SIN emojis problematicos
    private String getStyleName() {
        switch (currentStyle) {
            case JERARQUICO: return "Jerarquico";
            case CIRCULAR: return "Circular";
            case ESPIRAL: return "Espiral";
            case HEXAGONAL: return "Hexagonal";
            case ORGANICO: return "Organico";
            case FLUJO_VERTICAL: return "Flujo Vertical";
            default: return "Desconocido";
        }
    }
    
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(COLOR_GRID);
        g2d.setStroke(new BasicStroke(0.5f));
        int gridSize = 50;
        int scaledGridSize = (int) (gridSize * scale);
        if (scaledGridSize < 10) return;
        
        int startX = (int) (translateX % scaledGridSize);
        int startY = (int) (translateY % scaledGridSize);
        
        for (int x = startX; x < getWidth(); x += scaledGridSize) {
            g2d.drawLine(x, 0, x, getHeight());
        }
        for (int y = startY; y < getHeight(); y += scaledGridSize) {
            g2d.drawLine(0, y, getWidth(), y);
        }
    }
    
    private void drawEdges(Graphics2D g2d) {
        if (graph == null) return;
        for (Edge edge : graph.getEdges()) {
            Point2D.Double sourcePos = nodePositions.get(edge.getSource().getId());
            Point2D.Double destPos = nodePositions.get(edge.getDestination().getId());
            if (sourcePos == null || destPos == null) continue;
            
            boolean isHighlighted = highlightedNodes.contains(edge.getSource()) && 
                                   highlightedNodes.contains(edge.getDestination());
            
            if (isHighlighted) {
                g2d.setColor(new Color(255, 80, 80, 200));
                g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else {
                g2d.setColor(COLOR_EDGE);
                g2d.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            
            double angle = Math.atan2(destPos.y - sourcePos.y, destPos.x - sourcePos.x);
            double startX = sourcePos.x + (NODE_WIDTH/2 - 3) * Math.cos(angle);
            double startY = sourcePos.y + (NODE_HEIGHT/2 - 3) * Math.sin(angle);
            double endX = destPos.x - (NODE_WIDTH/2 - 3) * Math.cos(angle);
            double endY = destPos.y - (NODE_HEIGHT/2 - 3) * Math.sin(angle);
            
            g2d.draw(new Line2D.Double(startX, startY, endX, endY));
            
            // Flecha
            int arrowSize = isHighlighted ? 8 : 6;
            double arrowAngle = Math.PI / 7;
            int[] xPoints = new int[3];
            int[] yPoints = new int[3];
            xPoints[0] = (int) endX;
            yPoints[0] = (int) endY;
            xPoints[1] = (int) (endX - arrowSize * Math.cos(angle - arrowAngle));
            yPoints[1] = (int) (endY - arrowSize * Math.sin(angle - arrowAngle));
            xPoints[2] = (int) (endX - arrowSize * Math.cos(angle + arrowAngle));
            yPoints[2] = (int) (endY - arrowSize * Math.sin(angle + arrowAngle));
            g2d.fillPolygon(xPoints, yPoints, 3);
        }
    }
    
    private void drawNodes(Graphics2D g2d) {
        if (graph == null) return;
        for (Node node : graph.getNodes()) {
            Point2D.Double pos = nodePositions.get(node.getId());
            if (pos == null) continue;
            
            boolean isHighlighted = highlightedNodes.contains(node);
            boolean isHovered = (node == hoveredNode);
            
            double x = pos.x;
            double y = pos.y;
            double w = NODE_WIDTH;
            double h = NODE_HEIGHT;
            
            Color[] palette = getNodePalette(node.getType());
            Color baseColor = palette[0];
            Color darkColor = palette[1];
            
            if (isHighlighted) { baseColor = baseColor.brighter(); darkColor = darkColor.brighter(); }
            if (isHovered) { baseColor = baseColor.brighter().brighter(); w *= 1.05; h *= 1.05; }
            
            // Sombra
            g2d.setColor(new Color(0, 0, 0, 25));
            g2d.fill(new RoundRectangle2D.Double(x - w/2 + 2, y - h/2 + 2, w, h, 12, 12));
            
            // Gradiente
            GradientPaint gradient = new GradientPaint(
                (float)(x - w/2), (float)(y - h/2), baseColor,
                (float)(x + w/2), (float)(y + h/2), darkColor);
            g2d.setPaint(gradient);
            g2d.fill(new RoundRectangle2D.Double(x - w/2, y - h/2, w, h, 12, 12));
            
            // Borde brillante
            if (isHighlighted) {
                g2d.setColor(new Color(255, 255, 255, 150));
                g2d.setStroke(new BasicStroke(2.5f));
            } else {
                g2d.setColor(new Color(255, 255, 255, 60));
                g2d.setStroke(new BasicStroke(1.5f));
            }
            g2d.draw(new RoundRectangle2D.Double(x - w/2, y - h/2, w, h, 12, 12));
            
            // Borde resaltado
            if (isHighlighted) {
                g2d.setColor(new Color(255, 100, 100, 200));
                g2d.setStroke(new BasicStroke(2f));
                g2d.draw(new RoundRectangle2D.Double(x - w/2 - 1, y - h/2 - 1, w + 2, h + 2, 13, 13));
            }
            
            // Texto
            String label = node.getLabel();
            if (label.length() > 22) label = label.substring(0, 19) + "...";
            
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g2d.getFontMetrics();
            int labelW = fm.stringWidth(label);
            
            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.drawString(label, (float)(x - labelW/2 + 1), (float)(y + fm.getAscent()/2));
            g2d.setColor(Color.WHITE);
            g2d.drawString(label, (float)(x - labelW/2), (float)(y + fm.getAscent()/2 - 1));
        }
    }
    
    private Color[] getNodePalette(String type) {
        if (type == null) return PALETTE_DEFAULT;
        switch (type) {
            case "ENTRY": return PALETTE_ENTRY;
            case "EXIT": return PALETTE_EXIT;
            case "IF": case "WHILE": case "FOR": case "SWITCH": return PALETTE_CONDITION;
            case "THEN": case "ELSE": case "CASE": case "ENDIF": return PALETTE_BRANCH;
            case "LOOP_BODY": case "LOOP_EXIT": return PALETTE_LOOP;
            default: return PALETTE_DEFAULT;
        }
    }
    
    private void drawPlaceholder(Graphics2D g2d) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("Arial", Font.BOLD, 15));
        String text = "Visualizacion del Grafo";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, cx - fm.stringWidth(text)/2, cy - 15);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        String subtext = "Genere un grafo para ver su estructura";
        FontMetrics fm2 = g2d.getFontMetrics();
        g2d.drawString(subtext, cx - fm2.stringWidth(subtext)/2, cy + 15);
    }
    
    private void drawZoomInfo(Graphics2D g2d) {
        if (graph != null && !nodePositions.isEmpty()) {
            String info = String.format("Zoom: %.0f%% | Nodos: %d | Aristas: %d", 
                scale * 100, graph.getNodes().size(), graph.getEdges().size());
            
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            FontMetrics fm = g2d.getFontMetrics();
            int textW = fm.stringWidth(info);
            
            g2d.setColor(new Color(255, 255, 255, 220));
            g2d.fillRoundRect(5, getHeight() - 22, textW + 14, 18, 8, 8);
            g2d.setColor(new Color(180, 180, 180));
            g2d.drawRoundRect(5, getHeight() - 22, textW + 14, 18, 8, 8);
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawString(info, 11, getHeight() - 8);
        }
    }
    
    private void drawLegend(Graphics2D g2d) {
        if (graph == null || nodePositions.isEmpty()) return;
        
        int lx = 5, ly = 5, lw = 140, lh = 160;
        
        g2d.setColor(new Color(255, 255, 255, 230));
        g2d.fillRoundRect(lx, ly, lw, lh, 10, 10);
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawRoundRect(lx, ly, lw, lh, 10, 10);
        
        g2d.setColor(Color.DARK_GRAY);
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.drawString("Leyenda", lx + 12, ly + 18);
        
        String[][] items = {
            {"Inicio/Fin", "ENTRY"},
            {"Condicion", "IF"},
            {"Ramas", "THEN"},
            {"Bucles", "LOOP_BODY"},
            {"Sentencias", "DEFAULT"}
        };
        
        int itemY = ly + 32;
        for (String[] item : items) {
            Color[] palette = getNodePalette(item[1]);
            GradientPaint gp = new GradientPaint(lx + 10, itemY - 4, palette[0], lx + 26, itemY + 6, palette[1]);
            g2d.setPaint(gp);
            g2d.fillRoundRect(lx + 10, itemY - 4, 18, 10, 4, 4);
            
            g2d.setColor(Color.DARK_GRAY);
            g2d.setFont(new Font("Arial", Font.PLAIN, 9));
            g2d.drawString(item[0], lx + 34, itemY + 4);
            itemY += 20;
        }
        
        g2d.setFont(new Font("Arial", Font.ITALIC, 8));
        g2d.setColor(Color.GRAY);
        g2d.drawString("Arrastrar: Mover", lx + 10, itemY + 10);
        g2d.drawString("Rueda: Zoom", lx + 10, itemY + 22);
    }
    
    public void zoomIn() {
        if (isCalculating || !hasGraph) return;
        scale = Math.min(5.0, scale * 1.2);
        repaint();
    }
    
    public void zoomOut() {
        if (isCalculating || !hasGraph) return;
        scale = Math.max(0.2, scale / 1.2);
        repaint();
    }
    
    public void resetView() {
        if (isCalculating || !hasGraph) return;
        scale = 1.0;
        if (!nodePositions.isEmpty()) centerView();
        repaint();
    }
    
    public void fitToScreen() {
        if (isCalculating || !hasGraph || nodePositions.isEmpty()) return;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        
        for (Point2D.Double pos : nodePositions.values()) {
            minX = Math.min(minX, pos.x - NODE_WIDTH/2);
            minY = Math.min(minY, pos.y - NODE_HEIGHT/2);
            maxX = Math.max(maxX, pos.x + NODE_WIDTH/2);
            maxY = Math.max(maxY, pos.y + NODE_HEIGHT/2);
        }
        
        double graphWidth = maxX - minX + MARGIN * 2;
        double graphHeight = maxY - minY + MARGIN * 2;
        
        if (getWidth() > 0 && getHeight() > 0) {
            scale = Math.min((getWidth() - 80) / graphWidth, (getHeight() - 80) / graphHeight);
            scale = Math.max(0.3, Math.min(1.5, scale));
        }
        centerView();
        repaint();
    }
}