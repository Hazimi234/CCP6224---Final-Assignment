package src.ui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import src.model.ParkingSpot; // Import the class you made earlier

public class ParkingLotGUI extends JFrame {
    private JPanel gridPanel;
    // Hardcoded for simplicity, but in real life you'd count rows/cols
    private int floors = 5; 
    private int rows = 2;   
    private int spotsPerRow = 10;

    public ParkingLotGUI() {
        setTitle("University Parking Lot System - Main View");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. Top Header
        JLabel titleLabel = new JLabel("Real-Time Parking Status", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);

        // 2. The Grid of Spots (Center)
        gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(floors, rows * spotsPerRow, 5, 5)); // A simple grid layout
        add(new JScrollPane(gridPanel), BorderLayout.CENTER);

        // 3. Refresh Button (Bottom)
        JButton refreshBtn = new JButton("Refresh Status");
        refreshBtn.addActionListener(e -> loadParkingSpots()); // Re-loads DB when clicked
        add(refreshBtn, BorderLayout.SOUTH);

        // Load data immediately when opened
        loadParkingSpots();
        
        setVisible(true);
    }

    private void loadParkingSpots() {
        gridPanel.removeAll(); // Clear old squares

        // Connect to DB and get all spots
        String url = "jdbc:sqlite:parking_lot.db";
        String sql = "SELECT id, spot_type, is_occupied FROM parking_spots ORDER BY id";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = rs.getString("id");
                String type = rs.getString("spot_type");
                boolean isOccupied = rs.getInt("is_occupied") == 1;

                // Create a visual "Square" for this spot
                JPanel spotPanel = new JPanel();
                spotPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                
                // Color logic: Red if taken, Green if free
                if (isOccupied) {
                    spotPanel.setBackground(new Color(255, 100, 100)); // Light Red
                } else {
                    spotPanel.setBackground(new Color(100, 255, 100)); // Light Green
                }

                spotPanel.add(new JLabel("<html><center>" + id + "<br>" + type + "</center></html>"));
                gridPanel.add(spotPanel);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    // Temporary Main method to test JUST this screen
    public static void main(String[] args) {
        // Ensure the DB exists first!
        new ParkingLotGUI();
    }
}