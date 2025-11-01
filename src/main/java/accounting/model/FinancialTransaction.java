package accounting.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDate;
import java.util.Objects;

/**
 * نموذج بيانات الحركة المالية المحسن (مُحدَّث لاستخدام خصائص JavaFX)
 */
public class FinancialTransaction {
    private final IntegerProperty transactionId;
    private final ObjectProperty<LocalDate> transactionDate;
    private final ObjectProperty<FinancialAccount> account;
    private final StringProperty transactionType;
    private final StringProperty description;
    private final DoubleProperty amount;
    private final ObjectProperty<Contact> relatedContact;
    private final ObjectProperty<PurchaseRecord> relatedPurchase;
    private final ObjectProperty<SaleRecord> relatedSale;
    private final StringProperty notes;

    // Constructors
    public FinancialTransaction() {
        this.transactionId = new SimpleIntegerProperty(0);
        this.transactionDate = new SimpleObjectProperty<>(null);
        this.account = new SimpleObjectProperty<>(null);
        this.transactionType = new SimpleStringProperty(null);
        this.description = new SimpleStringProperty(null);
        this.amount = new SimpleDoubleProperty(0.0);
        this.relatedContact = new SimpleObjectProperty<>(null);
        this.relatedPurchase = new SimpleObjectProperty<>(null);
        this.relatedSale = new SimpleObjectProperty<>(null);
        this.notes = new SimpleStringProperty(null);
    }

    public FinancialTransaction(int transactionId, LocalDate transactionDate,
                               FinancialAccount account, String transactionType,
                               String description, double amount, Contact relatedContact,
                               PurchaseRecord relatedPurchase, SaleRecord relatedSale,
                               String notes) {
        this.transactionId = new SimpleIntegerProperty(transactionId);
        this.transactionDate = new SimpleObjectProperty<>(transactionDate);
        this.account = new SimpleObjectProperty<>(account);
        this.transactionType = new SimpleStringProperty(transactionType);
        this.description = new SimpleStringProperty(description);
        this.amount = new SimpleDoubleProperty(amount);
        this.relatedContact = new SimpleObjectProperty<>(relatedContact);
        this.relatedPurchase = new SimpleObjectProperty<>(relatedPurchase);
        this.relatedSale = new SimpleObjectProperty<>(relatedSale);
        this.notes = new SimpleStringProperty(notes);
    }

    // --- Getters and Setters for values ---
    public int getTransactionId() { return transactionId.get(); }
    public void setTransactionId(int transactionId) { this.transactionId.set(transactionId); }
    public IntegerProperty transactionIdProperty() { return transactionId; }

    public LocalDate getTransactionDate() { return transactionDate.get(); }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate.set(transactionDate); }
    public ObjectProperty<LocalDate> transactionDateProperty() { return transactionDate; }

    public FinancialAccount getAccount() { return account.get(); }
    public void setAccount(FinancialAccount account) { this.account.set(account); }
    public ObjectProperty<FinancialAccount> accountProperty() { return account; }

    public String getTransactionType() { return transactionType.get(); }
    public void setTransactionType(String transactionType) { this.transactionType.set(transactionType); }
    public StringProperty transactionTypeProperty() { return transactionType; }

    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    public StringProperty descriptionProperty() { return description; }

    public double getAmount() { return amount.get(); }
    public void setAmount(double amount) { this.amount.set(amount); }
    public DoubleProperty amountProperty() { return amount; }

    public Contact getRelatedContact() { return relatedContact.get(); }
    public void setRelatedContact(Contact relatedContact) { this.relatedContact.set(relatedContact); }
    public ObjectProperty<Contact> relatedContactProperty() { return relatedContact; }

    public PurchaseRecord getRelatedPurchase() { return relatedPurchase.get(); }
    public void setRelatedPurchase(PurchaseRecord relatedPurchase) { this.relatedPurchase.set(relatedPurchase); }
    public ObjectProperty<PurchaseRecord> relatedPurchaseProperty() { return relatedPurchase; }

    public SaleRecord getRelatedSale() { return relatedSale.get(); }
    public void setRelatedSale(SaleRecord relatedSale) { this.relatedSale.set(relatedSale); }
    public ObjectProperty<SaleRecord> relatedSaleProperty() { return relatedSale; }

    public String getNotes() { return notes.get(); }
    public void setNotes(String notes) { this.notes.set(notes); }
    public StringProperty notesProperty() { return notes; }

    /**
     * يحدد ما إذا كانت الحركة إيراد أم مصروف
     * @return true إذا كانت إيراد، false إذا كانت مصروف
     */
    public boolean isIncome() {
        return getAmount() > 0;
    }

    /**
     * يحصل على القيمة المطلقة للمبلغ
     * @return القيمة المطلقة للمبلغ
     */
    public double getAbsoluteAmount() {
        return Math.abs(getAmount());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FinancialTransaction that = (FinancialTransaction) obj;
        return getTransactionId() == that.getTransactionId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTransactionId());
    }
}