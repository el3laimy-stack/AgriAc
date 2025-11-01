package accounting.util;

import accounting.model.*;
import accounting.service.ContactDataService;
import accounting.service.FinancialAccountDataService;
import accounting.service.PaymentDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Payment Workflow Test")
public class PaymentWorkflowTest {

    @Test
    @DisplayName("Test creating and then deleting a payment")
    void testCreateAndDeletePayment() {
        Connection anchorConnection = null;
        ImprovedDataManager dataManager = null;
        try {
            // == Phase 0: Full Test Setup ==
            ImprovedDataManager.reinitializeForTest("jdbc:sqlite:file::memory:?cache=shared");
            dataManager = ImprovedDataManager.getInstance();
            anchorConnection = dataManager.getConnection(); // Anchor connection

            FinancialAccountDataService accountService = new FinancialAccountDataService();
            ContactDataService contactService = new ContactDataService();
            PaymentDataService paymentService = new PaymentDataService();

            // 1. Get accounts and contact
            Contact supplier = new Contact(0, "مورد عام", "111-222-333", "السوق", true, false);
            contactService.addContact(supplier);
            Contact createdSupplier = contactService.getAllContacts().get(0);

            FinancialAccount cashAccount = accountService.getAllAccounts().stream().filter(a -> a.getAccountName().equals("الخزنة الرئيسية")).findFirst().orElseThrow();
            FinancialAccount apAccount = accountService.getAllAccounts().stream().filter(a -> a.getAccountName().equals("الذمم الدائنة (الموردين)")).findFirst().orElseThrow();

            // 2. Add a payment
            Payment payment = new Payment();
            payment.setContact(createdSupplier);
            payment.setPaymentAccount(cashAccount);
            payment.setAmount(500.0);
            payment.setPaymentDate(LocalDate.now());
            payment.setPaymentType("PAY");
            payment.setDescription("دفعة تحت الحساب");
            int paymentId = paymentService.addPayment(payment);
            payment.setPaymentId(paymentId);

            // 3. Verify state before deletion
            FinancialAccount cashAfterAdd = accountService.getAccountById(cashAccount.getAccountId());
            FinancialAccount apAfterAdd = accountService.getAccountById(apAccount.getAccountId());
            assertEquals(-500.0, cashAfterAdd.getCurrentBalance());
            assertEquals(-500.0, apAfterAdd.getCurrentBalance());

            // == Phase 2: ACT (Delete the Payment) ==
            System.out.println("\nPHASE 2: Deleting the payment...");
            boolean deleted = paymentService.deletePayment(payment.getPaymentId());
            assertTrue(deleted, "deletePayment should return true.");

            // == Phase 3: ASSERT (Verify Impacts) ==
            System.out.println("\nPHASE 3: Verifying impacts of the deletion...");
            FinancialAccount finalCashAccount = accountService.getAccountById(cashAccount.getAccountId());
            FinancialAccount finalApAccount = accountService.getAccountById(apAccount.getAccountId());

            assertEquals(0.0, finalCashAccount.getCurrentBalance(), "Cash balance should be reversed to zero.");
            assertEquals(0.0, finalApAccount.getCurrentBalance(), "A/P balance should be reversed to zero.");

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

    @Test
    @DisplayName("Test updating a payment")
    void testUpdatePayment() {
        Connection anchorConnection = null;
        ImprovedDataManager dataManager = null;
        try {
            // == Phase 0: Full Test Setup ==
            ImprovedDataManager.reinitializeForTest("jdbc:sqlite:file::memory:?cache=shared");
            dataManager = ImprovedDataManager.getInstance();
            anchorConnection = dataManager.getConnection(); // Anchor connection

            FinancialAccountDataService accountService = new FinancialAccountDataService();
            ContactDataService contactService = new ContactDataService();
            PaymentDataService paymentService = new PaymentDataService();

            // 1. Get accounts and contact
            Contact supplier = new Contact(0, "مورد عام", "111-222-333", "السوق", true, false);
            contactService.addContact(supplier);
            Contact createdSupplier = contactService.getAllContacts().get(0);

            FinancialAccount cashAccount = accountService.getAllAccounts().stream().filter(a -> a.getAccountName().equals("الخزنة الرئيسية")).findFirst().orElseThrow();
            FinancialAccount apAccount = accountService.getAllAccounts().stream().filter(a -> a.getAccountName().equals("الذمم الدائنة (الموردين)")).findFirst().orElseThrow();

            // 2. Add an initial payment
            Payment originalPayment = new Payment();
            originalPayment.setContact(createdSupplier);
            originalPayment.setPaymentAccount(cashAccount);
            originalPayment.setAmount(500.0);
            originalPayment.setPaymentDate(LocalDate.now());
            originalPayment.setPaymentType("PAY");
            originalPayment.setDescription("دفعة أولى");
            int paymentId = paymentService.addPayment(originalPayment);
            originalPayment.setPaymentId(paymentId);

            // == Phase 1: Get state before update ==
            FinancialAccount apAfterAdd = accountService.getAccountById(apAccount.getAccountId());
            assertEquals(-500.0, apAfterAdd.getCurrentBalance());

            // == Phase 2: ACT (Update the Payment) ==
            System.out.println("\nPHASE 2: Updating the payment...");
            Payment updatedPayment = new Payment();
            updatedPayment.setPaymentId(paymentId);
            updatedPayment.setContact(createdSupplier);
            updatedPayment.setPaymentAccount(cashAccount);
            updatedPayment.setAmount(700.0); // Changed amount
            updatedPayment.setPaymentDate(LocalDate.now());
            updatedPayment.setPaymentType("PAY");
            updatedPayment.setDescription("دفعة معدلة");

            paymentService.updatePayment(updatedPayment);

            // == Phase 3: ASSERT (Verify Impacts) ==
            System.out.println("\nPHASE 3: Verifying impacts of the update...");
            FinancialAccount finalApAccount = accountService.getAccountById(apAccount.getAccountId());
            assertEquals(-700.0, finalApAccount.getCurrentBalance(), "A/P balance should reflect the new payment amount.");

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
