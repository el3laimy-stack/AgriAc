package accounting.util;

import accounting.model.FinancialAccount;
import accounting.service.FinancialAccountDataService;
import accounting.service.FinancialTransactionDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Expense Workflow Test")
public class ExpenseWorkflowTest {

    @Test
    @DisplayName("Test creating an expense and check balances")
    void testCreateExpense() {
        Connection anchorConnection = null;
        ImprovedDataManager dataManager = null;
        try {
            // == Phase 0: Full Test Setup ==
            ImprovedDataManager.reinitializeForTest("jdbc:sqlite:file::memory:?cache=shared");
            dataManager = ImprovedDataManager.getInstance();
            anchorConnection = dataManager.getConnection(); // Anchor connection

            FinancialAccountDataService accountService = new FinancialAccountDataService();
            FinancialTransactionDataService transactionService = new FinancialTransactionDataService();

            // 1. Get accounts for the transaction
            List<FinancialAccount> accounts = accountService.getAllAccounts();
            FinancialAccount cashAccount = accounts.stream().filter(a -> a.getAccountName().equals("الخزنة الرئيسية")).findFirst().orElseThrow();
            FinancialAccount expenseAccount = accounts.stream().filter(a -> a.getAccountName().equals("مصروفات عمومية وإدارية")).findFirst().orElseThrow();

            // 2. Add an expense
            String description = "شراء أدوات مكتبية";
            double amount = 150.0;
            transactionService.addExpense(LocalDate.now(), amount, description, expenseAccount.getAccountId(), cashAccount.getAccountId());

            // 3. Verify state after adding expense
            FinancialAccount finalCashAccount = accountService.getAccountById(cashAccount.getAccountId());
            FinancialAccount finalExpenseAccount = accountService.getAccountById(expenseAccount.getAccountId());

            assertEquals(-150.0, finalCashAccount.getCurrentBalance(), "Cash balance should be -150.");
            assertEquals(150.0, finalExpenseAccount.getCurrentBalance(), "Expense balance should be 150.");

            System.out.println("\nVerification complete.");

        } catch (SQLException e) {
            fail("Test failed due to SQL exception: " + e.getMessage());
        } finally {
            if (anchorConnection != null) {
                try { anchorConnection.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (dataManager != null) {
                dataManager.shutdown();
            }
        }
    }
}
