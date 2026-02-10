package src.ui;

import java.awt.*;
import javax.swing.*;

public class AdminPanel extends JPanel {
    private AdminStatsPanel statsPanel;
    private OverviewPanel overviewPanel;
    private JTabbedPane innerTabs;

    public AdminPanel() {
        setLayout(new BorderLayout());

        // 1. Initialize the sub-panels
        statsPanel = new AdminStatsPanel();
        overviewPanel = new OverviewPanel();

        // 2. Create internal tabs
        innerTabs = new JTabbedPane(JTabbedPane.TOP);
        
        // Tab A: The Stats/Reports
        innerTabs.addTab("Overview", null, statsPanel, "Manage fines and view revenue");
        
        // Tab B: The Parking Map
        innerTabs.addTab("Parking Map", null, overviewPanel, "View real-time parking status");

        add(innerTabs, BorderLayout.CENTER);

        // 3. Auto-refresh
        innerTabs.addChangeListener(e -> refreshCurrentView());
    }

    public void refreshCurrentView() {
        Component selected = innerTabs.getSelectedComponent();
        
        if (selected == overviewPanel) {
            overviewPanel.loadParkingSpots();
        } else if (selected == statsPanel) {
            statsPanel.refreshData();
        }
    }
}