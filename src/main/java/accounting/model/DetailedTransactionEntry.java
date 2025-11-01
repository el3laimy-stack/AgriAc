package accounting.model;

import java.time.LocalDate;

/**
 * نموذج قيد معاملة مفصل مع تفاصيل العملية
 */
public class DetailedTransactionEntry {
    private LocalDate date;
    private String reference;
    private String type;
    private String mainDescription;
    
    // تفاصيل المعاملة الإضافية
    private String itemName; // اسم الصنف/المحصول
    private Double quantity; // الكمية
    private String unit; // الوحدة
    private Double unitPrice; // سعر الوحدة
    private Double totalAmount; // المبلغ الإجمالي
    private Double paidAmount; // المبلغ المدفوع
    private Double remainingAmount; // المبلغ المتبقي
    
    // الأعمدة المحاسبية
    private double debit;
    private double credit;
    private double balance;
    
    // معلومات إضافية
    private String contactName; // اسم العميل/المورد
    private String paymentMethod; // طريقة الدفع
    private String notes; // ملاحظات

    // Constructor للمعاملات المبسطة
    public DetailedTransactionEntry(LocalDate date, String reference, String type, 
                                  String mainDescription, double debit, double credit, double balance) {
        this.date = date;
        this.reference = reference;
        this.type = type;
        this.mainDescription = mainDescription;
        this.debit = debit;
        this.credit = credit;
        this.balance = balance;
    }

    // Constructor للمعاملات المفصلة
    public DetailedTransactionEntry(LocalDate date, String reference, String type, 
                                  String mainDescription, String itemName, Double quantity, 
                                  String unit, Double unitPrice, Double totalAmount,
                                  Double paidAmount, Double remainingAmount,
                                  double debit, double credit, double balance,
                                  String contactName, String paymentMethod, String notes) {
        this(date, reference, type, mainDescription, debit, credit, balance);
        this.itemName = itemName;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.remainingAmount = remainingAmount;
        this.contactName = contactName;
        this.paymentMethod = paymentMethod;
        this.notes = notes;
    }

    // Factory method لإنشاء معاملة بيع مفصلة
    public static DetailedTransactionEntry createSaleEntry(LocalDate date, String invoiceNumber,
                                                         String cropName, double quantity, String unit,
                                                         double unitPrice, double totalAmount,
                                                         double paidAmount, String customerName) {
        double remaining = totalAmount - paidAmount;
        String description = String.format("بيع %s - %.0f %s بسعر %.2f للوحدة", 
                                         cropName, quantity, unit, unitPrice);
        
        return new DetailedTransactionEntry(
            date, invoiceNumber, "SALE", description,
            cropName, quantity, unit, unitPrice, totalAmount,
            paidAmount, remaining, totalAmount, 0.0, totalAmount,
            customerName, paidAmount > 0 ? "نقدي جزئي" : "آجل", 
            remaining > 0 ? "الباقي عنده " + String.format("%.2f", remaining) : "مسدد بالكامل"
        );
    }

    // Factory method لإنشاء معاملة دفع مفصلة
    public static DetailedTransactionEntry createPaymentEntry(LocalDate date, String paymentRef,
                                                            double amount, String customerName,
                                                            String paymentMethod, double newBalance) {
        String description = String.format("دفع من %s - %s", customerName, paymentMethod);
        
        return new DetailedTransactionEntry(
            date, paymentRef, "PAYMENT", description,
            null, null, null, null, amount,
            amount, 0.0, 0.0, amount, newBalance,
            customerName, paymentMethod, "دفعة نقدية"
        );
    }

    // Getters and Setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMainDescription() { return mainDescription; }
    public void setMainDescription(String mainDescription) { this.mainDescription = mainDescription; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public Double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Double paidAmount) { this.paidAmount = paidAmount; }

    public Double getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Double remainingAmount) { this.remainingAmount = remainingAmount; }

    public double getDebit() { return debit; }
    public void setDebit(double debit) { this.debit = debit; }

    public double getCredit() { return credit; }
    public void setCredit(double credit) { this.credit = credit; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // Helper methods
    public boolean isSale() { return "SALE".equals(type); }
    public boolean isPurchase() { return "PURCHASE".equals(type); }
    public boolean isPayment() { return "PAYMENT".equals(type) || "RECEIVE".equals(type); }
    
    public boolean hasItemDetails() { 
        return itemName != null && quantity != null && unitPrice != null; 
    }
    
    public boolean hasRemainingBalance() { 
        return remainingAmount != null && remainingAmount > 0; 
    }

    /**
     * عرض النص التفصيلي للمعاملة
     */
    public String getDetailedDescription() {
        if (hasItemDetails()) {
            return String.format("%s: %s - %.0f %s × %.2f = %.2f", 
                               getTypeInArabic(), itemName, quantity, unit != null ? unit : "وحدة", 
                               unitPrice, totalAmount);
        }
        return mainDescription;
    }

    /**
     * ترجمة نوع المعاملة إلى العربية
     */
    public String getTypeInArabic() {
        switch (type) {
            case "SALE": return "مبيع";
            case "PURCHASE": return "شراء";
            case "PAYMENT": return "دفع";
            case "RECEIVE": return "قبض";
            default: return type;
        }
    }

    /**
     * الحصول على معلومات الدفع المفصلة
     */
    public String getPaymentInfo() {
        if (totalAmount != null && paidAmount != null) {
            if (remainingAmount != null && remainingAmount > 0) {
                return String.format("دفع %.2f من %.2f - الباقي %.2f", 
                                   paidAmount, totalAmount, remainingAmount);
            } else {
                return String.format("مسدد بالكامل %.2f", paidAmount);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s - %s: %s", date, reference, getDetailedDescription());
    }
}