package src.ui;

import java.awt.*;
import javax.swing.*; // <--- This fixes the "Component cannot be resolved" error
import src.util.DatabaseSetup;

public class MainApp extends JFrame {

    public MainApp() {
        setTitle("University Parking Management System");
        setSize(900, 600); // Made it slightly bigger to fit everything nicely
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen

        // Initialize DB if it doesn't exist yet
        DatabaseSetup.createNewDatabase();

        // Create the Tabs
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Member B's Entry Panel
        EntryPanel entryPanel = new EntryPanel();
        tabbedPane.addTab("Vehicle Entry", null, entryPanel, "Park a new vehicle");

        // Tab 2: Member B's Exit Panel
        ExitPanel exitPanel = new ExitPanel();
        tabbedPane.addTab("Vehicle Exit & Payment", null, exitPanel, "Process payments");

        // Tab 3: Member C's Admin Panel
        AdminPanel adminPanel = new AdminPanel();
        tabbedPane.addTab("Admin Panel", null, adminPanel, "View revenue, fines, and map");

        // Tab 4: Reports Panel
        ReportPanel reportPanel = new ReportPanel();
        tabbedPane.addTab("Reports", null, reportPanel, "Detailed statistics and logs");

        // --- THE MAGIC GLUE ---
        // This makes the screens refresh automatically when you click the tabs!
        tabbedPane.addChangeListener(e -> {
            Component selected = tabbedPane.getSelectedComponent();
            
            if (selected == adminPanel) {
                adminPanel.refreshCurrentView(); // Refresh whatever is currently showing in Admin
            } else if (selected == reportPanel) {
                reportPanel.refreshCurrentTab(); // Refresh the active report tab
            }
        });

        add(tabbedPane);
        setVisible(true);
    }
}