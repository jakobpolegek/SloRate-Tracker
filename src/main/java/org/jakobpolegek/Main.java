package org.jakobpolegek;
import org.jakobpolegek.data.DataDownloader;
import org.jakobpolegek.data.DatabaseManager;
import org.jakobpolegek.data.SloRateTrackerApp;
import org.jakobpolegek.data.XmlParser;

import java.io.InputStream;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing SloRate Tracker...");
        System.out.println("Downloading latest exchange rate data...");
        Optional<InputStream> xmlStreamOptional = DataDownloader.downloadXmlFile();

        if (xmlStreamOptional.isEmpty()) {
            System.err.println("Could not download data. Please check your internet connection and try again.");
            System.err.println("Application will now exit...");
            return;
        }

        DatabaseManager.initializeDatabase();
        System.out.println("Loading downloaded data into the database...");
        XmlParser.parseAndStore(xmlStreamOptional.get());
        System.out.println("Data loaded successfully.");

        SloRateTrackerApp app = new SloRateTrackerApp();
        app.run();
    }
}
