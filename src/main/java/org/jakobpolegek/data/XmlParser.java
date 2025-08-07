package org.jakobpolegek.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class XmlParser {

    public static void parseAndStore() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        AtomicInteger ratesParsed = new AtomicInteger(0);

        try (Connection conn = DriverManager.getConnection("jdbc:h2:./rates_db;AUTO_SERVER=TRUE", "sa", "")) {
            conn.setAutoCommit(false);
            String sql = "MERGE INTO exchange_rates (rate_date, currency, rate) KEY(rate_date, currency) VALUES (?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                InputStream xmlInputStream = XmlParser.class.getClassLoader().getResourceAsStream("dtecbs-l.xml");
                if (xmlInputStream == null) {
                    System.err.println("FATAL ERROR: dtecbs-l.xml not found in src/main/resources folder.");
                    return;
                }

                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                dbFactory.setNamespaceAware(true);
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

                Document doc = dBuilder.parse(xmlInputStream);
                doc.getDocumentElement().normalize();

                NodeList tecajnicaNodes = doc.getElementsByTagNameNS("*", "tecajnica");

                for (int i = 0; i < tecajnicaNodes.getLength(); i++) {
                    Element dayElement = (Element) tecajnicaNodes.item(i);
                    LocalDate date = LocalDate.parse(dayElement.getAttribute("datum"), formatter);

                    NodeList rateNodes = dayElement.getElementsByTagNameNS("*", "tecaj");
                    for (int j = 0; j < rateNodes.getLength(); j++) {
                        Element rateElement = (Element) rateNodes.item(j);
                        String currency = rateElement.getAttribute("oznaka");
                        double rate = Double.parseDouble(rateElement.getTextContent());

                        pstmt.setDate(1, java.sql.Date.valueOf(date));
                        pstmt.setString(2, currency);
                        pstmt.setDouble(3, rate);
                        pstmt.addBatch();
                        ratesParsed.incrementAndGet();
                    }
                }
                pstmt.executeBatch();
                conn.commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
