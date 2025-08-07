package org.jakobpolegek;
import org.jakobpolegek.data.DatabaseManager;
import org.jakobpolegek.data.SloRateTrackerApp;
import org.jakobpolegek.data.XmlParser;

public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing SloRate Tracker...");
        DatabaseManager.initializeDatabase();
        System.out.println("Loading exchange rate data from XML...");
        XmlParser.parseAndStore();
        System.out.println("Data loaded successfully.");

        SloRateTrackerApp app = new SloRateTrackerApp();
        app.run();
    }
}
