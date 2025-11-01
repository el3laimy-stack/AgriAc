package accounting.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * نموذج بيانات سجل البيع المحسن
 * 
 * يمثل هذا الكلاس عملية بيع لمحصول زراعي لعميل، ويحتوي على جميع التفاصيل
 * المتعلقة بالعملية بما في ذلك الكمية، السعر، المبلغ الإجمالي، وغيرها.
 * 
 * يتم استخدام هذا الكلاس في عمليات:
 * - تسجيل مبيعات جديدة
 * - تتبع المخزون
 * - إعداد التقارير المالية
 * - إدارة العلاقات مع العملاء
 */
public class SaleRecord {
    /** المعرف الفريد لسجل البيع */
    private int saleId;
    
    /** العميل الذي تم البيع له */
    private Contact customer;
    
    /** المحصول الذي تم بيعه */
    private Crop crop;
    
    /** الكمية المباعة بالكيلوغرام */
    private double quantitySoldKg;
    
    /** وحدة التسعير المستخدمة في البيع */
    private String sellingPricingUnit;
    
    /** عامل التحويل الخاص بوحدة التسعير */
    private double specificSellingFactor;
    
    /** سعر الوحدة بالعملة المحلية */
    private double sellingUnitPrice;
    
    /** المبلغ الإجمالي للعملية */
    private double totalSaleAmount;
    
    /** تاريخ البيع */
    private LocalDate saleDate;
    
    /** رقم فاتورة البيع */
    private String saleInvoiceNumber;
    
    /** ملاحظات إضافية حول العملية */
    private String notes;

    private double amountPaid;
    private double balance;
    private String paymentStatus;

    // Constructors
    /**
     * مُنشئ افتراضي لسجل البيع
     */
    public SaleRecord() {
        this.notes = "";
    }

    /**
     * مُنشئ كامل لسجل البيع
     * 
     * @param saleId المعرف الفريد لسجل البيع
     * @param customer العميل الذي تم البيع له
     * @param crop المحصول الذي تم بيعه
     * @param quantitySoldKg الكمية المباعة بالكيلوغرام
     * @param sellingPricingUnit وحدة التسعير المستخدمة
     * @param specificSellingFactor عامل التحويل الخاص بوحدة التسعير
     * @param sellingUnitPrice سعر الوحدة
     * @param totalSaleAmount المبلغ الإجمالي
     * @param saleDate تاريخ البيع
     * @param saleInvoiceNumber رقم فاتورة البيع
     * @param notes ملاحظات إضافية
     */
    public SaleRecord(int saleId, Contact customer, Crop crop, double quantitySoldKg,
                     String sellingPricingUnit, double specificSellingFactor,
                     double sellingUnitPrice, double totalSaleAmount,
                     LocalDate saleDate, String saleInvoiceNumber, String notes) {
        this.saleId = saleId;
        this.customer = customer;
        this.crop = crop;
        this.quantitySoldKg = quantitySoldKg;
        this.sellingPricingUnit = sellingPricingUnit;
        this.specificSellingFactor = specificSellingFactor;
        this.sellingUnitPrice = sellingUnitPrice;
        this.totalSaleAmount = totalSaleAmount;
        this.saleDate = saleDate;
        this.saleInvoiceNumber = saleInvoiceNumber;
        this.notes = notes;
    }

    // Getters and Setters
    /**
     * يحصل على المعرف الفريد لسجل البيع
     * @return المعرف الفريد
     */
    public int getSaleId() { return saleId; }
    
    /**
     * يحدد المعرف الفريد لسجل البيع
     * @param saleId المعرف الفريد
     */
    public void setSaleId(int saleId) { this.saleId = saleId; }

    /**
     * يحصل على العميل الذي تم البيع له
     * @return العميل
     */
    public Contact getCustomer() { return customer; }
    
    /**
     * يحدد العميل الذي تم البيع له
     * @param customer العميل
     */
    public void setCustomer(Contact customer) { this.customer = customer; }

