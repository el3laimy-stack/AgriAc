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

@DisplayName("Manual Journal Entry Workflow Test")
public class ManualTransactionWorkflowTest {

    @Test
    @DisplayName("Test creating and then deleting a manual journal entry")
    void testCreateAndDeleteManualJournalEntry() {
        Connection anchorConnection = null;
        ImprovedDataManager dataManager = null;
        try {
            // == Phase 0: Full Test Setup ==
            ImprovedDataManager.reinitializeForTest("jdbc:sqlite:file::memory:?cache=shared");
            dataManager = ImprovedDataManager.getInstance();
            anchorConnection = dataManager.getConnection();

            FinancialAccountDataService accountService = new FinancialAccountDataService();
            FinancialTransactionDataService transactionService = new FinancialTransactionDataService();

            // 1. Get accounts
            FinancialAccount cashAccount = accountService.getAllAccounts().stream().filter(a -> a.getAccountName().equals("الخزنة الرئيسية")).findFirst().orElseThrow();
            FinancialAccount expenseAccount = accountService.getAllAccounts().stream().filter(a -> a.getAccountName().equals("مصروفات عمومية وإدارية")).findFirst().orElseThrow();

            // 2. Add a manual journal entry
            String description = "شراء مستلزمات مكتبية";
            double amount = 250.0;
            String transactionRef = transactionService.addJournalEntry(expenseAccount, cashAccount, LocalDate.now(), description, amount);


            // 3. Verify state before deletion
            FinancialAccount cashAfterAdd = accountService.getAccountById(cashAccount.getAccountId());
            FinancialAccount expenseAfterAdd = accountService.getAccountById(expenseAccount.getAccountId());
            assertEquals(-250.0, cashAfterAdd.getCurrentBalance(), "Cash balance should be -250 after add.");
            assertEquals(250.0, expenseAfterAdd.getCurrentBalance(), "Expense balance should be 250 after add.");

            // == Phase 2: ACT (Delete the Journal Entry) ==
            System.out.println("\nPHASE 2: Deleting the manual journal entry...");
            boolean deleted = transactionService.deleteJournalEntry(transactionRef);
            assertTrue(deleted, "deleteJournalEntryByRef should return true.");

            // == Phase 3: ASSERT (Verify Impacts) ==
            System.out.println("\nPHASE 3: Verifying impacts of the deletion...");
            FinancialAccount finalCashAccount = accountService.getAccountById(cashAccount.getAccountId());
            FinancialAccount finalExpenseAccount = accountService.getAccountById(expenseAccount.getAccountId());

            assertEquals(0.0, finalCashAccount.getCurrentBalance(), "Cash balance should be reversed to zero.");
            assertEquals(0.0, finalExpenseAccount.getCurrentBalance(), "Expense balance should be reversed to zero.");

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