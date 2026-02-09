package src.main;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import src.ui.MainApp; // Import the UI window you built

public class Main {
    public static void main(String[] args) {
        // 1. Make the app look like a native Windows/Mac app (instead of old Java style)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Launch the application safely
        SwingUtilities.invokeLater(() -> {
            new MainApp(); // This opens the window we created earlier
        });
    }
}