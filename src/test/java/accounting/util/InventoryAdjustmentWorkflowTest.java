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

@DisplayName("Full End-to-End Inventory Adjustment Workflow Test")
public class InventoryAdjustmentWorkflowTest {

    @Test
    @DisplayName("Test a DAMAGED inventory adjustment and check balances")
    void testDamageAdjustmentWorkflow() {
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
            Crop crop = new Crop(0, "أرز", List.of("كيلو"), Map.of("كيلو", List.of(1.0)));
            cropDataService.addCrop(crop);
            Crop createdCrop = cropDataService.getCropById(1);

            Contact supplier = new Contact(0, "مورد أرز", "444-444-444", "المخزن الكبير", true, false);
            contactDataService.addContact(supplier);
            Contact createdSupplier = contactDataService.getAllContacts().stream().filter(Contact::isSupplier).findFirst().orElseThrow();

            // 2. Purchase stock (500 kg @ 30/kg -> Cost = 15000)
            PurchaseRecord purchase = new PurchaseRecord();
            purchase.setCrop(createdCrop);
            purchase.setSupplier(createdSupplier);
            purchase.setPurchaseDate(LocalDate.now().minusDays(10));
            purchase.setQuantityKg(500.0);
            purchase.setPricingUnit("كيلو");
            purchase.setSpecificFactor(1.0);
            purchase.setUnitPrice(30.0);
            purchase.setTotalCost(15000.0);
            purchase.setInvoiceNumber("PUR-404");
            purchaseDataService.addPurchase(purchase, null, 0); // On credit

            // == Phase 1: Get state before the adjustment ==
            System.out.println("PHASE 1: Getting state before adjustment...");

            List<FinancialAccount> accounts = financialAccountDataService.getAllAccounts();
            FinancialAccount inventoryAccount = accounts.stream().filter(a -> a.getAccountName().equals("المخزون")).findFirst().orElseThrow();
            FinancialAccount inventoryLossAccount = accounts.stream().filter(a -> a.getAccountName().equals("خسائر المخزون")).findFirst().orElseThrow();

            double initialInventoryBalance = inventoryAccount.getCurrentBalance();
            double initialLossBalance = inventoryLossAccount.getCurrentBalance();

            // We are adjusting for 20kg of damage. Cost = 20kg * 30/kg = 600.
            double costOfDamagedGoods = 600.0;

            // == Phase 2: ACT (Perform Inventory Adjustment) ==
            System.out.println("\nPHASE 2: Performing a DAMAGE adjustment...");
            InventoryAdjustment adjustment = new InventoryAdjustment();
            adjustment.setCrop(createdCrop);
            adjustment.setAdjustmentDate(LocalDate.now());
            adjustment.setAdjustmentType(InventoryAdjustment.AdjustmentType.DAMAGE);
            adjustment.setQuantityKg(20.0);
            adjustment.setReason("تلف بسبب الرطوبة");

            int adjustmentId = cropDataService.addInventoryAdjustment(adjustment);
            assertTrue(adjustmentId > 0, "Adjustment was not created.");
            System.out.println("Adjustment recorded with ID: " + adjustmentId);

            // == Phase 3: ASSERT (Verify Impacts) ==
            System.out.println("\nPHASE 3: Verifying impacts of the adjustment...");

            // 3.1: Verify Financial Account Balances
            List<FinancialAccount> finalAccounts = financialAccountDataService.getAllAccounts();
            FinancialAccount finalInventoryAccount = finalAccounts.stream().filter(a -> a.getAccountId() == inventoryAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount finalLossAccount = finalAccounts.stream().filter(a -> a.getAccountId() == inventoryLossAccount.getAccountId()).findFirst().orElseThrow();

            // Inventory value should decrease by the cost of damaged goods
            assertEquals(initialInventoryBalance - costOfDamagedGoods, finalInventoryAccount.getCurrentBalance(), "Inventory balance should decrease by the cost of damage.");
            System.out.println("SUCCESS: Inventory balance is correct.");

            // Inventory Loss account should increase by the cost of damaged goods
            assertEquals(initialLossBalance + costOfDamagedGoods, finalLossAccount.getCurrentBalance(), "Inventory Loss balance should increase by the cost of damage.");
            System.out.println("SUCCESS: Inventory Loss balance is correct.");

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