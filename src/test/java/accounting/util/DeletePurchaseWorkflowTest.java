package accounting.util;

import accounting.model.*;
import accounting.service.ContactDataService;
import accounting.service.CropDataService;
import accounting.service.FinancialAccountDataService;
import accounting.service.PurchaseDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Full End-to-End Delete Purchase Workflow Test")
public class DeletePurchaseWorkflowTest {

    @Test
    @DisplayName("Test deleting a purchase and ensure all accounting entries are reversed")
    void testDeletePurchaseWorkflow() {
        Connection anchorConnection = null;
        ImprovedDataManager dataManager = null;
        try {
            // == Phase 0: Full Test Setup ==
            ImprovedDataManager.reinitializeForTest("jdbc:sqlite:file::memory:?cache=shared");
            dataManager = ImprovedDataManager.getInstance();
            anchorConnection = dataManager.getConnection(); // Anchor connection

            CropDataService cropDataService = new CropDataService();
            ContactDataService contactDataService = new ContactDataService();
            FinancialAccountDataService financialAccountDataService = new FinancialAccountDataService();
            PurchaseDataService purchaseDataService = new PurchaseDataService();

            // 1. Create Crop and Supplier
            Crop crop = new Crop(0, "قمح", List.of("طن"), Map.of("طن", List.of(1000.0)));
            cropDataService.addCrop(crop);
            Crop createdCrop = cropDataService.getCropById(1);

            Contact supplier = new Contact(0, "مورد قمح", "555-555-555", "المزرعة الكبيرة", true, false);
            contactDataService.addContact(supplier);
            Contact createdSupplier = contactDataService.getAllContacts().stream().filter(Contact::isSupplier).findFirst().orElseThrow();

            // 2. Perform a purchase to be deleted
            PurchaseRecord purchase = new PurchaseRecord();
            purchase.setCrop(createdCrop);
            purchase.setSupplier(createdSupplier);
            purchase.setPurchaseDate(LocalDate.now().minusDays(3));
            purchase.setQuantityKg(1000.0); // 1 ton
            purchase.setPricingUnit("طن");
            purchase.setSpecificFactor(1000.0);
            purchase.setUnitPrice(1500.0); // 1500 per ton
            purchase.setTotalCost(1500.0);
            purchase.setInvoiceNumber("PUR-DEL-01");
            int purchaseId = purchaseDataService.addPurchase(purchase, null, 0); // On credit
            assertTrue(purchaseId > 0, "Purchase setup failed.");

            // == Phase 1: Get state before the deletion ==
            System.out.println("PHASE 1: Getting state before deletion...");

            List<FinancialAccount> accounts = financialAccountDataService.getAllAccounts();
            FinancialAccount apAccount = accounts.stream().filter(a -> a.getAccountName().equals("الذمم الدائنة (الموردين)")).findFirst().orElseThrow();
            FinancialAccount inventoryAccount = accounts.stream().filter(a -> a.getAccountName().equals("المخزون")).findFirst().orElseThrow();

            // Balances should reflect the purchase
            assertEquals(1500.0, apAccount.getCurrentBalance(), "A/P balance should be 1500 before deletion.");
            assertEquals(1500.0, inventoryAccount.getCurrentBalance(), "Inventory balance should be 1500 before deletion.");

            // == Phase 2: ACT (Delete the Purchase) ==
            System.out.println("\nPHASE 2: Deleting the purchase...");
            boolean deleted = purchaseDataService.deletePurchase(purchaseId);
            assertTrue(deleted, "deletePurchase method returned false.");
            System.out.println("Purchase with ID " + purchaseId + " was deleted.");

            // == Phase 3: ASSERT (Verify Impacts) ==
            System.out.println("\nPHASE 3: Verifying impacts of the deletion...");

            // 3.1: Verify Inventory Stock is zero
            CropDataService.CropStatistics finalStats = cropDataService.getCropStatistics(createdCrop.getCropId(), null, null);
            assertEquals(0, finalStats.getCurrentStock(), "Inventory stock should be zero after deletion.");
            System.out.println("SUCCESS: Inventory stock is correct.");

            // 3.2: Verify Financial Account Balances are zero
            List<FinancialAccount> finalAccounts = financialAccountDataService.getAllAccounts();
            FinancialAccount finalApAccount = finalAccounts.stream().filter(a -> a.getAccountId() == apAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount finalInventoryAccount = finalAccounts.stream().filter(a -> a.getAccountId() == inventoryAccount.getAccountId()).findFirst().orElseThrow();

            // Both balances should be reversed to their initial state (0)
            assertEquals(0.0, finalApAccount.getCurrentBalance(), "A/P balance should be reversed to zero.");
            assertEquals(0.0, finalInventoryAccount.getCurrentBalance(), "Inventory balance should be reversed to zero.");
            System.out.println("SUCCESS: Financial balances are correct.");

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
