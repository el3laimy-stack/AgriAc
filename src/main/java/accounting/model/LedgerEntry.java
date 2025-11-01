package accounting.model;

import java.time.LocalDate;

/**
 * يمثل سطراً واحداً في كشف حساب (لعميل أو مورد).
 * هذا الكلاس يساعد على توحيد عرض أنواع مختلفة من الحركات (بيع، شراء، دفعات) في جدول واحد.
 */
public class LedgerEntry {

    private final LocalDate date;
    private final String description;
    private final String reference; // رقم الفاتورة أو الإيصال
    private final double debit;     // مدين (مبلغ له)
    private final double credit;    // دائن (مبلغ عليه)
    private double balance;   // الرصيد بعد الحركة
    private final String accountType; // To identify the type of account (e.g., CASH, BANK, REVENUE)
    private final String sourceType; // e.g., "PURCHASE", "SALE", "MANUAL"
    private final int sourceId;      // The ID of the original record (e.g., purchase_id)

    // Additional details for a more descriptive journal
    private final String contactName;
    private final String itemName;
    private final Double quantity;
    private final Double unitPrice;
    private int entryId;
    private int accountId;


    public LedgerEntry(int entryId, String transactionRef, LocalDate entryDate, int accountId, double debit, double credit, String description, String sourceType, int sourceId) {
        this.entryId = entryId;
        this.reference = transactionRef;
        this.date = entryDate;
        this.accountId = accountId;
        this.debit = debit;
        this.credit = credit;
        this.description = description;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.balance = 0;
        this.accountType = null;
        this.contactName = null;
        this.itemName = null;
        this.quantity = null;
        this.unitPrice = null;
    }

    public LedgerEntry(LocalDate date, String description, String reference, double debit, double credit, 
                       String accountType, String sourceType, int sourceId,
                       String contactName, String itemName, Double quantity, Double unitPrice) {
        this.date = date;
        this.description = description;
        this.reference = reference;
        this.debit = debit;
        this.credit = credit;
        this.balance = 0; // Will be calculated later
        this.accountType = accountType;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.contactName = contactName;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // Getters
    public int getEntryId() {
        return entryId;
    }

    public int getAccountId() {
        return accountId;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public double getDebit() {
        return debit;
    }

    public double getCredit() {
        return credit;
    }

    public double getBalance() {
        return balance;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public int getSourceId() {
        return sourceId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getItemName() {
        return itemName;
    }

    public Double getQuantity() {
        return quantity;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    // Setter for the running balance
    public void setBalance(double balance) {
        this.balance = balance;
    }
}
