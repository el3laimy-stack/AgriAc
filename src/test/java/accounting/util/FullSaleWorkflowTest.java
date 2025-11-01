package accounting.util;

import accounting.model.Contact;
import accounting.model.Crop;
import accounting.model.FinancialAccount;
import accounting.model.PurchaseRecord;
import accounting.model.SaleRecord;
import accounting.service.ContactDataService;
import accounting.service.CropDataService;
import accounting.service.FinancialAccountDataService;
import accounting.service.PurchaseDataService;
import accounting.service.SaleDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Full End-to-End Sale Workflow Test")
public class FullSaleWorkflowTest {

    private ImprovedDataManager dataManager;
    private CropDataService cropDataService;
    private ContactDataService contactDataService;
    private FinancialAccountDataService financialAccountDataService;
    private PurchaseDataService purchaseDataService;
    private SaleDataService saleDataService;

    @BeforeEach
    void setUp() {
        ImprovedDataManager.reinitializeForTest("jdbc:sqlite:file::memory:?cache=shared");
        dataManager = ImprovedDataManager.getInstance();
        cropDataService = new CropDataService();
        contactDataService = new ContactDataService();
        financialAccountDataService = new FinancialAccountDataService();
        purchaseDataService = new PurchaseDataService();
        saleDataService = new SaleDataService();
    }

    @AfterEach
    void tearDown() {
        dataManager.shutdown();
    }

