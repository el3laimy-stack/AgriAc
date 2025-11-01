package accounting.model;

import java.time.LocalDate;

public class SaleReturn {
    private int returnId;
    private SaleRecord originalSale;
    private LocalDate returnDate;
    private double quantityKg;
    private String returnReason;
    private double refundAmount; // المبلغ المسترد للعميل

    // Constructors, Getters, and Setters
    public SaleReturn() {}

    public int getReturnId() { return returnId; }
    public void setReturnId(int returnId) { this.returnId = returnId; }

    public SaleRecord getOriginalSale() { return originalSale; }
    public void setOriginalSale(SaleRecord originalSale) { this.originalSale = originalSale; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(double quantityKg) { this.quantityKg = quantityKg; }

    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }

    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double refundAmount) { this.refundAmount = refundAmount; }
}