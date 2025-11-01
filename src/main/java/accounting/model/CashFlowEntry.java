package accounting.model;

import java.time.LocalDate;

public class CashFlowEntry {

    private final LocalDate date;
    private final String description;
    private final String type;
    private final double inflow;
    private final double outflow;
    private double balance;

    public CashFlowEntry(LocalDate date, String description, String type, double inflow, double outflow) {
        this.date = date;
        this.description = description;
        this.type = type;
        this.inflow = inflow;
        this.outflow = outflow;
    }

    // Getters
    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public double getInflow() {
        return inflow;
    }

    public double getOutflow() {
        return outflow;
    }

    public double getBalance() {
        return balance;
    }

    // Setter
    public void setBalance(double balance) {
        this.balance = balance;
    }
}