    /**
     * يحصل على المحصول الذي تم بيعه
     * @return المحصول
     */
    public Crop getCrop() { return crop; }
    
    /**
     * يحدد المحصول الذي تم بيعه
     * @param crop المحصول
     */
    public void setCrop(Crop crop) { this.crop = crop; }

    /**
     * يحصل على الكمية المباعة بالكيلوغرام
     * @return الكمية بالكيلوغرام
     */
    public double getQuantitySoldKg() { return quantitySoldKg; }
    
    /**
     * يحدد الكمية المباعة بالكيلوغرام
     * @param quantitySoldKg الكمية بالكيلوغرام
     */
    public void setQuantitySoldKg(double quantitySoldKg) { this.quantitySoldKg = quantitySoldKg; }

    /**
     * يحصل على وحدة التسعير المستخدمة في البيع
     * @return وحدة التسعير
     */
    public String getSellingPricingUnit() { return sellingPricingUnit; }
    
    /**
     * يحدد وحدة التسعير المستخدمة في البيع
     * @param sellingPricingUnit وحدة التسعير
     */
    public void setSellingPricingUnit(String sellingPricingUnit) { this.sellingPricingUnit = sellingPricingUnit; }

    /**
     * يحصل على عامل التحويل الخاص بوحدة التسعير
     * @return عامل التحويل
     */
    public double getSpecificSellingFactor() { return specificSellingFactor; }
    
    /**
     * يحدد عامل التحويل الخاص بوحدة التسعير
     * @param specificSellingFactor عامل التحويل
     */
    public void setSpecificSellingFactor(double specificSellingFactor) { this.specificSellingFactor = specificSellingFactor; }

    /**
     * يحصل على سعر الوحدة بالعملة المحلية
     * @return سعر الوحدة
     */
    public double getSellingUnitPrice() { return sellingUnitPrice; }
    
    /**
     * يحدد سعر الوحدة بالعملة المحلية
     * @param sellingUnitPrice سعر الوحدة
     */
    public void setSellingUnitPrice(double sellingUnitPrice) { this.sellingUnitPrice = sellingUnitPrice; }

    /**
     * يحصل على المبلغ الإجمالي للعملية
     * @return المبلغ الإجمالي
     */
    public double getTotalSaleAmount() { return totalSaleAmount; }
    
    /**
     * يحدد المبلغ الإجمالي للعملية
     * @param totalSaleAmount المبلغ الإجمالي
     */
    public void setTotalSaleAmount(double totalSaleAmount) { this.totalSaleAmount = totalSaleAmount; }

    /**
     * يحصل على تاريخ البيع
     * @return تاريخ البيع
     */
    public LocalDate getSaleDate() { return saleDate; }
    
    /**
     * يحدد تاريخ البيع
     * @param saleDate تاريخ البيع
     */
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

    /**
     * يحصل على رقم فاتورة البيع
     * @return رقم فاتورة البيع
     */
    public String getSaleInvoiceNumber() { return saleInvoiceNumber; }
    
    /**
     * يحدد رقم فاتورة البيع
     * @param saleInvoiceNumber رقم فاتورة البيع
     */
    public void setSaleInvoiceNumber(String saleInvoiceNumber) { this.saleInvoiceNumber = saleInvoiceNumber; }

    /**
     * يحصل على ملاحظات إضافية حول العملية
     * @return ملاحظات إضافية
     */
    public String getNotes() { return notes; }
    
    /**
     * يحدد ملاحظات إضافية حول العملية
     * @param notes ملاحظات إضافية
     */
    public void setNotes(String notes) { this.notes = notes; }

    /**
     * يحسب الكمية بوحدة التسعير
     * @return الكمية بوحدة التسعير
     */
    public double getQuantityInSellingUnit() {
        if (specificSellingFactor > 0) {
            return quantitySoldKg / specificSellingFactor;
        }
        return quantitySoldKg;
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
        SaleRecord that = (SaleRecord) obj;
        return saleId == that.saleId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(saleId);
    }
}

