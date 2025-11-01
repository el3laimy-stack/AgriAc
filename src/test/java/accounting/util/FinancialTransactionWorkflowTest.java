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

@DisplayName("Financial Transaction (Expense) Workflow Test")
public class FinancialTransactionWorkflowTest {

    @Test
    @DisplayName("Test deleting an expense and ensure balances are reversed")
    void testDeleteExpenseWorkflow() {
        Connection anchorConnection = null;
        ImprovedDataManager dataManager = null;
        try {
            // == Phase 0: Full Test Setup ==
            ImprovedDataManager.reinitializeForTest("jdbc:sqlite:file::memory:?cache=shared");
            dataManager = ImprovedDataManager.getInstance();
            anchorConnection = dataManager.getConnection(); // Anchor connection

            FinancialAccountDataService financialAccountDataService = new FinancialAccountDataService();
            FinancialTransactionDataService transactionDataService = new FinancialTransactionDataService();

            // 1. Get accounts
            List<FinancialAccount> accounts = financialAccountDataService.getAllAccounts();
            FinancialAccount expenseAccount = accounts.stream().filter(a -> a.getAccountName().equals("مصروفات عمومية وإدارية")).findFirst().orElseThrow();
            FinancialAccount cashAccount = accounts.stream().filter(a -> a.getAccountName().equals("الخزنة الرئيسية")).findFirst().orElseThrow();

            // 2. Add an expense
            String description = "صيانة المكتب";
            double amount = 350.0;
            String transactionRef = transactionDataService.addExpense(LocalDate.now(), amount, description, expenseAccount.getAccountId(), cashAccount.getAccountId());

            // == Phase 1: Get state before deletion ==
            System.out.println("PHASE 1: Getting state before deletion...");
            FinancialAccount currentExpenseAccount = financialAccountDataService.getAllAccounts().stream().filter(a -> a.getAccountId() == expenseAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount currentCashAccount = financialAccountDataService.getAllAccounts().stream().filter(a -> a.getAccountId() == cashAccount.getAccountId()).findFirst().orElseThrow();

            assertEquals(350.0, currentExpenseAccount.getCurrentBalance(), "Expense account balance should be 350 before deletion.");
            assertEquals(-350.0, currentCashAccount.getCurrentBalance(), "Cash account balance should be -350 before deletion.");

            // == Phase 2: ACT (Delete the Expense) ==
            System.out.println("\nPHASE 2: Deleting the expense...");
            boolean deleted = transactionDataService.deleteExpense(transactionRef);
            assertTrue(deleted, "deleteExpense should return true.");

            // == Phase 3: ASSERT (Verify Impacts) ==
            System.out.println("\nPHASE 3: Verifying impacts of the deletion...");
            FinancialAccount finalExpenseAccount = financialAccountDataService.getAllAccounts().stream().filter(a -> a.getAccountId() == expenseAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount finalCashAccount = financialAccountDataService.getAllAccounts().stream().filter(a -> a.getAccountId() == cashAccount.getAccountId()).findFirst().orElseThrow();

            assertEquals(0.0, finalExpenseAccount.getCurrentBalance(), "Expense account balance should be reversed to zero.");
            assertEquals(0.0, finalCashAccount.getCurrentBalance(), "Cash account balance should be reversed to zero.");
            System.out.println("SUCCESS: Financial balances are correct.");

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