    @Test
    @DisplayName("Test a full sale workflow from purchase to sale and checking balances")
    void testFullSaleWorkflow() throws SQLException {
        // == Phase 1: SETUP (Create entities and stock inventory) ==
        System.out.println("PHASE 1: Setting up entities and initial stock...");

        // 1.1: Create a Crop
        Crop crop = new Crop(0, "طماطم", List.of("صندوق"), Map.of("صندوق", List.of(10.0))); // صندوق = 10 كيلو
        int cropId = cropDataService.addCrop(crop);
        Crop createdCrop = cropDataService.getCropById(cropId);
        assertNotNull(createdCrop, "Crop setup failed.");

        // 1.2: Create a Supplier and a Customer
        Contact supplier = new Contact(0, "مشتل الخضروات", "444-555-666", "المزرعة", true, false);
        Contact createdSupplier = contactDataService.addContact(supplier).orElse(null);
        assertNotNull(createdSupplier, "Supplier setup failed.");

        Contact customer = new Contact(0, "زبون التجزئة", "777-888-999", "السوق المحلي", false, true);
        Contact createdCustomer = contactDataService.addContact(customer).orElse(null);
        assertNotNull(createdCustomer, "Customer setup failed.");

        // 1.3: Purchase to get stock (20 boxes = 200 kg @ 15/box)
        PurchaseRecord purchase = new PurchaseRecord();
        purchase.setCrop(createdCrop);
        purchase.setSupplier(createdSupplier);
        purchase.setPurchaseDate(LocalDate.now().minusDays(1));
        purchase.setQuantityKg(200.0); // 20 boxes * 10 kg/box
        purchase.setPricingUnit("صندوق");
        purchase.setSpecificFactor(10.0);
        purchase.setUnitPrice(15.0); // 15 per box
        purchase.setTotalCost(3000.0); // 20 boxes * 15
        purchase.setInvoiceNumber("PUR-101");
        purchaseDataService.addPurchase(purchase, null, 0); // Purchase on credit

        System.out.println("Initial stock of 'طماطم' is 200 kg.");

        // 1.4: Get initial state of relevant financial accounts
        List<FinancialAccount> allAccounts = financialAccountDataService.getAllAccounts();
        FinancialAccount arAccount = allAccounts.stream().filter(a -> a.getAccountName().equals("الذمم المدينة (العملاء)")).findFirst().orElseThrow();
        FinancialAccount cashAccount = allAccounts.stream().filter(a -> a.getAccountName().equals("الخزنة الرئيسية")).findFirst().orElseThrow();
        FinancialAccount salesRevenueAccount = allAccounts.stream().filter(a -> a.getAccountName().equals("إيرادات المبيعات")).findFirst().orElseThrow();
        FinancialAccount cogsAccount = allAccounts.stream().filter(a -> a.getAccountName().equals("تكلفة البضاعة المباعة")).findFirst().orElseThrow();

        double initialArBalance = arAccount.getCurrentBalance();
        double initialCashBalance = cashAccount.getCurrentBalance();
        double initialSalesRevenue = salesRevenueAccount.getCurrentBalance();
        double initialCogs = cogsAccount.getCurrentBalance();

        // == Phase 2: ACT (Perform Sale) ==
        System.out.println("\nPHASE 2: Performing a sale...");
        // Sell 5 boxes (50 kg) for 25/box. Total = 1250. Pay 1000 in cash.
        SaleRecord sale = new SaleRecord();
        sale.setCustomer(createdCustomer);
        sale.setCrop(createdCrop);
        sale.setSaleDate(LocalDate.now());
        sale.setQuantitySoldKg(50.0); // 5 boxes * 10 kg/box
        sale.setSellingPricingUnit("صندوق");
        sale.setSpecificSellingFactor(10.0);
        sale.setSellingUnitPrice(25.0); // 25 per box
        sale.setTotalSaleAmount(1250.0); // 5 boxes * 25
        sale.setSaleInvoiceNumber("SAL-001");

        // Perform sale with partial payment
        int saleId = saleDataService.addSale(sale, cashAccount, 1000.0);
        assertTrue(saleId > 0, "Sale was not created.");
        System.out.println("Sale recorded with ID: " + saleId);

        // == Phase 3: ASSERT (Verify Impacts) ==
        System.out.println("\nPHASE 3: Verifying impacts of the sale...");

        // 3.1: Verify Inventory Stock
        CropDataService.CropStatistics stats = cropDataService.getCropStatistics(cropId, null, null);
        assertNotNull(stats, "Could not retrieve crop statistics.");
        assertEquals(150.0, stats.getCurrentStock(), "Inventory stock quantity should decrease by 50 kg.");
        System.out.println("SUCCESS: Inventory stock is correct (200 - 50 = 150).");

        // 3.2: Verify Financial Account Balances
        List<FinancialAccount> finalAccounts = financialAccountDataService.getAllAccounts();
        FinancialAccount finalArAccount = finalAccounts.stream().filter(a -> a.getAccountId() == arAccount.getAccountId()).findFirst().orElseThrow();
        FinancialAccount finalCashAccount = finalAccounts.stream().filter(a -> a.getAccountId() == cashAccount.getAccountId()).findFirst().orElseThrow();
        FinancialAccount finalSalesRevenueAccount = finalAccounts.stream().filter(a -> a.getAccountId() == salesRevenueAccount.getAccountId()).findFirst().orElseThrow();
        FinancialAccount finalCogsAccount = finalAccounts.stream().filter(a -> a.getAccountId() == cogsAccount.getAccountId()).findFirst().orElseThrow();

        // AR should increase by the unpaid amount (1250 - 1000 = 250)
        assertEquals(initialArBalance + 250.0, finalArAccount.getCurrentBalance(), "A/R balance should increase by the unpaid amount.");
        System.out.println("SUCCESS: Accounts Receivable balance is correct.");

        // Cash should increase by the amount paid
        assertEquals(initialCashBalance + 1000.0, finalCashAccount.getCurrentBalance(), "Cash balance should increase by the amount paid.");
        System.out.println("SUCCESS: Cash balance is correct.");

        // Sales Revenue should increase by the total sale amount
        assertEquals(initialSalesRevenue + 1250.0, finalSalesRevenueAccount.getCurrentBalance(), "Sales Revenue should increase by total sale amount.");
        System.out.println("SUCCESS: Sales Revenue balance is correct.");

        // COGS should increase. Cost was 3000 for 200kg = 15/kg. Sale was 50kg. COGS = 50 * 15 = 750.
        assertEquals(initialCogs + 750.0, finalCogsAccount.getCurrentBalance(), "COGS should increase by the cost of the goods sold.");
        System.out.println("SUCCESS: Cost of Goods Sold balance is correct.");

        System.out.println("\nVerification complete.");
    }
}
