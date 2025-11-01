package accounting.model;

import java.util.Map;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

public class BalanceSheet {

    private final Map<String, Double> assets;
    private final Map<String, Double> liabilities;
    private final Map<String, Double> equity;
    private final double retainedEarnings; // Net income for the period

    private final double totalAssets;
    private final double totalLiabilities;
    private final double totalEquity;
    private final double totalLiabilitiesAndEquity;

    // Additional detailed data
    private final ObservableList<PieChart.Data> inventoryBreakdown;
    private final ObservableList<String> topDebtors;
    private final ObservableList<String> topCreditors;

    public BalanceSheet(Map<String, Double> assets, Map<String, Double> liabilities, Map<String, Double> equity, double retainedEarnings,
                          ObservableList<PieChart.Data> inventoryBreakdown,
                          ObservableList<String> topDebtors,
                          ObservableList<String> topCreditors) {
        this.assets = assets;
        this.liabilities = liabilities;
        this.equity = equity;
        this.retainedEarnings = retainedEarnings;
        this.inventoryBreakdown = inventoryBreakdown;
        this.topDebtors = topDebtors;
        this.topCreditors = topCreditors;

        this.totalAssets = assets.values().stream().mapToDouble(Double::doubleValue).sum();
        this.totalLiabilities = liabilities.values().stream().mapToDouble(Double::doubleValue).sum();
        this.totalEquity = equity.values().stream().mapToDouble(Double::doubleValue).sum();
        this.totalLiabilitiesAndEquity = this.totalLiabilities + this.totalEquity + this.retainedEarnings;
    }

    // Getters
    public Map<String, Double> getAssets() { return assets; }
    public Map<String, Double> getLiabilities() { return liabilities; }
    public Map<String, Double> getEquity() { return equity; }
    public double getRetainedEarnings() { return retainedEarnings; }
    public double getTotalAssets() { return totalAssets; }
    public double getTotalLiabilities() { return totalLiabilities; }
    public double getTotalEquity() { return totalEquity; }
    public double getTotalLiabilitiesAndEquity() { return totalLiabilitiesAndEquity; }
    public ObservableList<PieChart.Data> getInventoryBreakdown() { return inventoryBreakdown; }
    public ObservableList<String> getTopDebtors() { return topDebtors; }
    public ObservableList<String> getTopCreditors() { return topCreditors; }

    /**
     * Inner class to represent an item in the balance sheet for TableView binding.
     */
    public static class BalanceItem {
        private final SimpleStringProperty name;
        private final SimpleDoubleProperty value;

        public BalanceItem(String name, Double value) {
            this.name = new SimpleStringProperty(name);
            this.value = new SimpleDoubleProperty(value);
        }

        public String getName() {
            return name.get();
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public double getValue() {
            return value.get();
        }

        public SimpleDoubleProperty valueProperty() {
            return value;
        }
    }
}