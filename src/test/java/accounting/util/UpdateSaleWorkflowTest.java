package accounting.util;

import accounting.model.*;
import accounting.service.ContactDataService;
import accounting.service.CropDataService;
import accounting.service.FinancialAccountDataService;
import accounting.service.PurchaseDataService;
import accounting.service.SaleDataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Full End-to-End Update Sale Workflow Test")
public class UpdateSaleWorkflowTest {

    @Test
    @DisplayName("Test updating a sale and ensure all accounting entries are correctly modified")
    void testUpdateSaleWorkflow() {
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
            SaleDataService saleDataService = new SaleDataService();

            // 1. Setup entities and initial stock
            Crop crop = new Crop(0, "برتقال", List.of("صندوق"), Map.of("صندوق", List.of(15.0)));
            cropDataService.addCrop(crop);
            Crop createdCrop = cropDataService.getCropById(1);

            Contact supplier = new Contact(0, "مورد برتقال", "123-456-789", "المزرعة", true, false);
            contactDataService.addContact(supplier);
            Contact createdSupplier = contactDataService.getAllContacts().stream().filter(Contact::isSupplier).findFirst().orElseThrow();

            Contact customer = new Contact(0, "زبون برتقال", "987-654-321", "السوق", false, true);
            contactDataService.addContact(customer);
            Contact createdCustomer = contactDataService.getAllContacts().stream().filter(Contact::isCustomer).findFirst().orElseThrow();

            PurchaseRecord purchase = new PurchaseRecord();
            purchase.setCrop(createdCrop);
            purchase.setSupplier(createdSupplier);
            purchase.setPurchaseDate(LocalDate.now().minusDays(10));
            purchase.setQuantityKg(150.0); // 10 boxes
            purchase.setPricingUnit("صندوق");
            purchase.setSpecificFactor(15.0);
            purchase.setUnitPrice(30.0); // Cost = 300
            purchase.setTotalCost(3000.0);
            purchase.setInvoiceNumber("PUR-UPD-01");
            purchaseDataService.addPurchase(purchase, null, 0);

            // 2. Perform an initial sale (5 boxes)
            SaleRecord originalSale = new SaleRecord();
            originalSale.setCustomer(createdCustomer);
            originalSale.setCrop(createdCrop);
            originalSale.setSaleDate(LocalDate.now().minusDays(1));
            originalSale.setQuantitySoldKg(75.0); // 5 boxes
            originalSale.setSellingPricingUnit("صندوق");
            originalSale.setSpecificSellingFactor(15.0);
            originalSale.setSellingUnitPrice(50.0); // Total = 2500
            originalSale.setTotalSaleAmount(2500.0);
            originalSale.setSaleInvoiceNumber("SAL-UPD-01");
            int saleId = saleDataService.addSale(originalSale, null, 0); // On credit
            originalSale.setSaleId(saleId);

            // == Phase 1: Get state before the update ==
            System.out.println("PHASE 1: Getting state before update...");
            List<FinancialAccount> accounts = financialAccountDataService.getAllAccounts();
            FinancialAccount arAccount = accounts.stream().filter(a -> a.getAccountName().equals("الذمم المدينة (العملاء)")).findFirst().orElseThrow();
            double initialArBalance = arAccount.getCurrentBalance();

            // == Phase 2: ACT (Update the Sale to 8 boxes) ==
            System.out.println("\nPHASE 2: Updating the sale...");
            SaleRecord updatedSale = new SaleRecord();
            updatedSale.setSaleId(saleId);
            updatedSale.setCustomer(createdCustomer);
            updatedSale.setCrop(createdCrop);
            updatedSale.setSaleDate(LocalDate.now().minusDays(1));
            updatedSale.setQuantitySoldKg(120.0); // Changed from 5 to 8 boxes (75kg to 120kg)
            updatedSale.setSellingPricingUnit("صندوق");
            updatedSale.setSpecificSellingFactor(15.0);
            updatedSale.setSellingUnitPrice(50.0);
            updatedSale.setTotalSaleAmount(4000.0); // New total: 8 * 50 = 4000
            updatedSale.setSaleInvoiceNumber("SAL-UPD-01-MOD");

            saleDataService.updateSale(updatedSale, null, 0);

            // == Phase 3: ASSERT (Verify Impacts) ==
            System.out.println("\nPHASE 3: Verifying impacts of the update...");

            // 3.1 Verify Financial Balances
            FinancialAccount finalArAccount = financialAccountDataService.getAllAccounts().stream().filter(a -> a.getAccountId() == arAccount.getAccountId()).findFirst().orElseThrow();
            assertEquals(4000.0, finalArAccount.getCurrentBalance(), "A/R balance should reflect the new total sale amount.");
            System.out.println("SUCCESS: A/R balance is correct.");

            // 3.2 Verify Inventory
            CropDataService.CropStatistics finalStats = cropDataService.getCropStatistics(createdCrop.getCropId(), null, null);
            assertEquals(30.0, finalStats.getCurrentStock(), "Inventory stock should be 30kg (150 initial - 120 sold).");
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
