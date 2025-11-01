package accounting.model;

import java.time.LocalDate;

public class ContactStatementEntry {

    private LocalDate date;
    private String reason; // e.g., "صادر له بضاعة", "واصل منه نقدية"
    private String type;   // e.g., "ارز شعير", "قمح"
    private Double weight;
    private Double price;
    private String notes;
    private double amount;
    private boolean isDebit; // True if it increases customer debt
    private double balance;
    private String balanceDescription; // "الباقي عليه" or "الباقي له"

    // Constructors, Getters, and Setters

    public ContactStatementEntry(LocalDate date, String reason, String type, Double weight, Double price, String notes, double amount, boolean isDebit) {
        this.date = date;
        this.reason = reason;
        this.type = type;
        this.weight = weight;
        this.price = price;
        this.notes = notes;
        this.amount = amount;
        this.isDebit = isDebit;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public boolean isDebit() {
        return isDebit;
    }

    public void setDebit(boolean isDebit) {
        this.isDebit = isDebit;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getBalanceDescription() {
        return balanceDescription;
    }

    public void setBalanceDescription(String balanceDescription) {
        this.balanceDescription = balanceDescription;
    }
}