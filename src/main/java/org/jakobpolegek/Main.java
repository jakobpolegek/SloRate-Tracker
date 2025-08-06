package org.jakobpolegek;
import org.jakobpolegek.data.DatabaseManager;
import org.jakobpolegek.data.XmlParser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        System.out.println("Initializing SloRate Tracker...");
        DatabaseManager.initializeDatabase();
        System.out.println("Loading exchange rate data from XML...");
        XmlParser.parseAndStore();
        System.out.println("Data loaded successfully.");

        while (true) {
            printMenu();
            int choice = Integer.parseInt(scanner.nextLine());
            switch (choice) {
                case 1:
                    displayRatesTable();
                    break;
                case 2:
                    calculateOpportunityLoss();
                    break;
                case 3:
                    System.out.println("Exiting application...");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n--- Main Menu ---");
        System.out.println("1. Display Exchange Rates in Table");
        System.out.println("2. Calculate Opportunity Gain/Loss");
        System.out.println("3. Exit");
        System.out.print("Enter your choice: ");
    }

    private static void displayRatesTable() {
        System.out.print("Enter currencies (comma-separated, e.g., USD,CHF): ");
        String[] currencyInputs = scanner.nextLine().toUpperCase().split(",");
        List<String> currencies = Arrays.stream(currencyInputs).map(String::trim).collect(Collectors.toList());
        System.out.print("Enter start date (YYYY-MM-DD): ");
        LocalDate startDate = LocalDate.parse(scanner.nextLine(), formatter);
        System.out.print("Enter end date (YYYY-MM-DD): ");
        LocalDate endDate = LocalDate.parse(scanner.nextLine(), formatter);

        List<Map<String, Object>> rates = DatabaseManager.getRatesForPeriod(startDate, endDate, currencies);

        System.out.printf("\n%-12s", "Date");
        currencies.forEach(c -> System.out.printf("%-10s", c));
        System.out.println();
        System.out.println("-".repeat(12 + currencies.size() * 10));

        Map<LocalDate, Map<String, Object>> ratesByDate = new TreeMap<>();
        for (Map<String, Object> rate : rates) {
            ratesByDate.computeIfAbsent((LocalDate) rate.get("date"), k -> new HashMap<>())
                    .put((String) rate.get("currency"), rate.get("rate"));
        }

        ratesByDate.forEach((date, currencyRates) -> {
            System.out.printf("%-12s", date.format(formatter));
            currencies.forEach(c -> {
                Object rate = currencyRates.get(c);
                System.out.printf("%-10s", rate != null ? rate.toString() + " " : "N/A");
            });
            System.out.println();
        });
    }

    private static void calculateOpportunityLoss() {
        System.out.print("Enter base currency (e.g., USD): ");
        String baseCurrency = scanner.nextLine().toUpperCase();
        System.out.print("Enter quote currency (e.g., JPY): ");
        String quoteCurrency = scanner.nextLine().toUpperCase();
        System.out.print("Enter start date (YYYY-MM-DD): ");
        LocalDate startDate = LocalDate.parse(scanner.nextLine(), formatter);
        System.out.print("Enter end date (YYYY-MM-DD): ");
        LocalDate endDate = LocalDate.parse(scanner.nextLine(), formatter);
        System.out.print("Enter amount in " + baseCurrency + ": ");
        double amount = Double.parseDouble(scanner.nextLine());

        Optional<Double> baseStartRate = DatabaseManager.getRate(startDate, baseCurrency);
        Optional<Double> quoteStartRate = DatabaseManager.getRate(startDate, quoteCurrency);
        Optional<Double> baseEndRate = DatabaseManager.getRate(endDate, baseCurrency);
        Optional<Double> quoteEndRate = DatabaseManager.getRate(endDate, quoteCurrency);

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
