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

@DisplayName("Full End-to-End Update Purchase Workflow Test")
public class UpdatePurchaseWorkflowTest {

    @Test
    @DisplayName("Test updating a purchase and ensure all accounting entries are correctly modified")
    void testUpdatePurchaseWorkflow() {
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

            // 1. Setup entities
            Crop crop = new Crop(0, "ذرة", List.of("كيس"), Map.of("كيس", List.of(25.0)));
            cropDataService.addCrop(crop);
            Crop createdCrop = cropDataService.getCropById(1);

            Contact supplier = new Contact(0, "مورد حبوب", "654-321-987", "الريف", true, false);
            contactDataService.addContact(supplier);
            Contact createdSupplier = contactDataService.getAllContacts().stream().filter(Contact::isSupplier).findFirst().orElseThrow();

            // 2. Perform an initial purchase (10 bags -> 250kg @ 50/bag = 5000 total)
            PurchaseRecord originalPurchase = new PurchaseRecord();
            originalPurchase.setCrop(createdCrop);
            originalPurchase.setSupplier(createdSupplier);
            originalPurchase.setPurchaseDate(LocalDate.now().minusDays(5));
            originalPurchase.setQuantityKg(250.0);
            originalPurchase.setPricingUnit("كيس");
            originalPurchase.setSpecificFactor(25.0);
            originalPurchase.setUnitPrice(200.0); // 5000 / 10 bags = 500 per bag? No, 5000 / 250kg = 20/kg. Let's use 200/bag
            originalPurchase.setTotalCost(5000.0);
            originalPurchase.setInvoiceNumber("PUR-UPD-101");
            int purchaseId = purchaseDataService.addPurchase(originalPurchase, null, 0);
            originalPurchase.setPurchaseId(purchaseId);

            // == Phase 1: Get state before the update ==
            System.out.println("PHASE 1: Getting state before update...");
            List<FinancialAccount> accounts = financialAccountDataService.getAllAccounts();
            FinancialAccount apAccount = accounts.stream().filter(a -> a.getAccountName().equals("الذمم الدائنة (الموردين)")).findFirst().orElseThrow();
            FinancialAccount inventoryAccount = accounts.stream().filter(a -> a.getAccountName().equals("المخزون")).findFirst().orElseThrow();

            assertEquals(5000.0, apAccount.getCurrentBalance(), "Pre-test A/P balance should be 5000.");
            assertEquals(5000.0, inventoryAccount.getCurrentBalance(), "Pre-test Inventory balance should be 5000.");

            // == Phase 2: ACT (Update the Purchase to 12 bags) ==
            System.out.println("\nPHASE 2: Updating the purchase...");
            PurchaseRecord updatedPurchase = new PurchaseRecord();
            updatedPurchase.setPurchaseId(purchaseId);
            updatedPurchase.setCrop(createdCrop);
            updatedPurchase.setSupplier(createdSupplier);
            updatedPurchase.setPurchaseDate(LocalDate.now().minusDays(4));
            updatedPurchase.setQuantityKg(300.0); // Changed from 10 to 12 bags (250kg to 300kg)
            updatedPurchase.setPricingUnit("كيس");
            updatedPurchase.setSpecificFactor(25.0);
            updatedPurchase.setUnitPrice(200.0);
            updatedPurchase.setTotalCost(6000.0); // New total: 12 bags * 200 = 6000
            updatedPurchase.setInvoiceNumber("PUR-UPD-101-MOD");

            purchaseDataService.updatePurchase(updatedPurchase);

            // == Phase 3: ASSERT (Verify Impacts) ==
            System.out.println("\nPHASE 3: Verifying impacts of the update...");

            // 3.1 Verify Financial Balances
            FinancialAccount finalApAccount = financialAccountDataService.getAllAccounts().stream().filter(a -> a.getAccountId() == apAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount finalInventoryAccount = financialAccountDataService.getAllAccounts().stream().filter(a -> a.getAccountId() == inventoryAccount.getAccountId()).findFirst().orElseThrow();

            assertEquals(6000.0, finalApAccount.getCurrentBalance(), "A/P balance should reflect the new total cost.");
            assertEquals(6000.0, finalInventoryAccount.getCurrentBalance(), "Inventory balance should reflect the new total cost.");
            System.out.println("SUCCESS: Financial balances are correct.");

            // 3.2 Verify Inventory Stock
            CropDataService.CropStatistics finalStats = cropDataService.getCropStatistics(createdCrop.getCropId(), null, null);
            assertEquals(300.0, finalStats.getCurrentStock(), "Inventory stock should be 300kg.");
            System.out.println("SUCCESS: Inventory stock is correct.");

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
