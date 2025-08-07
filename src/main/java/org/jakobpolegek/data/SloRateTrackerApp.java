package org.jakobpolegek.data;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SloRateTrackerApp {
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void run() {
        while (true) {
            printMenu();
            try {
                int choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 1:
                        displayRatesTable();
                        break;
                    case 2:
                        calculateOpportunityLoss();
                        break;
                    case 3:
                        System.out.println("Exiting application.");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Display Exchange Rates in Table");
        System.out.println("2. Calculate Opportunity Gain/Loss");
        System.out.println("3. Exit");
        System.out.print("Enter your choice: ");
    }

    private void displayChart(Map<LocalDate, Map<String, Object>> ratesByDate, List<String> currencies, LocalDate startDate, LocalDate endDate) {
        String title = String.format("Exchange Rates vs. EUR (%s to %s)", startDate, endDate);
        XYChart chart = new XYChartBuilder().width(800).height(600).title(title).xAxisTitle("Date").yAxisTitle("Rate").build();

        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setDatePattern("yyyy-MM-dd");
        chart.getStyler().setToolTipsEnabled(true);

        List<LocalDate> sortedDates = ratesByDate.keySet().stream().sorted().collect(Collectors.toList());
        List<java.util.Date> xData = sortedDates.stream()
                .map(d -> java.sql.Date.valueOf(d))
                .collect(Collectors.toList());

        for (String currency : currencies) {
            List<Double> yData = sortedDates.stream()
                    .map(date -> {
                        Object rate = ratesByDate.get(date).get(currency);
                        return (rate instanceof Number) ? ((Number) rate).doubleValue() : null;
                    })
                    .collect(Collectors.toList());
            chart.addSeries(currency, xData, yData);
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("SloRate Tracker - Chart");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel chartPanel = new XChartPanel<>(chart);
            frame.add(chartPanel, BorderLayout.CENTER);

            JPanel controlPanel = new JPanel();
            controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
            controlPanel.setBorder(BorderFactory.createTitledBorder("Tick:"));

            for (String currency : currencies) {
                JCheckBox checkBox = new JCheckBox(currency, true);
                checkBox.addActionListener(e -> {
                    boolean isSelected = checkBox.isSelected();
                    chart.getSeriesMap().get(currency).setEnabled(isSelected);
                    chartPanel.revalidate();
                    chartPanel.repaint();
                });
                controlPanel.add(checkBox);
            }

            frame.add(controlPanel, BorderLayout.EAST);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }


    private void displayRatesTable() {
        System.out.print("Enter currencies (comma-separated, e.g., USD,CHF): ");
        String[] currencyInputs = scanner.nextLine().toUpperCase().split(",");
        List<String> currencies = Arrays.stream(currencyInputs).map(String::trim).collect(Collectors.toList());
        System.out.print("Enter start date (YYYY-MM-DD): ");
        LocalDate startDate = LocalDate.parse(scanner.nextLine(), formatter);
        System.out.print("Enter end date (YYYY-MM-DD): ");
        LocalDate endDate = LocalDate.parse(scanner.nextLine(), formatter);

        List<Map<String, Object>> rates = DatabaseManager.getRatesForPeriod(startDate, endDate, currencies);

        System.out.printf("\n%-12s", "Date");
        currencies.forEach(c -> System.out.printf("%-15s", c));
        System.out.println();
        System.out.println("-".repeat(12 + currencies.size() * 15));

        Map<LocalDate, Map<String, Object>> ratesByDate = new TreeMap<>();
        for (Map<String, Object> rate : rates) {
            ratesByDate.computeIfAbsent((LocalDate) rate.get("date"), k -> new HashMap<>())
                    .put((String) rate.get("currency"), rate.get("rate"));
        }

        ratesByDate.forEach((date, currencyRates) -> {
            System.out.printf("%-12s", date.format(formatter));
            currencies.forEach(c -> {
                Object rate = currencyRates.get(c);
                if (rate != null) {
                    System.out.printf("%-15.4f", ((Number) rate).doubleValue());
                } else {
                    System.out.printf("%-15s", "N/A");
                }
            });
            System.out.println();
        });

        if (!rates.isEmpty()) {
            System.out.println("\nLaunching chart in a new window...");
            displayChart(ratesByDate, currencies, startDate, endDate);
        } else {
            System.out.println("\nNo data available to display a chart.");
        }
    }

    private void calculateOpportunityLoss() {
        System.out.print("Enter base currency (e.g., USD): ");
        String baseCurrency = scanner.nextLine().toUpperCase().trim();
        System.out.print("Enter quote currency (e.g., JPY): ");
        String quoteCurrency = scanner.nextLine().toUpperCase().trim();
        System.out.print("Enter start date (YYYY-MM-DD): ");
        LocalDate startDate = LocalDate.parse(scanner.nextLine(), formatter);
        System.out.print("Enter end date (YYYY-MM-DD): ");
        LocalDate endDate = LocalDate.parse(scanner.nextLine(), formatter);
        System.out.print("Enter amount in " + baseCurrency + ": ");
        double amount = Double.parseDouble(scanner.nextLine());

        Optional<Double> baseStartRate = DatabaseManager.getMostRecentRate(startDate, baseCurrency);
        Optional<Double> quoteStartRate = DatabaseManager.getMostRecentRate(startDate, quoteCurrency);
        Optional<Double> baseEndRate = DatabaseManager.getMostRecentRate(endDate, baseCurrency);
        Optional<Double> quoteEndRate = DatabaseManager.getMostRecentRate(endDate, quoteCurrency);

        if (baseStartRate.isEmpty() || quoteStartRate.isEmpty() || baseEndRate.isEmpty() || quoteEndRate.isEmpty()) {
            System.out.println("Could not find rates for one or more currencies on the specified dates.");
            return;
        }

        double eurAmount = amount / baseStartRate.get();
        double quoteAmount = eurAmount * quoteStartRate.get();

        double finalEurAmount = quoteAmount / quoteEndRate.get();
        double finalBaseAmount = finalEurAmount * baseEndRate.get();

        double gainLoss = finalBaseAmount - amount;

        System.out.printf("\n--- Opportunity Gain/Loss Calculation ---\n");
        System.out.printf("Initial Investment: %.2f %s\n", amount, baseCurrency);
        System.out.printf("Value at End Date: %.2f %s\n", finalBaseAmount, baseCurrency);
        System.out.printf("Opportunity Gain/Loss: %.2f %s\n", gainLoss, baseCurrency);
    }
}