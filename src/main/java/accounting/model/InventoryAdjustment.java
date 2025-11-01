// src/main/java/accounting/model/InventoryAdjustment.java
package accounting.model;

import java.time.LocalDate;

public class InventoryAdjustment {
    
    private int adjustmentId;
    private Crop crop;
    private LocalDate adjustmentDate;
    private AdjustmentType adjustmentType;
    private double quantityKg;
    private String reason;
    private double cost;

    public enum AdjustmentType {
        DAMAGE("تالف"),
        SHORTAGE("عجز"),
        SURPLUS("زيادة");

        private final String arabicName;
        AdjustmentType(String name) { this.arabicName = name; }
        public String getArabicName() { return arabicName; }
        @Override public String toString() { return arabicName; }
    }

    // Constructors, Getters, and Setters
    public InventoryAdjustment() {}

    public int getAdjustmentId() { return adjustmentId; }
    public void setAdjustmentId(int id) { this.adjustmentId = id; }
    
    public Crop getCrop() { return crop; }
    public void setCrop(Crop crop) { this.crop = crop; }

    public LocalDate getAdjustmentDate() { return adjustmentDate; }
    public void setAdjustmentDate(LocalDate date) { this.adjustmentDate = date; }

    public AdjustmentType getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(AdjustmentType type) { this.adjustmentType = type; }

    public double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(double quantity) { this.quantityKg = quantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
}