package src.ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import src.manager.ParkingSystemFacade;

public class OverviewPanel extends JPanel {
    private JPanel gridPanel;
    private ParkingSystemFacade facade = new ParkingSystemFacade();
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

        List<Map<String, Object>> spots = facade.getMapSpots();
        for (Map<String, Object> s : spots) {
            String id = (String) s.get("id");
            String type = (String) s.get("type");
            String plate = (String) s.get("plate");
            boolean isOccupied = (boolean) s.get("occupied");

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

        gridPanel.revalidate();
        gridPanel.repaint();
    }
}