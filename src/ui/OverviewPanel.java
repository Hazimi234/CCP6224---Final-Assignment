package src.ui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class OverviewPanel extends JPanel {
    private JPanel gridPanel;
    private JLabel statusLabel;

    public OverviewPanel() {
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.add(new JLabel("Legend: "));
        
        JLabel lblFree = new JLabel(" GREEN = Available ");
        lblFree.setOpaque(true);
        lblFree.setBackground(new Color(150, 255, 150));
        header.add(lblFree);

        JLabel lblOccupied = new JLabel(" RED = Occupied ");
        lblOccupied.setOpaque(true);
        lblOccupied.setBackground(new Color(255, 150, 150));
        header.add(lblOccupied);
        
        // Add a manual refresh button just in case
        JButton btnRefresh = new JButton("Refresh Map");
        btnRefresh.addActionListener(e -> loadParkingSpots());
        header.add(btnRefresh);

        add(header, BorderLayout.NORTH);

        // The Grid
        gridPanel = new JPanel();
        // 5 Floors, 10 Spots per floor (2 rows x 5 cols) = 50 spots
        gridPanel.setLayout(new GridLayout(5, 10, 5, 5)); 
        add(new JScrollPane(gridPanel), BorderLayout.CENTER);

        loadParkingSpots();
    }

    // Connects to DB and draws the squares
    public void loadParkingSpots() {
        gridPanel.removeAll(); 

        String url = "jdbc:sqlite:parking_lot.db";
        String sql = "SELECT spot_id, spot_type, current_vehicle_plate FROM parking_spots ORDER BY spot_id";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = rs.getString("spot_id");
                String type = rs.getString("spot_type");
                String plate = rs.getString("current_vehicle_plate");
                boolean isOccupied = (plate != null && !plate.isEmpty());

                JPanel spot = new JPanel();
                spot.setPreferredSize(new Dimension(80, 60));
                spot.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                spot.setLayout(new BorderLayout());

                if (isOccupied) {
                    spot.setBackground(new Color(255, 150, 150)); // Red
                    spot.add(new JLabel("<html><center><b>" + plate + "</b></center></html>", SwingConstants.CENTER), BorderLayout.CENTER);
                } else {
                    spot.setBackground(new Color(150, 255, 150)); // Green
                    spot.add(new JLabel("<html><center>" + id + "<br><small>" + type + "</small></center></html>", SwingConstants.CENTER), BorderLayout.CENTER);
                }
                
                gridPanel.add(spot);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }
}