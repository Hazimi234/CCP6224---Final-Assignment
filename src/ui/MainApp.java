package src.ui;

import javax.swing.*;
import java.awt.*; // <--- This fixes the "Component cannot be resolved" error
import src.util.DatabaseSetup;

public class MainApp extends JFrame {

    public MainApp() {
        setTitle("University Parking Management System (Group Project)");
        setSize(1100, 750); // Made it slightly bigger to fit everything nicely
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen

        // Initialize DB if it doesn't exist yet
        DatabaseSetup.createNewDatabase();

        // Create the Tabs
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Member B's Entry Panel
        EntryPanel entryPanel = new EntryPanel();
        tabbedPane.addTab("Vehicle Entry", null, entryPanel, "Park a new vehicle");

        // Tab 2: Member A's Overview Map
        OverviewPanel overviewPanel = new OverviewPanel();
        tabbedPane.addTab("Parking Map (Live)", null, overviewPanel, "View all spots");

        // Tab 3: Member B's Exit Panel
        ExitPanel exitPanel = new ExitPanel();
        tabbedPane.addTab("Vehicle Exit & Payment", null, exitPanel, "Process payments");

        // Tab 4: Member C's Admin Panel
        AdminPanel adminPanel = new AdminPanel();
        tabbedPane.addTab("Admin & Reports", null, adminPanel, "View revenue and fines");

        // --- THE MAGIC GLUE ---
        // This makes the screens refresh automatically when you click the tabs!
        tabbedPane.addChangeListener(e -> {
            Component selected = tabbedPane.getSelectedComponent();
            
            if (selected == overviewPanel) {
                overviewPanel.loadParkingSpots(); // Refresh Map
            } else if (selected == adminPanel) {
                adminPanel.refreshData(); // Refresh Admin Stats
            }
        });

        add(tabbedPane);
        setVisible(true);
    }
}