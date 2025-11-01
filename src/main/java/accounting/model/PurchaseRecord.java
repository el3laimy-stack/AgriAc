package accounting.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * نموذج بيانات سجل الشراء المحسن
 * 
 * يمثل هذا الكلاس عملية شراء لمحصول زراعي من مورد، ويحتوي على جميع التفاصيل
 * المتعلقة بالعملية بما في ذلك الكمية، السعر، التكلفة الإجمالية، وغيرها.
 * 
 * يتم استخدام هذا الكلاس في عمليات:
 * - تسجيل مشتريات جديدة
 * - تتبع المخزون
 * - إعداد التقارير المالية
 * - إدارة العلاقات مع الموردين
 */
public class PurchaseRecord {
    /** المعرف الفريد لسجل الشراء */
    private int purchaseId;
    
    /** المحصول الذي تم شراؤه */
    private Crop crop;
    
    /** المورد الذي تم الشراء منه */
    private Contact supplier;
    
    /** الكمية المشتراة بالكيلوغرام */
    private double quantityKg;
    
    /** وحدة التسعير المستخدمة في الشراء */
    private String pricingUnit;
    
    /** عامل التحويل الخاص بوحدة التسعير */
    private double specificFactor;
    
    /** سعر الوحدة بالعملة المحلية */
    private double unitPrice;
    
    /** التكلفة الإجمالية للعملية */
    private double totalCost;
    
    /** تاريخ الشراء */
    private LocalDate purchaseDate;
    
    /** رقم الفاتورة */
    private String invoiceNumber;

    /** ملاحظات إضافية على الفاتورة */
    private String notes;

    private double amountPaid;
    private double balance;
    private String paymentStatus;

    // Constructors
    public PurchaseRecord() {}

    /**
     * مُنشئ كامل لسجل الشراء
     * 
     * @param purchaseId المعرف الفريد لسجل الشراء
     * @param crop المحصول الذي تم شراؤه
     * @param supplier المورد الذي تم الشراء منه
     * @param quantityKg الكمية المشتراة بالكيلوغرام
     * @param pricingUnit وحدة التسعير المستخدمة
     * @param specificFactor عامل التحويل الخاص بوحدة التسعير
     * @param unitPrice سعر الوحدة
     * @param totalCost التكلفة الإجمالية
     * @param purchaseDate تاريخ الشراء
     * @param invoiceNumber رقم الفاتورة
     */
    public PurchaseRecord(int purchaseId, Crop crop, Contact supplier, double quantityKg,
                         String pricingUnit, double specificFactor, double unitPrice,
                         double totalCost, LocalDate purchaseDate, String invoiceNumber) {
        this.purchaseId = purchaseId;
        this.crop = crop;
        this.supplier = supplier;
        this.quantityKg = quantityKg;
        this.pricingUnit = pricingUnit;
        this.specificFactor = specificFactor;
        this.unitPrice = unitPrice;
        this.totalCost = totalCost;
        this.purchaseDate = purchaseDate;
        this.invoiceNumber = invoiceNumber;
    }

    // Getters and Setters
    /**
     * يحصل على المعرف الفريد لسجل الشراء
     * @return المعرف الفريد
     */
    public int getPurchaseId() { return purchaseId; }
    
    /**
     * يحدد المعرف الفريد لسجل الشراء
     * @param purchaseId المعرف الفريد
     */
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    /**
     * يحصل على المحصول الذي تم شراؤه
     * @return المحصول
     */
    public Crop getCrop() { return crop; }
    
    /**
     * يحدد المحصول الذي تم شراؤه
     * @param crop المحصول
     */
    public void setCrop(Crop crop) { this.crop = crop; }

    /**
     * يحصل على المورد الذي تم الشراء منه
     * @return المورد
     */
    public Contact getSupplier() { return supplier; }
    
    /**
     * يحدد المورد الذي تم الشراء منه
     * @param supplier المورد
     */
    public void setSupplier(Contact supplier) { this.supplier = supplier; }

    /**
     * يحصل على الكمية المشتراة بالكيلوغرام
     * @return الكمية بالكيلوغرام
     */
    public double getQuantityKg() { return quantityKg; }
    
    /**
     * يحدد الكمية المشتراة بالكيلوغرام
     * @param quantityKg الكمية بالكيلوغرام
     */
    public void setQuantityKg(double quantityKg) { this.quantityKg = quantityKg; }

    /**
     * يحصل على وحدة التسعير المستخدمة في الشراء
     * @return وحدة التسعير
     */
    public String getPricingUnit() { return pricingUnit; }
    
    /**
     * يحدد وحدة التسعير المستخدمة في الشراء
     * @param pricingUnit وحدة التسعير
     */
    public void setPricingUnit(String pricingUnit) { this.pricingUnit = pricingUnit; }

    /**
     * يحصل على عامل التحويل الخاص بوحدة التسعير
     * @return عامل التحويل
     */
    public double getSpecificFactor() { return specificFactor; }
    
    /**
     * يحدد عامل التحويل الخاص بوحدة التسعير
     * @param specificFactor عامل التحويل
     */
    public void setSpecificFactor(double specificFactor) { this.specificFactor = specificFactor; }

    /**
     * يحصل على سعر الوحدة بالعملة المحلية
     * @return سعر الوحدة
     */
    public double getUnitPrice() { return unitPrice; }
    
    /**
     * يحدد سعر الوحدة بالعملة المحلية
     * @param unitPrice سعر الوحدة
     */
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    /**
     * يحصل على التكلفة الإجمالية للعملية
     * @return التكلفة الإجمالية
     */
    public double getTotalCost() { return totalCost; }
    
    /**
     * يحدد التكلفة الإجمالية للعملية
     * @param totalCost التكلفة الإجمالية
     */
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    /**
     * يحصل على تاريخ الشراء
     * @return تاريخ الشراء
     */
    public LocalDate getPurchaseDate() { return purchaseDate; }
    
    /**
     * يحدد تاريخ الشراء
     * @param purchaseDate تاريخ الشراء
     */
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    /**
     * يحصل على رقم الفاتورة
     * @return رقم الفاتورة
     */
    public String getInvoiceNumber() { return invoiceNumber; }
    
    /**
     * يحدد رقم الفاتورة
     * @param invoiceNumber رقم الفاتورة
     */
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    /**
     * يحصل على الملاحظات الإضافية على الفاتورة
     * @return الملاحظات
     */
    public String getNotes() { return notes; }

    /**
     * يحدد الملاحظات الإضافية على الفاتورة
     * @param notes الملاحظات
     */
    public void setNotes(String notes) { this.notes = notes; }

    /**
     * يحسب الكمية بوحدة التسعير
     * @return الكمية بوحدة التسعير
     */
    public double getQuantityInPricingUnit() {
        if (specificFactor > 0) {
            return quantityKg / specificFactor;
        }
        return quantityKg;
    }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PurchaseRecord that = (PurchaseRecord) obj;
        return purchaseId == that.purchaseId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(purchaseId);
    }
}

