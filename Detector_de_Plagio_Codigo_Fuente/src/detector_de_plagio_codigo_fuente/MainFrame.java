/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package detector_de_plagio_codigo_fuente;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.file.Files;
import java.util.Map;

public class MainFrame extends JFrame {
    private JTextArea codeTextArea1;
    private JTextArea codeTextArea2;
    private JTextArea resultTextArea;
    private GraphPanel graphPanel1;
    private GraphPanel graphPanel2;
    private JLabel similarityLabel;
    private JProgressBar similarityProgressBar;
    private JSpinner thresholdSpinner;
    
    private CodeParser parser;
    private IsomorphismDetector detector;
    private Graph currentGraph1;
    private Graph currentGraph2;
    
    // Referencias a los paneles contenedores
    private JPanel leftGraphContainer;
    private JPanel rightGraphContainer;
    private JSplitPane leftSplitPane;
    private JSplitPane rightSplitPane;
    private JSplitPane mainSplitPane;
    
    // Ventanas emergentes para grafos
    private JFrame graphPopup1;
    private JFrame graphPopup2;
    
    // Selectores de estilo
    private JComboBox<GraphPanel.LayoutStyle> styleCombo1;
    private JComboBox<GraphPanel.LayoutStyle> styleCombo2;
    
    public MainFrame() {
        parser = new CodeParser();
        detector = new IsomorphismDetector();
        initializeUI();
        setupKeyboardShortcuts();
    }
    
