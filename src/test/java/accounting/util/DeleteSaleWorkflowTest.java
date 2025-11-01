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

@DisplayName("Full End-to-End Delete Sale Workflow Test")
public class DeleteSaleWorkflowTest {

    @Test
    @DisplayName("Test deleting a sale and ensure all accounting entries are reversed")
    void testDeleteSaleWorkflow() {
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
            Crop crop = new Crop(0, "تفاح", List.of("صندوق"), Map.of("صندوق", List.of(20.0)));
            cropDataService.addCrop(crop);
            Crop createdCrop = cropDataService.getCropById(1);

            Contact supplier = new Contact(0, "مورد تفاح", "777-777-777", "البستان", true, false);
            contactDataService.addContact(supplier);
            Contact createdSupplier = contactDataService.getAllContacts().stream().filter(Contact::isSupplier).findFirst().orElseThrow();

            Contact customer = new Contact(0, "زبون تفاح", "888-888-888", "السوق المركزي", false, true);
            contactDataService.addContact(customer);
            Contact createdCustomer = contactDataService.getAllContacts().stream().filter(Contact::isCustomer).findFirst().orElseThrow();

            PurchaseRecord purchase = new PurchaseRecord();
            purchase.setCrop(createdCrop);
            purchase.setSupplier(createdSupplier);
            purchase.setPurchaseDate(LocalDate.now().minusDays(10));
            purchase.setQuantityKg(200.0); // 10 boxes
            purchase.setPricingUnit("صندوق");
            purchase.setSpecificFactor(20.0);
            purchase.setUnitPrice(12.5);
            purchase.setTotalCost(2500.0);
            purchase.setInvoiceNumber("PUR-SETUP-505");
            purchaseDataService.addPurchase(purchase, null, 0);

            // 2. Perform a sale to be deleted
            SaleRecord sale = new SaleRecord();
            sale.setCustomer(createdCustomer);
            sale.setCrop(createdCrop);
            sale.setSaleDate(LocalDate.now().minusDays(1));
            sale.setQuantitySoldKg(100.0); // 5 boxes
            sale.setSellingPricingUnit("صندوق");
            sale.setSpecificSellingFactor(20.0);
            sale.setSellingUnitPrice(20.0); // 20 per kg
            sale.setTotalSaleAmount(2000.0); // 100kg * 20/kg
            sale.setSaleInvoiceNumber("SAL-DEL-01");
            int saleId = saleDataService.addSale(sale, null, 0); // On credit
            assertTrue(saleId > 0, "Sale setup failed.");

            // == Phase 1: Get state before the deletion ==
            System.out.println("PHASE 1: Getting state before deletion...");
            List<FinancialAccount> accounts = financialAccountDataService.getAllAccounts();
            FinancialAccount arAccount = accounts.stream().filter(a -> a.getAccountName().equals("الذمم المدينة (العملاء)")).findFirst().orElseThrow();
            FinancialAccount inventoryAccount = accounts.stream().filter(a -> a.getAccountName().equals("المخزون")).findFirst().orElseThrow();
            FinancialAccount cogsAccount = accounts.stream().filter(a -> a.getAccountName().equals("تكلفة البضاعة المباعة")).findFirst().orElseThrow();
            FinancialAccount salesRevenueAccount = accounts.stream().filter(a -> a.getAccountName().equals("إيرادات المبيعات")).findFirst().orElseThrow();

            double arBalance = arAccount.getCurrentBalance();
            double inventoryBalance = inventoryAccount.getCurrentBalance();
            double cogsBalance = cogsAccount.getCurrentBalance();
            double salesRevenueBalance = salesRevenueAccount.getCurrentBalance();

            // == Phase 2: ACT (Delete the Sale) ==
            System.out.println("\nPHASE 2: Deleting the sale...");
            boolean deleted = saleDataService.deleteSale(saleId);
            assertTrue(deleted, "deleteSale method returned false.");
            System.out.println("Sale with ID " + saleId + " was deleted.");

            // == Phase 3: ASSERT (Verify Impacts) ==
            System.out.println("\nPHASE 3: Verifying impacts of the deletion...");

            List<FinancialAccount> finalAccounts = financialAccountDataService.getAllAccounts();
            FinancialAccount finalArAccount = finalAccounts.stream().filter(a -> a.getAccountId() == arAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount finalInventoryAccount = finalAccounts.stream().filter(a -> a.getAccountId() == inventoryAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount finalCogsAccount = finalAccounts.stream().filter(a -> a.getAccountId() == cogsAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount finalSalesRevenueAccount = finalAccounts.stream().filter(a -> a.getAccountId() == salesRevenueAccount.getAccountId()).findFirst().orElseThrow();

            // All balances should be reversed to their pre-sale state
            assertEquals(arBalance - 2000.0, finalArAccount.getCurrentBalance(), "A/R balance should be reversed.");
            assertEquals(inventoryBalance + 1250.0, finalInventoryAccount.getCurrentBalance(), "Inventory balance should be reversed.");
            assertEquals(cogsBalance - 1250.0, finalCogsAccount.getCurrentBalance(), "COGS balance should be reversed.");
            assertEquals(salesRevenueBalance - 2000.0, finalSalesRevenueAccount.getCurrentBalance(), "Sales Revenue balance should be reversed.");
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