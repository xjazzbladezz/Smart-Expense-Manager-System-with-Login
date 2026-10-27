package com.smartexpense;

import com.smartexpense.db.DBConnection;
import com.smartexpense.db.DatabaseInitializer;
import com.smartexpense.ui.LoginFrame;
import com.smartexpense.ui.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Entry point. Run with:  mvn exec:java   or   java -jar target/SmartExpenseManager.jar */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            try {
                DatabaseInitializer.init();
            } catch (Exception ex) {
                Throwable root = ex;
                while (root.getCause() != null) root = root.getCause();
                JOptionPane.showMessageDialog(null,
                        "Could not connect to MySQL at " + DBConnection.describe() + "\n\n"
                                + root.getMessage() + "\n\n"
                                + "1. Make sure MySQL is running.\n"
                                + "2. Edit src/main/resources/db.properties (user / password).\n"
                                + "3. Run the app again.",
                        "Database connection failed", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }
            new LoginFrame().setVisible(true);
        });
    }
}