    private void initializeUI() {
        setTitle("Detector de Plagio de Código Fuente - Análisis de Isomorfismo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLayout(new BorderLayout(10, 10));
        
        // Panel principal con división horizontal
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.5);
        mainSplitPane.setDividerLocation(700);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setContinuousLayout(true);
        
        // Panel izquierdo - Programa 1
        JPanel leftPanel = createCodePanel("Programa 1 (Original)", 1);
        
        // Panel derecho - Programa 2
        JPanel rightPanel = createCodePanel("Programa 2 (Sospechoso)", 2);
        
        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(rightPanel);
        
        // Panel de resultados en la parte inferior
        JPanel resultPanel = createResultPanel();
        
        // Agregar componentes
        add(mainSplitPane, BorderLayout.CENTER);
        add(resultPanel, BorderLayout.SOUTH);
        
        // Barra de herramientas
        add(createToolBar(), BorderLayout.NORTH);
        
        // Listener para redimensionar
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSplitPaneDividers();
            }
        });
        
        setLocationRelativeTo(null);
    }
    
    private JPanel createCodePanel(String title, int programNumber) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.BLUE, 2),
            title,
            TitledBorder.CENTER,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14)
        ));
        
        // Área de código con placeholder
        JTextArea codeArea = new JTextArea(15, 40);
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        codeArea.setLineWrap(false);
        codeArea.setWrapStyleWord(false);
        
        // Placeholder text
        if (programNumber == 1) {
            codeArea.setText("// Ingrese el código del Programa 1 aquí...\n" +
                           "// O use 'Cargar Archivo' para abrir un archivo .java\n\n");
        } else {
            codeArea.setText("// Ingrese el código del Programa 2 aquí...\n" +
                           "// O use 'Cargar Archivo' para abrir un archivo .java\n\n");
        }
        
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(BorderFactory.createTitledBorder("Código Fuente"));
        
        // Crear GraphPanel personalizado
        GraphPanel graphPanel = new GraphPanel();
        graphPanel.setPreferredSize(new Dimension(500, 400));
        graphPanel.setMinimumSize(new Dimension(300, 200));
        graphPanel.setBorder(BorderFactory.createTitledBorder("Grafo de Flujo de Control"));
        
        // Panel de controles de visualización del grafo
        JPanel viewControls = createGraphControls(graphPanel, programNumber);
        
        // Panel contenedor del grafo con sus controles
        JPanel graphContainer = new JPanel(new BorderLayout());
        graphContainer.add(graphPanel, BorderLayout.CENTER);
        graphContainer.add(viewControls, BorderLayout.SOUTH);
        
        // Panel de botones principal
        JPanel buttonPanel = createButtonPanel(codeArea, graphPanel, programNumber);
        
        // Almacenar referencias a los contenedores
        if (programNumber == 1) {
            leftGraphContainer = graphContainer;
        } else {
            rightGraphContainer = graphContainer;
        }
        
        // Panel contenedor del código
        JPanel codeContainer = new JPanel(new BorderLayout());
        codeContainer.add(codeScroll, BorderLayout.CENTER);
        codeContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        // SplitPane para dividir código y grafo
        JSplitPane splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            codeContainer,
            graphContainer);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.4);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        
        // Almacenar referencia al splitPane
        if (programNumber == 1) {
            leftSplitPane = splitPane;
        } else {
            rightSplitPane = splitPane;
        }
        
        // Almacenar referencias
        if (programNumber == 1) {
            codeTextArea1 = codeArea;
            graphPanel1 = graphPanel;
        } else {
            codeTextArea2 = codeArea;
            graphPanel2 = graphPanel;
        }
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createGraphControls(GraphPanel graphPanel, int programNumber) {
        JPanel viewControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        viewControls.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(3, 3, 3, 3)
        ));
        
        // Botones de zoom y navegación
        JButton zoomInBtn = new JButton("🔍+");
        JButton zoomOutBtn = new JButton("🔍-");
        JButton fitBtn = new JButton("📐 Ajustar");
        JButton resetBtn = new JButton("🔄 Reset");
        JButton expandBtn = new JButton("⛶ Expandir");
        JButton fullScreenBtn = new JButton("🖥️");
        
        // Tooltips
        zoomInBtn.setToolTipText("Acercar zoom (Ctrl+Rueda arriba)");
        zoomOutBtn.setToolTipText("Alejar zoom (Ctrl+Rueda abajo)");
        fitBtn.setToolTipText("Ajustar grafo a la ventana");
        resetBtn.setToolTipText("Restaurar vista original");
        expandBtn.setToolTipText("Expandir/contraer el panel del grafo");
        fullScreenBtn.setToolTipText("Ver grafo en ventana independiente (F11/F12)");
        
        // Estilo de botones
        Font buttonFont = new Font("Arial", Font.PLAIN, 9);
        zoomInBtn.setFont(buttonFont);
        zoomOutBtn.setFont(buttonFont);
        fitBtn.setFont(buttonFont);
        resetBtn.setFont(buttonFont);
        expandBtn.setFont(buttonFont);
        fullScreenBtn.setFont(buttonFont);
        
        // Dimensiones compactas
        Dimension smallBtnSize = new Dimension(50, 22);
        Dimension mediumBtnSize = new Dimension(65, 22);
        Dimension largeBtnSize = new Dimension(80, 22);
        
        zoomInBtn.setPreferredSize(smallBtnSize);
        zoomOutBtn.setPreferredSize(smallBtnSize);
        fitBtn.setPreferredSize(mediumBtnSize);
        resetBtn.setPreferredSize(mediumBtnSize);
        expandBtn.setPreferredSize(largeBtnSize);
        fullScreenBtn.setPreferredSize(new Dimension(45, 22));
        
        // Acciones de zoom
        zoomInBtn.addActionListener(e -> graphPanel.zoomIn());
        zoomOutBtn.addActionListener(e -> graphPanel.zoomOut());
        fitBtn.addActionListener(e -> graphPanel.fitToScreen());
        resetBtn.addActionListener(e -> graphPanel.resetView());
        
        // Acción de expandir/contraer
        expandBtn.addActionListener(e -> {
            JSplitPane splitPane = (programNumber == 1) ? leftSplitPane : rightSplitPane;
            if (splitPane.getDividerLocation() > 100) {
                splitPane.setDividerLocation(50);
                expandBtn.setText("⮥ Contraer");
            } else {
                splitPane.setDividerLocation(300);
                expandBtn.setText("⛶ Expandir");
            }
        });
        
        // Acción de pantalla completa
        fullScreenBtn.addActionListener(e -> {
            showGraphInSeparateWindow(graphPanel, programNumber);
        });
        
        // Selector de estilo de layout
        JLabel styleLabel = new JLabel("🎨");
        styleLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        styleLabel.setToolTipText("Estilo de visualización del grafo");
        
        JComboBox<GraphPanel.LayoutStyle> styleCombo = new JComboBox<>(GraphPanel.LayoutStyle.values());
        styleCombo.setFont(new Font("Arial", Font.PLAIN, 9));
        styleCombo.setPreferredSize(new Dimension(110, 22));
        styleCombo.setMaximumRowCount(6);
        styleCombo.setToolTipText("Selecciona el estilo de visualización del grafo");
        
        // Renderer personalizado para mostrar nombres con iconos
        styleCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                label.setFont(new Font("Arial", Font.PLAIN, 10));
                if (value instanceof GraphPanel.LayoutStyle) {
                    GraphPanel.LayoutStyle style = (GraphPanel.LayoutStyle) value;
                    switch (style) {
                        case JERARQUICO: label.setText("🎯 Jerárquico"); break;
                        case CIRCULAR: label.setText("⭕ Circular"); break;
                        case ESPIRAL: label.setText("🌀 Espiral"); break;
                        case HEXAGONAL: label.setText("⬡ Hexagonal"); break;
                        case ORGANICO: label.setText("🌿 Orgánico"); break;
                        case FLUJO_VERTICAL: label.setText("📊 Flujo Vert."); break;
                    }
                }
                return label;
            }
        });
        
        styleCombo.addActionListener(e -> {
            GraphPanel.LayoutStyle selectedStyle = (GraphPanel.LayoutStyle) styleCombo.getSelectedItem();
            if (selectedStyle != null) {
                graphPanel.setLayoutStyle(selectedStyle);
            }
        });
        
        // Guardar referencia al combo para sincronizar estilos
        if (programNumber == 1) {
            styleCombo1 = styleCombo;
        } else {
            styleCombo2 = styleCombo;
        }
        
        // Agregar componentes al panel de controles
        viewControls.add(zoomInBtn);
        viewControls.add(zoomOutBtn);
        viewControls.add(fitBtn);
        viewControls.add(resetBtn);
        viewControls.add(expandBtn);
        viewControls.add(fullScreenBtn);
        viewControls.add(new JSeparator(SwingConstants.VERTICAL));
        viewControls.add(styleLabel);
        viewControls.add(styleCombo);
        
        return viewControls;
    }
    
    private JPanel createButtonPanel(JTextArea codeArea, GraphPanel graphPanel, int programNumber) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        
        JButton parseButton = new JButton("🔍 Generar Grafo");
        JButton loadFileButton = new JButton("📁 Cargar Archivo");
        JButton clearButton = new JButton("🗑️ Limpiar");
        JButton pasteButton = new JButton("📋 Pegar");
        
        // Tooltips
        parseButton.setToolTipText("Analiza el código y genera el grafo de flujo de control");
        loadFileButton.setToolTipText("Cargar código desde un archivo .java o .txt");
        clearButton.setToolTipText("Limpia el área de código y el grafo");
        pasteButton.setToolTipText("Pega código desde el portapapeles");
        
        // Dimensiones
        Dimension mainButtonSize = new Dimension(125, 28);
        parseButton.setPreferredSize(mainButtonSize);
        loadFileButton.setPreferredSize(mainButtonSize);
        clearButton.setPreferredSize(mainButtonSize);
        pasteButton.setPreferredSize(mainButtonSize);
        
        // Estilo
        Font buttonFont = new Font("Arial", Font.PLAIN, 11);
        parseButton.setFont(buttonFont);
        loadFileButton.setFont(buttonFont);
        clearButton.setFont(buttonFont);
        pasteButton.setFont(buttonFont);
        
        parseButton.addActionListener(e -> parseAndGenerateGraph(codeArea, graphPanel, programNumber));
        loadFileButton.addActionListener(e -> loadFile(codeArea, programNumber));
        clearButton.addActionListener(e -> clearPanel(codeArea, graphPanel, programNumber));
        pasteButton.addActionListener(e -> pasteFromClipboard(codeArea));
        
        buttonPanel.add(parseButton);
        buttonPanel.add(loadFileButton);
        buttonPanel.add(pasteButton);
        buttonPanel.add(clearButton);
        
        return buttonPanel;
    }
    
    private void parseAndGenerateGraph(JTextArea codeArea, GraphPanel graphPanel, int programNumber) {
        String code = codeArea.getText();
        
        // Validación rápida
        if (code == null || code.trim().isEmpty() || 
            code.startsWith("// Ingrese el código")) {
            JOptionPane.showMessageDialog(this,
                "Por favor, ingrese código válido para analizar.\n\n" +
                "Opciones disponibles:\n" +
                "• Escriba código directamente en el área de texto\n" +
                "• Use '📁 Cargar Archivo' para abrir un archivo\n" +
                "• Use '📋 Pegar' para pegar desde el portapapeles",
                "Sin Código", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Parsear y generar grafo
            Graph graph = parser.parseCode(code, "Programa " + programNumber);
            
            // Asignar al panel (el layout se calcula en background con SwingWorker)
            graphPanel.setGraph(graph);
            
            if (programNumber == 1) {
                currentGraph1 = graph;
            } else {
                currentGraph2 = graph;
            }
            
            // Actualizar comparación si ambos grafos existen
            if (currentGraph1 != null && currentGraph2 != null) {
                updateComparison();
            }
            
            // Auto-expandir el panel del grafo
            JSplitPane splitPane = (programNumber == 1) ? leftSplitPane : rightSplitPane;
            splitPane.setDividerLocation(150);
            
            // Mostrar mensaje de éxito con estadísticas
            JOptionPane.showMessageDialog(this, 
                "✅ ¡Grafo generado exitosamente!\n\n" +
                "📊 Estadísticas del grafo:\n" +
                "   • Nodos: " + graph.getNodes().size() + "\n" +
                "   • Aristas: " + graph.getEdges().size() + "\n\n" +
                "💡 Consejos de visualización:\n" +
                "   • 🎨 Use el selector de estilo para cambiar el layout\n" +
                "   • 🖱️ Arrastre para mover el grafo\n" +
                "   • 🔍 Rueda del mouse para zoom\n" +
                "   • 👆 Hover sobre nodos para ver conexiones\n" +
                "   • ⛶ Use 'Expandir' para ver el grafo más grande\n" +
                "   • 🖥️ Use 'Pantalla Completa' para ventana independiente",
                "Grafo Generado - Programa " + programNumber, 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "❌ Error al analizar el código:\n\n" + ex.getMessage() + "\n\n" +
                "Asegúrese de que el código tenga una sintaxis válida.",
                "Error de Análisis", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    private void showGraphInSeparateWindow(GraphPanel originalGraphPanel, int programNumber) {
        // Verificar si ya existe una ventana para este grafo
        JFrame existingPopup = (programNumber == 1) ? graphPopup1 : graphPopup2;
        
        if (existingPopup != null && existingPopup.isVisible()) {
            existingPopup.toFront();
            existingPopup.requestFocus();
            return;
        }
        
        // Obtener el grafo correspondiente
        Graph graph = (programNumber == 1) ? currentGraph1 : currentGraph2;
        if (graph == null) {
            JOptionPane.showMessageDialog(this,
                "No hay un grafo generado para mostrar.\n" +
                "Genere el grafo primero.",
                "Sin Grafo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Crear nueva ventana
        JFrame popupFrame = new JFrame("Grafo de Flujo de Control - Programa " + programNumber);
        popupFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Crear nuevo GraphPanel para la ventana
        GraphPanel popupGraphPanel = new GraphPanel();
        popupGraphPanel.setGraph(graph);
        
        // Copiar el estilo actual
        GraphPanel.LayoutStyle currentStyle = originalGraphPanel.getLayoutStyle();
        popupGraphPanel.setLayoutStyle(currentStyle);
        
        // Panel de controles para la ventana emergente
        JPanel popupControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        popupControls.setBorder(BorderFactory.createEtchedBorder());
        
        JButton zoomInBtn = new JButton("🔍+ Zoom");
        JButton zoomOutBtn = new JButton("🔍- Zoom");
        JButton fitBtn = new JButton("📐 Ajustar");
        JButton resetBtn = new JButton("🔄 Reset");
        
        // Selector de estilo en ventana emergente
        JLabel styleLabel = new JLabel("🎨 Estilo:");
        styleLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        
        JComboBox<GraphPanel.LayoutStyle> popupStyleCombo = new JComboBox<>(GraphPanel.LayoutStyle.values());
        popupStyleCombo.setFont(new Font("Arial", Font.PLAIN, 10));
        popupStyleCombo.setSelectedItem(currentStyle);
        popupStyleCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                label.setFont(new Font("Arial", Font.PLAIN, 10));
                if (value instanceof GraphPanel.LayoutStyle) {
                    GraphPanel.LayoutStyle style = (GraphPanel.LayoutStyle) value;
                    switch (style) {
                        case JERARQUICO: label.setText("🎯 Jerárquico"); break;
                        case CIRCULAR: label.setText("⭕ Circular"); break;
                        case ESPIRAL: label.setText("🌀 Espiral"); break;
                        case HEXAGONAL: label.setText("⬡ Hexagonal"); break;
                        case ORGANICO: label.setText("🌿 Orgánico"); break;
                        case FLUJO_VERTICAL: label.setText("📊 Flujo Vertical"); break;
                    }
                }
                return label;
            }
        });
        
        popupStyleCombo.addActionListener(e -> {
            GraphPanel.LayoutStyle selectedStyle = (GraphPanel.LayoutStyle) popupStyleCombo.getSelectedItem();
            if (selectedStyle != null) {
                popupGraphPanel.setLayoutStyle(selectedStyle);
            }
        });
        
        JButton closeBtn = new JButton("✖ Cerrar");
        
        zoomInBtn.addActionListener(e -> popupGraphPanel.zoomIn());
        zoomOutBtn.addActionListener(e -> popupGraphPanel.zoomOut());
        fitBtn.addActionListener(e -> popupGraphPanel.fitToScreen());
        resetBtn.addActionListener(e -> popupGraphPanel.resetView());
        closeBtn.addActionListener(e -> popupFrame.dispose());
        
        popupControls.add(zoomInBtn);
        popupControls.add(zoomOutBtn);
        popupControls.add(fitBtn);
        popupControls.add(resetBtn);
        popupControls.add(new JSeparator(SwingConstants.VERTICAL));
        popupControls.add(styleLabel);
        popupControls.add(popupStyleCombo);
        popupControls.add(new JSeparator(SwingConstants.VERTICAL));
        popupControls.add(closeBtn);
        
        // Panel principal de la ventana emergente
        JPanel popupPanel = new JPanel(new BorderLayout());
        popupPanel.add(popupGraphPanel, BorderLayout.CENTER);
        popupPanel.add(popupControls, BorderLayout.SOUTH);
        
        // Información del grafo
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        infoPanel.setBackground(new Color(240, 240, 245));
        infoPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        
        JLabel infoLabel = new JLabel(String.format(
            "📊 Programa %d | Nodos: %d | Aristas: %d | 🖱️ Arrastre para mover | 🔍 Rueda para zoom",
            programNumber,
            graph.getNodes().size(),
            graph.getEdges().size()));
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        infoPanel.add(infoLabel);
        popupPanel.add(infoPanel, BorderLayout.NORTH);
        
        popupFrame.add(popupPanel);
        
        // Configurar tamaño y posición
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int)(screenSize.width * 0.75);
        int height = (int)(screenSize.height * 0.75);
        popupFrame.setSize(width, height);
        
        // Posicionar según el programa
        if (programNumber == 1) {
            popupFrame.setLocation(50, 50);
        } else {
            popupFrame.setLocation(screenSize.width - width - 50, 50);
        }
        
        // Guardar referencia
        if (programNumber == 1) {
            graphPopup1 = popupFrame;
        } else {
            graphPopup2 = popupFrame;
        }
        
        // Listener para limpiar referencia al cerrar
        popupFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (programNumber == 1) {
                    graphPopup1 = null;
                } else {
                    graphPopup2 = null;
                }
            }
        });
        
        popupFrame.setVisible(true);
        
        // Ajustar el grafo a la ventana después de mostrarla
        SwingUtilities.invokeLater(() -> {
            popupGraphPanel.fitToScreen();
        });
    }
    
    private void loadFile(JTextArea codeArea, int programNumber) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar archivo de código fuente - Programa " + programNumber);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || 
                       f.getName().toLowerCase().endsWith(".java") ||
                       f.getName().toLowerCase().endsWith(".txt") ||
                       f.getName().toLowerCase().endsWith(".c") ||
                       f.getName().toLowerCase().endsWith(".cpp") ||
                       f.getName().toLowerCase().endsWith(".py") ||
                       f.getName().toLowerCase().endsWith(".js");
            }
            public String getDescription() {
                return "Archivos de código (*.java, *.txt, *.c, *.cpp, *.py, *.js)";
            }
        });
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                codeArea.setText(content);
                codeArea.setCaretPosition(0);
                JOptionPane.showMessageDialog(this,
                    "✅ Archivo cargado exitosamente:\n" + file.getName() + "\n\n" +
                    "Presione '🔍 Generar Grafo' para analizar el código.",
                    "Archivo Cargado", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "❌ Error al leer el archivo:\n" + ex.getMessage(),
                    "Error de Lectura", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void clearPanel(JTextArea codeArea, GraphPanel graphPanel, int programNumber) {
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de limpiar el código y el grafo del Programa " + programNumber + "?\n\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar Limpieza", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            codeArea.setText("// Ingrese el código del Programa " + programNumber + " aquí...\n" +
                           "// O use 'Cargar Archivo' para abrir un archivo .java\n\n");
            codeArea.setCaretPosition(0);
            graphPanel.setGraph(null);
            
            if (programNumber == 1) {
                currentGraph1 = null;
                if (graphPopup1 != null) {
                    graphPopup1.dispose();
                    graphPopup1 = null;
                }
            } else {
                currentGraph2 = null;
                if (graphPopup2 != null) {
                    graphPopup2.dispose();
                    graphPopup2 = null;
                }
            }
            
            if (currentGraph1 == null && currentGraph2 == null) {
                clearResults();
            }
        }
    }
    
    private void pasteFromClipboard(JTextArea codeArea) {
        try {
            String clipboardText = (String) Toolkit.getDefaultToolkit()
                .getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
            if (clipboardText != null && !clipboardText.trim().isEmpty()) {
                codeArea.setText(clipboardText);
                codeArea.setCaretPosition(0);
            } else {
                JOptionPane.showMessageDialog(this,
                    "El portapapeles está vacío o no contiene texto.",
                    "Portapapeles Vacío", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error al acceder al portapapeles.\n" +
                "Asegúrese de haber copiado texto previamente.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GREEN, 2),
            "Resultados del Análisis de Isomorfismo",
            TitledBorder.CENTER,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14)
        ));
        
        // Panel de similitud
        JPanel similarityPanel = new JPanel(new GridBagLayout());
        similarityPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        
        // Etiqueta de similitud
        similarityLabel = new JLabel("Similitud: 0%");
        similarityLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        similarityPanel.add(similarityLabel, gbc);
        
        // Barra de progreso
        similarityProgressBar = new JProgressBar(0, 100);
        similarityProgressBar.setPreferredSize(new Dimension(250, 30));
        similarityProgressBar.setStringPainted(true);
        similarityProgressBar.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridx = 1;
        gbc.gridy = 0;
        similarityPanel.add(similarityProgressBar, gbc);
        
        // Control de umbral
        JPanel thresholdPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JLabel thresholdLabel = new JLabel("Umbral de detección:");
        thresholdLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        thresholdSpinner = new JSpinner(new SpinnerNumberModel(0.7, 0.0, 1.0, 0.05));
        thresholdSpinner.setPreferredSize(new Dimension(70, 25));
        thresholdSpinner.setFont(new Font("Arial", Font.PLAIN, 12));
        thresholdSpinner.setToolTipText("Ajusta la sensibilidad de detección de plagio (0.0 - 1.0)");
        
        JLabel thresholdInfoLabel = new JLabel("(0.7 = 70% recomendado)");
        thresholdInfoLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        thresholdInfoLabel.setForeground(Color.GRAY);
        
        thresholdSpinner.addChangeListener(e -> {
            double threshold = (Double) thresholdSpinner.getValue();
            detector.setSimilarityThreshold(threshold);
            if (currentGraph1 != null && currentGraph2 != null) {
                updateComparison();
            }
        });
        
        thresholdPanel.add(thresholdLabel);
        thresholdPanel.add(thresholdSpinner);
        thresholdPanel.add(thresholdInfoLabel);
        
        gbc.gridx = 2;
        gbc.gridy = 0;
        similarityPanel.add(thresholdPanel, gbc);
        
        // Área de resultados
        resultTextArea = new JTextArea(8, 60);
        resultTextArea.setEditable(false);
        resultTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultTextArea.setBackground(new Color(245, 245, 245));
        JScrollPane resultScroll = new JScrollPane(resultTextArea);
        resultScroll.setBorder(BorderFactory.createLoweredBevelBorder());
        
        // Panel de botones de resultados
        JPanel resultButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
        JButton exportButton = new JButton("📊 Exportar Resultados");
        JButton clearResultButton = new JButton("🗑️ Limpiar Resultados");
        JButton detailsButton = new JButton("📋 Ver Detalles");
        
        exportButton.setToolTipText("Guarda los resultados en un archivo de texto");
        clearResultButton.setToolTipText("Limpia el panel de resultados");
        detailsButton.setToolTipText("Muestra información detallada del análisis");
        
        exportButton.addActionListener(e -> exportResults());
        clearResultButton.addActionListener(e -> clearResults());
        detailsButton.addActionListener(e -> showDetailedAnalysis());
        
        resultButtonPanel.add(exportButton);
        resultButtonPanel.add(detailsButton);
        resultButtonPanel.add(clearResultButton);
        
        panel.add(similarityPanel, BorderLayout.NORTH);
        panel.add(resultScroll, BorderLayout.CENTER);
        panel.add(resultButtonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JButton compareButton = new JButton("🔄 Comparar Programas");
        JButton clearAllButton = new JButton("🗑️ Limpiar Todo");
        JButton expandAllBtn = new JButton("⛶ Expandir Grafos");
        JButton syncStyleBtn = new JButton("🎨 Sincronizar Estilos");
        JButton helpButton = new JButton("❓ Ayuda");
        JButton aboutButton = new JButton("ℹ️ Acerca de");
        
        Font toolbarFont = new Font("Arial", Font.PLAIN, 12);
        compareButton.setFont(toolbarFont);
        clearAllButton.setFont(toolbarFont);
        expandAllBtn.setFont(toolbarFont);
        syncStyleBtn.setFont(toolbarFont);
        helpButton.setFont(toolbarFont);
        aboutButton.setFont(toolbarFont);
        
        compareButton.setToolTipText("Compara los grafos de flujo de control (Ctrl+Enter)");
        clearAllButton.setToolTipText("Limpia todos los campos, grafos y resultados (Ctrl+L)");
        expandAllBtn.setToolTipText("Expande/contrae ambos paneles de grafos (Ctrl+E)");
        syncStyleBtn.setToolTipText("Aplica el mismo estilo de visualización a ambos grafos");
        helpButton.setToolTipText("Muestra la guía de uso del sistema (F1)");
        aboutButton.setToolTipText("Información sobre el detector de plagio");
        
        compareButton.addActionListener(e -> {
            if (currentGraph1 != null && currentGraph2 != null) {
                updateComparison();
                JOptionPane.showMessageDialog(this,
                    "✅ Comparación completada.\nRevise los resultados en el panel inferior.",
                    "Comparación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } else {
                showComparisonError();
            }
        });
        
        clearAllButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de limpiar todos los datos?\n\n" +
                "Se eliminarán:\n" +
                "• Código fuente de ambos programas\n" +
                "• Grafos generados\n" +
                "• Resultados del análisis\n\n" +
                "Esta acción no se puede deshacer.",
                "Confirmar Limpieza Total", 
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                clearAll();
                JOptionPane.showMessageDialog(this,
                    "✅ Todos los datos han sido limpiados exitosamente.",
                    "Limpieza Completa", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        expandAllBtn.addActionListener(e -> {
            if (leftSplitPane.getDividerLocation() > 100) {
                leftSplitPane.setDividerLocation(50);
                rightSplitPane.setDividerLocation(50);
                expandAllBtn.setText("⮥ Contraer Grafos");
            } else {
                leftSplitPane.setDividerLocation(300);
                rightSplitPane.setDividerLocation(300);
                expandAllBtn.setText("⛶ Expandir Grafos");
            }
        });
        
        syncStyleBtn.addActionListener(e -> {
            if (styleCombo1 != null && styleCombo2 != null) {
                GraphPanel.LayoutStyle style1 = (GraphPanel.LayoutStyle) styleCombo1.getSelectedItem();
                styleCombo2.setSelectedItem(style1);
                JOptionPane.showMessageDialog(this,
                    "✅ Estilos sincronizados: " + getStyleDisplayName(style1),
                    "Sincronización Exitosa", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        helpButton.addActionListener(e -> showHelp());
        aboutButton.addActionListener(e -> showAbout());
        
        toolBar.add(compareButton);
        toolBar.add(expandAllBtn);
        toolBar.add(syncStyleBtn);
        toolBar.addSeparator();
        toolBar.add(clearAllButton);
        toolBar.addSeparator();
        toolBar.add(helpButton);
        toolBar.add(aboutButton);
        
        return toolBar;
    }
    
    private String getStyleDisplayName(GraphPanel.LayoutStyle style) {
        if (style == null) return "Desconocido";
        switch (style) {
            case JERARQUICO: return "🎯 Jerárquico";
            case CIRCULAR: return "⭕ Circular";
            case ESPIRAL: return "🌀 Espiral";
            case HEXAGONAL: return "⬡ Hexagonal";
            case ORGANICO: return "🌿 Orgánico";
            case FLUJO_VERTICAL: return "📊 Flujo Vertical";
            default: return style.toString();
        }
    }
    
    private void setupKeyboardShortcuts() {
        JRootPane rootPane = getRootPane();
        
        // Ctrl+Enter para comparar
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "compare");
        rootPane.getActionMap().put("compare", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (currentGraph1 != null && currentGraph2 != null) {
                    updateComparison();
                }
            }
        });
        
        // Ctrl+L para limpiar todo
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "clearAll");
        rootPane.getActionMap().put("clearAll", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                clearAll();
            }
        });
        
        // Ctrl+E para expandir/contraer grafos
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "expandGraphs");
        rootPane.getActionMap().put("expandGraphs", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (leftSplitPane.getDividerLocation() > 100) {
                    leftSplitPane.setDividerLocation(50);
                    rightSplitPane.setDividerLocation(50);
                } else {
                    leftSplitPane.setDividerLocation(300);
                    rightSplitPane.setDividerLocation(300);
                }
            }
        });
        
        // Ctrl+1 a Ctrl+6 para cambiar estilos
        for (int i = 0; i < GraphPanel.LayoutStyle.values().length; i++) {
            final int index = i;
            final GraphPanel.LayoutStyle style = GraphPanel.LayoutStyle.values()[i];
            
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, InputEvent.CTRL_DOWN_MASK), "style" + i);
            rootPane.getActionMap().put("style" + i, new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    if (styleCombo1 != null) styleCombo1.setSelectedItem(style);
                    if (styleCombo2 != null) styleCombo2.setSelectedItem(style);
                }
            });
        }
        
        // F11/F12 para pantalla completa
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "fullScreen1");
        rootPane.getActionMap().put("fullScreen1", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (graphPanel1 != null) showGraphInSeparateWindow(graphPanel1, 1);
            }
        });
        
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "fullScreen2");
        rootPane.getActionMap().put("fullScreen2", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (graphPanel2 != null) showGraphInSeparateWindow(graphPanel2, 2);
            }
        });
        
        // F1 para ayuda
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "help");
        rootPane.getActionMap().put("help", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                showHelp();
            }
        });
    }
    
    private void updateSplitPaneDividers() {
        if (leftSplitPane != null && leftSplitPane.getDividerLocation() < 50) {
            leftSplitPane.setDividerLocation(50);
        }
        if (rightSplitPane != null && rightSplitPane.getDividerLocation() < 50) {
            rightSplitPane.setDividerLocation(50);
        }
    }
    
    private void updateComparison() {
        if (currentGraph1 == null || currentGraph2 == null) return;
        
        IsomorphismResult result = detector.detectIsomorphism(currentGraph1, currentGraph2);
        
        int similarityPercent = (int) (result.getFinalSimilarity() * 100);
        similarityProgressBar.setValue(similarityPercent);
        similarityLabel.setText(String.format("Similitud: %d%%", similarityPercent));
        
        if (result.isIsomorphic()) {
            similarityProgressBar.setForeground(new Color(220, 50, 50));
            similarityLabel.setForeground(new Color(180, 0, 0));
        } else if (result.getFinalSimilarity() > 0.5) {
            similarityProgressBar.setForeground(new Color(255, 140, 0));
            similarityLabel.setForeground(new Color(200, 100, 0));
        } else {
            similarityProgressBar.setForeground(new Color(50, 150, 50));
            similarityLabel.setForeground(new Color(0, 100, 0));
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("══════════════════════════════════════════════════\n");
        sb.append("        RESULTADOS DEL ANÁLISIS DE PLAGIO          \n");
        sb.append("══════════════════════════════════════════════════\n\n");
        
        String veredict;
        String icon;
        if (result.isIsomorphic()) {
            icon = "🚨";
            veredict = "POSIBLE PLAGIO DETECTADO";
        } else if (result.getFinalSimilarity() > 0.5) {
            icon = "⚠️";
            veredict = "SIMILITUD MODERADA - REVISAR";
        } else {
            icon = "✅";
            veredict = "NO SE DETECTA PLAGIO SIGNIFICATIVO";
        }
        
        sb.append("VEREDICTO: ").append(icon).append(" ").append(veredict).append("\n\n");
        sb.append("──────────────────────────────────────────────────\n");
        sb.append("MÉTRICAS DE SIMILITUD:\n");
        sb.append("──────────────────────────────────────────────────\n\n");
        sb.append(String.format("Similitud Final:        %.1f%%\n", result.getFinalSimilarity() * 100));
        sb.append(String.format("├─ Similitud Estructural:   %.1f%%\n", result.getStructuralSimilarity() * 100));
        sb.append(String.format("├─ Similitud de Subgrafos:  %.1f%%\n", result.getSubgraphSimilarity() * 100));
        sb.append(String.format("└─ Similitud de Secuencias: %.1f%%\n\n", result.getSequenceSimilarity() * 100));
        
        sb.append("──────────────────────────────────────────────────\n");
        sb.append("CARACTERÍSTICAS DEL PROGRAMA 1:\n");
        sb.append(formatGraphFeatures(currentGraph1));
        sb.append("\nCARACTERÍSTICAS DEL PROGRAMA 2:\n");
        sb.append(formatGraphFeatures(currentGraph2));
        
        sb.append("\n──────────────────────────────────────────────────\n");
        sb.append("RECOMENDACIÓN:\n");
        if (result.isIsomorphic()) {
            sb.append("⚠️  Se recomienda revisión manual exhaustiva.\n");
        } else if (result.getFinalSimilarity() > 0.5) {
            sb.append("⚡ Existen similitudes que merecen atención.\n");
        } else {
            sb.append("✓  Los programas son estructuralmente diferentes.\n");
        }
        
        resultTextArea.setText(sb.toString());
        resultTextArea.setCaretPosition(0);
    }
    
    private String formatGraphFeatures(Graph graph) {
        Map<String, Object> features = graph.getGraphFeatures();
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.format("  • Nodos: %d\n", features.get("nodeCount")));
        sb.append(String.format("  • Aristas: %d\n", features.get("edgeCount")));
        sb.append(String.format("  • Grado entrada prom: %.2f\n", features.get("avgInDegree")));
        sb.append(String.format("  • Grado salida prom: %.2f\n", features.get("avgOutDegree")));
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> typeDist = (Map<String, Integer>) features.get("typeDistribution");
        sb.append("  • Distribución:\n");
        for (Map.Entry<String, Integer> entry : typeDist.entrySet()) {
            sb.append(String.format("    - %s: %d\n", getTypeNameInSpanish(entry.getKey()), entry.getValue()));
        }
        
        return sb.toString();
    }
    
    private String getTypeNameInSpanish(String type) {
        switch (type) {
            case "ENTRY": return "Entrada";
            case "EXIT": return "Salida";
            case "IF": return "Condicional";
            case "WHILE": return "Bucle While";
            case "FOR": return "Bucle For";
            case "THEN": return "Rama Then";
            case "ELSE": return "Rama Else";
            case "ENDIF": return "Fin If";
            case "LOOP_BODY": return "Cuerpo bucle";
            case "LOOP_EXIT": return "Salida bucle";
            case "STATEMENT": return "Sentencia";
            case "MERGE": return "Unión";
            default: return type;
        }
    }
    
    private void showComparisonError() {
        StringBuilder message = new StringBuilder();
        message.append("No se pueden comparar los programas.\n\n");
        message.append("Estado actual:\n");
        
        if (currentGraph1 == null) {
            message.append("❌ Programa 1: Sin grafo generado\n");
        } else {
            message.append("✅ Programa 1: Grafo disponible (");
            message.append(currentGraph1.getNodes().size()).append(" nodos)\n");
        }
        
        if (currentGraph2 == null) {
            message.append("❌ Programa 2: Sin grafo generado\n");
        } else {
            message.append("✅ Programa 2: Grafo disponible (");
            message.append(currentGraph2.getNodes().size()).append(" nodos)\n");
        }
        
        JOptionPane.showMessageDialog(this, message.toString(), 
            "Información de Estado", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showDetailedAnalysis() {
        if (currentGraph1 == null || currentGraph2 == null) {
            JOptionPane.showMessageDialog(this,
                "Genere los grafos de ambos programas primero.",
                "Análisis no disponible", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        IsomorphismResult result = detector.detectIsomorphism(currentGraph1, currentGraph2);
        
        StringBuilder details = new StringBuilder();
        details.append("ANÁLISIS DETALLADO DE ISOMORFISMO\n");
        details.append("=================================\n\n");
        details.append("PROGRAMA 1:\n");
        details.append(formatGraphFeatures(currentGraph1));
        details.append("\nPROGRAMA 2:\n");
        details.append(formatGraphFeatures(currentGraph2));
        details.append("\nMÉTRICAS:\n");
        details.append(String.format("Estructural: %.2f%%\n", result.getStructuralSimilarity() * 100));
        details.append(String.format("Subgrafos: %.2f%%\n", result.getSubgraphSimilarity() * 100));
        details.append(String.format("Secuencias: %.2f%%\n", result.getSequenceSimilarity() * 100));
        details.append(String.format("\nSIMILITUD FINAL: %.2f%%\n", result.getFinalSimilarity() * 100));
        
        JTextArea detailsArea = new JTextArea(details.toString());
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(detailsArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        
        JOptionPane.showMessageDialog(this, scrollPane,
            "Análisis Detallado", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void exportResults() {
        if (resultTextArea.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No hay resultados para exportar.",
                "Sin Resultados", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar resultados del análisis");
        fileChooser.setSelectedFile(new File("resultados_plagio.txt"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }
            
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.println("═══════════════════════════════════════════");
                writer.println("  REPORTE DE ANÁLISIS DE PLAGIO DE CÓDIGO");
                writer.println("═══════════════════════════════════════════");
                writer.println("Fecha: " + new java.util.Date().toString());
                writer.println();
                writer.println(resultTextArea.getText());
                writer.println();
                writer.println("═══════════════════════════════════════════");
                
                JOptionPane.showMessageDialog(this,
                    "✅ Resultados exportados exitosamente.",
                    "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "❌ Error al exportar:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void clearResults() {
        resultTextArea.setText("");
        similarityProgressBar.setValue(0);
        similarityLabel.setText("Similitud: 0%");
        similarityLabel.setForeground(Color.BLACK);
        similarityProgressBar.setForeground(new Color(50, 150, 50));
    }
    
    private void clearAll() {
        // Limpiar Programa 1
        codeTextArea1.setText("// Ingrese el código del Programa 1 aquí...\n" +
                            "// O use 'Cargar Archivo' para abrir un archivo .java\n\n");
        codeTextArea1.setCaretPosition(0);
        graphPanel1.setGraph(null);
        currentGraph1 = null;
        
        // Limpiar Programa 2
        codeTextArea2.setText("// Ingrese el código del Programa 2 aquí...\n" +
                            "// O use 'Cargar Archivo' para abrir un archivo .java\n\n");
        codeTextArea2.setCaretPosition(0);
        graphPanel2.setGraph(null);
        currentGraph2 = null;
        
        // Cerrar ventanas emergentes
        if (graphPopup1 != null) {
            graphPopup1.dispose();
            graphPopup1 = null;
        }
        if (graphPopup2 != null) {
            graphPopup2.dispose();
            graphPopup2 = null;
        }
        
        clearResults();
        
        // Restaurar divisores
        if (leftSplitPane != null) leftSplitPane.setDividerLocation(300);
        if (rightSplitPane != null) rightSplitPane.setDividerLocation(300);
    }
    
    private void showHelp() {
        String helpMessage = 
            "╔════════════════════════════════════════════════╗\n" +
            "║     DETECTOR DE PLAGIO DE CÓDIGO FUENTE      ║\n" +
            "╚════════════════════════════════════════════════╝\n\n" +
            "📌 GUÍA DE USO:\n\n" +
            "1️⃣  INGRESAR CÓDIGO:\n" +
            "   • Escriba directamente o cargue archivos\n" +
            "   • Use '📁 Cargar Archivo' o '📋 Pegar'\n\n" +
            "2️⃣  GENERAR GRAFOS:\n" +
            "   • Presione '🔍 Generar Grafo'\n\n" +
            "3️⃣  ESTILOS DE VISUALIZACIÓN:\n" +
            "   • 🎯 Jerárquico: Árbol de arriba abajo\n" +
            "   • ⭕ Circular: Nodos en círculos\n" +
            "   • 🌀 Espiral: Patrón espiral\n" +
            "   • ⬡ Hexagonal: Diseño de panal\n" +
            "   • 🌿 Orgánico: Dispersión natural\n" +
            "   • 📊 Flujo Vertical: Columnas\n\n" +
            "4️⃣  NAVEGACIÓN:\n" +
            "   • 🖱️ Arrastrar: Mover\n" +
            "   • 🔍 Rueda: Zoom\n" +
            "   • F11/F12: Pantalla completa\n\n" +
            "5️⃣  ATAJOS DE TECLADO:\n" +
            "   • Ctrl+Enter: Comparar\n" +
            "   • Ctrl+1-6: Cambiar estilo\n" +
            "   • Ctrl+E: Expandir grafos\n" +
            "   • Ctrl+L: Limpiar todo\n" +
            "   • F1: Ayuda";
        
        JTextArea helpArea = new JTextArea(helpMessage);
        helpArea.setEditable(false);
        helpArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(helpArea);
        scrollPane.setPreferredSize(new Dimension(550, 500));
        
        JOptionPane.showMessageDialog(this, scrollPane, 
            "Ayuda del Sistema", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showAbout() {
        String aboutMessage = 
            "╔══════════════════════════════════════════╗\n" +
            "║   DETECTOR DE PLAGIO DE CÓDIGO FUENTE  ║\n" +
            "╚══════════════════════════════════════════╝\n\n" +
            "Versión 4.0 - Visualización Creativa\n\n" +
            "CARACTERÍSTICAS:\n" +
            "• 6 estilos de visualización\n" +
            "• Layouts creativos y profesionales\n" +
            "• Zoom y navegación interactiva\n" +
            "• Múltiples métricas de comparación\n" +
            "• Exportación de resultados\n" +
            "• Ventanas independientes\n\n" +
            "© 2024 - Todos los derechos reservados";
        
        JTextArea aboutArea = new JTextArea(aboutMessage);
        aboutArea.setEditable(false);
        aboutArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JOptionPane.showMessageDialog(this, aboutArea, 
            "Acerca del Sistema", JOptionPane.INFORMATION_MESSAGE);
    }
}