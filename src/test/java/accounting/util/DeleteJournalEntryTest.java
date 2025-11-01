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

@DisplayName("End-to-End Journal Entry (Manual Transaction) Workflow Test")
public class DeleteJournalEntryTest {

    @Test
    @DisplayName("Test deleting a manual journal entry and ensure balances are reversed")
    void testDeleteManualJournalEntry() {
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

            // Ensure initial balances are zero
            assertEquals(0.0, cashAccount.getCurrentBalance());
            assertEquals(0.0, expenseAccount.getCurrentBalance());

            // 2. Add a manual journal entry (e.g., paying for office supplies)
            String description = "شراء أدوات مكتبية";
            double amount = 150.0;
            String transactionRef = transactionService.addJournalEntry(expenseAccount, cashAccount, LocalDate.now(), description, amount);


            // == Phase 1: Get state before deletion ==
            System.out.println("PHASE 1: Getting state before deletion...");
            FinancialAccount cashAccountAfterAdd = accountService.getAllAccounts().stream().filter(a -> a.getAccountId() == cashAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount expenseAccountAfterAdd = accountService.getAllAccounts().stream().filter(a -> a.getAccountId() == expenseAccount.getAccountId()).findFirst().orElseThrow();

            assertEquals(-150.0, cashAccountAfterAdd.getCurrentBalance(), "Cash balance should be -150.");
            assertEquals(150.0, expenseAccountAfterAdd.getCurrentBalance(), "Expense balance should be 150.");

            // == Phase 2: ACT (Delete the Journal Entry) ==
            System.out.println("\nPHASE 2: Deleting the manual journal entry...");
            boolean deleted = transactionService.deleteJournalEntry(transactionRef);
            assertTrue(deleted, "deleteJournalEntryByRef should return true.");

            // == Phase 3: ASSERT (Verify Impacts) ==
            System.out.println("\nPHASE 3: Verifying impacts of the deletion...");
            FinancialAccount finalCashAccount = accountService.getAllAccounts().stream().filter(a -> a.getAccountId() == cashAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount finalExpenseAccount = accountService.getAllAccounts().stream().filter(a -> a.getAccountId() == expenseAccount.getAccountId()).findFirst().orElseThrow();

            assertEquals(0.0, finalCashAccount.getCurrentBalance(), "Cash balance should be reversed to zero.");
            assertEquals(0.0, finalExpenseAccount.getCurrentBalance(), "Expense balance should be reversed to zero.");

            System.out.println("\nVerification complete.");

        } catch (SQLException e) {
            fail("Test failed due to SQL exception: " + e.getMessage());
        } finally {
            if (anchorConnection != null) {
                try {
                    anchorConnection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (dataManager != null) {
                dataManager.shutdown();
            }
        }
    }
}
