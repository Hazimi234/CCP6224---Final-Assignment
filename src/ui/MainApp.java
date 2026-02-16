package src.ui;

import java.awt.*;
import javax.swing.*;
import src.util.DatabaseSetup;

public class MainApp extends JFrame {

    public MainApp() {
        // --- Window Setup ---
        setTitle("University Parking Lot Management System");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Ensures app stops when 'X' is clicked
        setLocationRelativeTo(null);

        // Initialize DB if it doesn't exist yet
        DatabaseSetup.createNewDatabase();

        // Create the Tabs
        JTabbedPane tabbedPane = new JTabbedPane();

        // 1. Entry Tab: Where users enter car details to park
        EntryPanel entryPanel = new EntryPanel();
        tabbedPane.addTab("Vehicle Entry", null, entryPanel, "Park a new vehicle");

        // 2. Exit Tab: Where users pay and leave
        ExitPanel exitPanel = new ExitPanel();
        tabbedPane.addTab("Vehicle Exit & Payment", null, exitPanel, "Process payments");

        // 3. Admin Tab: Strategy configuration and map view
        AdminPanel adminPanel = new AdminPanel();
        tabbedPane.addTab("Admin Panel", null, adminPanel, "View revenue, fines, and map");

        // 4. Reports Tab: Detailed statistical tables
        ReportPanel reportPanel = new ReportPanel();
        tabbedPane.addTab("Reports", null, reportPanel, "Detailed statistics and logs");

        // --- Auto-Refresh Logic ---
        // Makes the screens refresh automatically when user clicks a different tab
        tabbedPane.addChangeListener(e -> {
            Component selected = tabbedPane.getSelectedComponent();

            if (selected == adminPanel) {
                adminPanel.refreshCurrentView(); // Reloads map/revenue
            } else if (selected == reportPanel) {
                reportPanel.refreshCurrentTab(); // Reloads the active report table
            }
        });

        // Add the tabs to the window and make it visible
        add(tabbedPane);
        setVisible(true);
    }
}