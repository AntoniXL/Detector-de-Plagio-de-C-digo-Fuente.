/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package detector_de_plagio_codigo_fuente;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Establecer look and feel del sistema
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Mostrar splash screen opcional
            showSplashScreen();
            
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
    
    private static void showSplashScreen() {
        JWindow splash = new JWindow();
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        
        JLabel title = new JLabel("Detector de Plagio de Código Fuente", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        
        JLabel subtitle = new JLabel("Análisis de Isomorfismo de Grafos", SwingConstants.CENTER);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JLabel loading = new JLabel("Cargando...", SwingConstants.CENTER);
        
        content.add(title, BorderLayout.NORTH);
        content.add(subtitle, BorderLayout.CENTER);
        content.add(loading, BorderLayout.SOUTH);
        
        splash.setContentPane(content);
        splash.setSize(400, 150);
        splash.setLocationRelativeTo(null);
        splash.setVisible(true);
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        splash.setVisible(false);
        splash.dispose();
    }
}