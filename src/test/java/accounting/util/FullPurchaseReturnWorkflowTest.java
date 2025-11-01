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

@DisplayName("Full End-to-End Purchase Return Workflow Test")
public class FullPurchaseReturnWorkflowTest {

    @Test
    @DisplayName("Test a full purchase return workflow and check all balances")
    void testFullPurchaseReturnWorkflow() {
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
            Crop crop = new Crop(0, "بطاطس", List.of("شوال"), Map.of("شوال", List.of(50.0)));
            cropDataService.addCrop(crop);
            Crop createdCrop = cropDataService.getCropById(1);

            Contact supplier = new Contact(0, "مورد بطاطس", "333-333-333", "الحقل", true, false);
            contactDataService.addContact(supplier);
            Contact createdSupplier = contactDataService.getAllContacts().stream().filter(Contact::isSupplier).findFirst().orElseThrow();

            // 2. Purchase stock (20 sacks = 1000 kg @ 100/sack -> Cost = 2000)
            PurchaseRecord originalPurchase = new PurchaseRecord();
            originalPurchase.setCrop(createdCrop);
            originalPurchase.setSupplier(createdSupplier);
            originalPurchase.setPurchaseDate(LocalDate.now().minusDays(5));
            originalPurchase.setQuantityKg(1000.0);
            originalPurchase.setPricingUnit("شوال");
            originalPurchase.setSpecificFactor(50.0);
            originalPurchase.setUnitPrice(100.0);
            originalPurchase.setTotalCost(2000.0);
            originalPurchase.setInvoiceNumber("PUR-303");
            int purchaseId = purchaseDataService.addPurchase(originalPurchase, null, 0); // On credit
            originalPurchase.setPurchaseId(purchaseId);

            // == Phase 1: Get state before the return ==
            System.out.println("PHASE 1: Getting state before return...");

            List<FinancialAccount> accounts = financialAccountDataService.getAllAccounts();
            FinancialAccount apAccount = accounts.stream().filter(a -> a.getAccountName().equals("الذمم الدائنة (الموردين)")).findFirst().orElseThrow();
            FinancialAccount inventoryAccount = accounts.stream().filter(a -> a.getAccountName().equals("المخزون")).findFirst().orElseThrow();

            double initialApBalance = apAccount.getCurrentBalance();
            double initialInventoryBalance = inventoryAccount.getCurrentBalance();

            CropDataService.CropStatistics initialStats = cropDataService.getCropStatistics(createdCrop.getCropId(), null, null);
            double initialStock = initialStats.getCurrentStock();

            // We are returning 5 sacks (250 kg). Cost of return = 250kg * (2000/1000)/kg = 500.
            double returnedCost = 500.0;

            // == Phase 2: ACT (Perform Purchase Return) ==
            System.out.println("\nPHASE 2: Performing a purchase return...");
            PurchaseReturn purchaseReturn = new PurchaseReturn();
            purchaseReturn.setOriginalPurchase(originalPurchase);
            purchaseReturn.setReturnDate(LocalDate.now());
            purchaseReturn.setQuantityKg(250.0); // Return 5 sacks
            purchaseReturn.setReturnedCost(returnedCost);
            purchaseReturn.setReturnReason("جودة منخفضة");

            int returnId = purchaseDataService.addPurchaseReturn(purchaseReturn);
            assertTrue(returnId > 0, "Purchase return was not created.");
            System.out.println("Purchase return recorded with ID: " + returnId);

            // == Phase 3: ASSERT (Verify Impacts) ==
            System.out.println("\nPHASE 3: Verifying impacts of the return...");

            // 3.1: Verify Inventory Stock
            CropDataService.CropStatistics finalStats = cropDataService.getCropStatistics(createdCrop.getCropId(), null, null);
            assertEquals(initialStock - 250.0, finalStats.getCurrentStock(), "Inventory stock should decrease by 250 kg.");
            System.out.println("SUCCESS: Inventory stock is correct.");

            // 3.2: Verify Financial Account Balances
            List<FinancialAccount> finalAccounts = financialAccountDataService.getAllAccounts();
            FinancialAccount finalApAccount = finalAccounts.stream().filter(a -> a.getAccountId() == apAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount finalInventoryAccount = finalAccounts.stream().filter(a -> a.getAccountId() == inventoryAccount.getAccountId()).findFirst().orElseThrow();

            // A/P should decrease by the returned cost
            assertEquals(initialApBalance - returnedCost, finalApAccount.getCurrentBalance(), "A/P balance should decrease by the returned cost.");
            System.out.println("SUCCESS: Accounts Payable balance is correct.");

            // Inventory value should decrease by the returned cost
            assertEquals(initialInventoryBalance - returnedCost, finalInventoryAccount.getCurrentBalance(), "Inventory balance should decrease by the returned cost.");
            System.out.println("SUCCESS: Inventory balance is correct.");

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
