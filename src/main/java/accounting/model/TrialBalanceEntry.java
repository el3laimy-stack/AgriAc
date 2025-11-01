package accounting.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class TrialBalanceEntry {
    private final SimpleIntegerProperty accountId;
    private final SimpleStringProperty accountName;
    private final SimpleDoubleProperty totalDebit;
    private final SimpleDoubleProperty totalCredit;
    private final SimpleDoubleProperty finalBalance;

    public TrialBalanceEntry(int accountId, String accountName, double totalDebit, double totalCredit) {
        this.accountId = new SimpleIntegerProperty(accountId);
        this.accountName = new SimpleStringProperty(accountName);
        this.totalDebit = new SimpleDoubleProperty(totalDebit);
        this.totalCredit = new SimpleDoubleProperty(totalCredit);
        this.finalBalance = new SimpleDoubleProperty(totalDebit - totalCredit);
    }

    // Getters
    public int getAccountId() { return accountId.get(); }
    public String getAccountName() { return accountName.get(); }
    public double getTotalDebit() { return totalDebit.get(); }
    public double getTotalCredit() { return totalCredit.get(); }
    public double getFinalBalance() { return finalBalance.get(); }
}