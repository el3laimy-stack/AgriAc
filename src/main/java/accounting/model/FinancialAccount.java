package accounting.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * نموذج بيانات الحساب المالي المحسن
 */
public class FinancialAccount {
    private int accountId;
    private String accountName;
    private AccountType accountType;
    private double openingBalance;
    private LocalDate openingBalanceDate;
    
    // ---==[[ الخطوة 1: إضافة الحقل الجديد ]] ==---
    private double currentBalance;

    public enum AccountType {
        // الأصول
        CASH("نقدي"),
        BANK("بنكي"),
        CURRENT_ASSET("أصل متداول"),
        ACCOUNTS_RECEIVABLE("ذمم مدينة"),

        // الخصوم
        CURRENT_LIABILITY("خصم متداول"),
        ACCOUNTS_PAYABLE("ذمم دائنة"),

        // حقوق الملكية
        EQUITY("حقوق ملكية"),

        // الإيرادات والمصروفات
        REVENUE("إيرادات"),
        EXPENSE("مصروفات"),
        
        // لتنظيم الشجرة
        HEADER("حساب رئيسي");

        private final String arabicName;

        AccountType(String arabicName) {
            this.arabicName = arabicName;
        }

        public String getArabicName() {
            return arabicName;
        }

        @Override
        public String toString() {
            return arabicName;
        }
    }

    // Constructors
    public FinancialAccount() {}

    public FinancialAccount(int accountId, String accountName, AccountType accountType,
                           double openingBalance, LocalDate openingBalanceDate) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountType = accountType;
        this.openingBalance = openingBalance;
        this.openingBalanceDate = openingBalanceDate;
        // ---==[[ الخطوة 2: تهيئة الرصيد الحالي بالرصيد الافتتاحي ]] ==---
        this.currentBalance = openingBalance; 
    }

    // Getters and Setters
    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

    public LocalDate getOpeningBalanceDate() { return openingBalanceDate; }
    public void setOpeningBalanceDate(LocalDate openingBalanceDate) { this.openingBalanceDate = openingBalanceDate; }

    // ---==[[ الخطوة 3: إضافة Getter و Setter للحقل الجديد ]] ==---
    public double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }


    @Override
    public String toString() {
        return accountName != null ? accountName : "حساب غير محدد";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FinancialAccount that = (FinancialAccount) obj;
        return accountId == that.accountId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId);
    }
}