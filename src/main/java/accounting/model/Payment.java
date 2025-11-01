package accounting.model;

import java.time.LocalDate;

public class Payment {
    private int paymentId;
    private LocalDate paymentDate;
    private Contact contact;
    private FinancialAccount paymentAccount;
    private double amount;
    private String paymentType;
    private String description;

    // Getters and Setters

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public FinancialAccount getPaymentAccount() {
        return paymentAccount;
    }

    public void setPaymentAccount(FinancialAccount paymentAccount) {
        this.paymentAccount = paymentAccount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
