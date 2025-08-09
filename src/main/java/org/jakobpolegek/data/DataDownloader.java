package org.jakobpolegek.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

public class DataDownloader {

    private static final String XML_URL = "http://www.bsi.si/_data/tecajnice/dtecbs-l.xml";
    private static final int MAX_REDIRECTS = 5;

    public static Optional<InputStream> downloadXmlFile() {
        try {
            String currentUrl = XML_URL;
            for (int i = 0; i < MAX_REDIRECTS; i++) {
                URL url = new URL(currentUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/xml");
                connection.setInstanceFollowRedirects(false);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    String newUrl = connection.getHeaderField("Location");
                    System.out.println("Redirect detected to: " + newUrl);
                    currentUrl = newUrl;
                    continue;
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    System.out.println("Successfully connected and downloaded data from: " + currentUrl);
                    return Optional.of(connection.getInputStream());
                }

                System.err.println("Failed to download file. Server responded with HTTP code: " + responseCode);
                return Optional.empty();
            }

            System.err.println("Too many redirects. Aborting download.");
            return Optional.empty();
        } catch (IOException e) {
            System.err.println("An error occurred during download: " + e.getMessage());
            return Optional.empty();
        }
    }
}
