package accounting.model;

import java.time.LocalDate;

public class PurchaseReturn {
    private int returnId;
    private PurchaseRecord originalPurchase;
    private LocalDate returnDate;
    private double quantityKg;
    private String returnReason;
    private double returnedCost;

    // Constructors, Getters, and Setters
    public PurchaseReturn() {}

    public int getReturnId() { return returnId; }
    public void setReturnId(int returnId) { this.returnId = returnId; }

    public PurchaseRecord getOriginalPurchase() { return originalPurchase; }
    
    // *** السطر التالي هو الذي تم تصحيحه ***
    public void setOriginalPurchase(PurchaseRecord originalPurchase) { this.originalPurchase = originalPurchase; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(double quantityKg) { this.quantityKg = quantityKg; }

    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }

    public double getReturnedCost() { return returnedCost; }
    public void setReturnedCost(double returnedCost) { this.returnedCost = returnedCost; }
}