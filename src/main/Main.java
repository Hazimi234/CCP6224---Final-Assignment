package src.main;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import src.ui.MainApp;

public class Main {
    public static void main(String[] args) {
        // Makes the app look like a native Windows/Mac app (instead of old Java style)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Launch the application safely
        SwingUtilities.invokeLater(() -> {
            new MainApp(); // Instantiates the main window defined in MainApp.java
        });
    }
}