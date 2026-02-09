package src.ui;

import javax.swing.*;
import src.util.DatabaseSetup; // Import your setup tool

public class MainApp extends JFrame {

    public MainApp() {
        setTitle("University Parking Management System (Group Project)");
        setSize(1000, 700);
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

        // Tab 4: Member C's Admin Panel (Placeholder for now)
        JPanel adminPlaceholder = new JPanel();
        adminPlaceholder.add(new JLabel("Member C will build the Admin/Reports Screen here next."));
        tabbedPane.addTab("Admin & Reports", null, adminPlaceholder, "View revenue and fines");

        // --- THE MAGIC GLUE ---
        // This makes the map refresh automatically when you click the tab!
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == overviewPanel) {
                overviewPanel.loadParkingSpots();
            }
        });

        add(tabbedPane);
        setVisible(true);
    }
}