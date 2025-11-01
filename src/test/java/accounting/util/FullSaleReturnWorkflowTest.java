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

@DisplayName("Full End-to-End Sale Return Workflow Test")
public class FullSaleReturnWorkflowTest {

    @Test
    @DisplayName("Test a full sale return workflow and check all balances")
    void testFullSaleReturnWorkflow() {
        Connection anchorConnection = null;
        ImprovedDataManager dataManager = null;
        try {
            // == Phase 0: Full Test Setup ==
            ImprovedDataManager.reinitializeForTest("jdbc:sqlite:file::memory:?cache=shared");
            dataManager = ImprovedDataManager.getInstance();
            // Create an anchor connection to keep the in-memory DB alive for the test duration
            anchorConnection = dataManager.getConnection();

            CropDataService cropDataService = new CropDataService();
            ContactDataService contactDataService = new ContactDataService();
            FinancialAccountDataService financialAccountDataService = new FinancialAccountDataService();
            PurchaseDataService purchaseDataService = new PurchaseDataService();
            SaleDataService saleDataService = new SaleDataService();

            // 1. Create Crop
            Crop crop = new Crop(0, "خيار", List.of("صندوق"), Map.of("صندوق", List.of(12.0)));
            cropDataService.addCrop(crop);
            Crop createdCrop = cropDataService.getCropById(1);

            // 2. Create Supplier and Customer
            Contact supplier = new Contact(0, "مورد الخيار", "111-111-111", "المزرعة", true, false);
            contactDataService.addContact(supplier);
            Contact createdSupplier = contactDataService.getAllContacts().stream().filter(Contact::isSupplier).findFirst().orElseThrow();

            Contact customer = new Contact(0, "زبون الخيار", "222-222-222", "السوق", false, true);
            contactDataService.addContact(customer);
            Contact createdCustomer = contactDataService.getAllContacts().stream().filter(Contact::isCustomer).findFirst().orElseThrow();

            // 3. Purchase to get stock (10 boxes = 120 kg @ 20/box -> Cost = 2400)
            PurchaseRecord purchase = new PurchaseRecord();
            purchase.setCrop(createdCrop);
            purchase.setSupplier(createdSupplier);
            purchase.setPurchaseDate(LocalDate.now().minusDays(2));
            purchase.setQuantityKg(120.0);
            purchase.setPricingUnit("صندوق");
            purchase.setSpecificFactor(12.0);
            purchase.setUnitPrice(20.0);
            purchase.setTotalCost(2400.0);
            purchase.setInvoiceNumber("PUR-202");
            purchaseDataService.addPurchase(purchase, null, 0); // On credit

            // 4. Sell the stock (5 boxes = 60 kg @ 35/box -> Sale = 2100)
            SaleRecord originalSale = new SaleRecord();
            originalSale.setCustomer(createdCustomer);
            originalSale.setCrop(createdCrop);
            originalSale.setSaleDate(LocalDate.now().minusDays(1));
            originalSale.setQuantitySoldKg(60.0);
            originalSale.setSellingPricingUnit("صندوق");
            originalSale.setSpecificSellingFactor(12.0);
            originalSale.setSellingUnitPrice(35.0);
            originalSale.setTotalSaleAmount(2100.0);
            originalSale.setSaleInvoiceNumber("SAL-002");
            int saleId = saleDataService.addSale(originalSale, null, 0); // Sale on credit
            originalSale.setSaleId(saleId);

            // == Phase 1: Get state before the return ==
            System.out.println("PHASE 1: Getting state before return...");

            List<FinancialAccount> accounts = financialAccountDataService.getAllAccounts();
            FinancialAccount arAccount = accounts.stream().filter(a -> a.getAccountName().equals("الذمم المدينة (العملاء)")).findFirst().orElseThrow();
            FinancialAccount inventoryAccount = accounts.stream().filter(a -> a.getAccountName().equals("المخزون")).findFirst().orElseThrow();
            FinancialAccount cogsAccount = accounts.stream().filter(a -> a.getAccountName().equals("تكلفة البضاعة المباعة")).findFirst().orElseThrow();
            FinancialAccount salesReturnAccount = accounts.stream().filter(a -> a.getAccountName().equals("مرتجعات ومسموحات المبيعات")).findFirst().orElseThrow();

            double initialArBalance = arAccount.getCurrentBalance();
            double initialInventoryBalance = inventoryAccount.getCurrentBalance();
            double initialCogsBalance = cogsAccount.getCurrentBalance();
            double initialSalesReturnBalance = salesReturnAccount.getCurrentBalance();

            CropDataService.CropStatistics initialStats = cropDataService.getCropStatistics(createdCrop.getCropId(), null, null);
            double initialStock = initialStats.getCurrentStock();

            double costOfReturnedGoods = 480.0;
            double refundAmount = 700.0;

            // == Phase 2: ACT (Perform Sale Return) ==
            System.out.println("\nPHASE 2: Performing a sale return...");
            SaleReturn saleReturn = new SaleReturn();
            saleReturn.setOriginalSale(originalSale);
            saleReturn.setReturnDate(LocalDate.now());
            saleReturn.setQuantityKg(24.0);
            saleReturn.setRefundAmount(refundAmount);
            saleReturn.setReturnReason("جودة غير مطابقة");

            int returnId = saleDataService.addSaleReturn(saleReturn);
            assertTrue(returnId > 0, "Sale return was not created.");
            System.out.println("Sale return recorded with ID: " + returnId);

            // == Phase 3: ASSERT (Verify Impacts) ==
            System.out.println("\nPHASE 3: Verifying impacts of the return...");

            CropDataService.CropStatistics finalStats = cropDataService.getCropStatistics(createdCrop.getCropId(), null, null);
            assertEquals(initialStock + 24.0, finalStats.getCurrentStock(), "Inventory stock should increase by 24 kg.");
            System.out.println("SUCCESS: Inventory stock is correct.");

            List<FinancialAccount> finalAccounts = financialAccountDataService.getAllAccounts();
            FinancialAccount finalArAccount = finalAccounts.stream().filter(a -> a.getAccountId() == arAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount finalInventoryAccount = finalAccounts.stream().filter(a -> a.getAccountId() == inventoryAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount finalCogsAccount = finalAccounts.stream().filter(a -> a.getAccountId() == cogsAccount.getAccountId()).findFirst().orElseThrow();
            FinancialAccount finalSalesReturnAccount = finalAccounts.stream().filter(a -> a.getAccountId() == salesReturnAccount.getAccountId()).findFirst().orElseThrow();

            assertEquals(initialArBalance - refundAmount, finalArAccount.getCurrentBalance(), "A/R balance should decrease by the refund amount.");
            System.out.println("SUCCESS: Accounts Receivable balance is correct.");

            assertEquals(initialInventoryBalance + costOfReturnedGoods, finalInventoryAccount.getCurrentBalance(), "Inventory balance should increase by the cost of returned goods.");
            System.out.println("SUCCESS: Inventory balance is correct.");

            assertEquals(initialCogsBalance - costOfReturnedGoods, finalCogsAccount.getCurrentBalance(), "COGS balance should decrease by the cost of returned goods.");
            System.out.println("SUCCESS: Cost of Goods Sold balance is correct.");

            assertEquals(initialSalesReturnBalance + refundAmount, finalSalesReturnAccount.getCurrentBalance(), "Sales Return balance should increase by the refund amount.");
            System.out.println("SUCCESS: Sales Return balance is correct.");

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