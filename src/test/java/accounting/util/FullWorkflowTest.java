package accounting.util;

import accounting.model.Contact;
import accounting.model.Crop;
import accounting.model.FinancialAccount;
import accounting.model.PurchaseRecord;
import accounting.service.ContactDataService;
import accounting.service.CropDataService;
import accounting.service.FinancialAccountDataService;
import accounting.service.PurchaseDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Full End-to-End Workflow Test")
public class FullWorkflowTest {

    private ImprovedDataManager dataManager;
    private CropDataService cropDataService;
    private ContactDataService contactDataService;
    private FinancialAccountDataService financialAccountDataService;
    private PurchaseDataService purchaseDataService;

    @BeforeEach
    void setUp() {
        ImprovedDataManager.reinitializeForTest("jdbc:sqlite:file::memory:?cache=shared");
        dataManager = ImprovedDataManager.getInstance();
        cropDataService = new CropDataService();
        contactDataService = new ContactDataService();
        financialAccountDataService = new FinancialAccountDataService();
        purchaseDataService = new PurchaseDataService();
    }

    @AfterEach
    void tearDown() {
        dataManager.shutdown();
    }

    @Test
    @DisplayName("Test a full purchase workflow from creating entities to checking balances")
    void testFullPurchaseWorkflow() throws SQLException {
        // == Phase 1: SETUP ==
        System.out.println("PHASE 1: Setting up entities...");

        // 1.1: Create a Crop
        Crop crop = new Crop(0, "تفاح", List.of("كيلو"), Map.of("كيلو", List.of(1.0)));
        int cropId = cropDataService.addCrop(crop);
        Crop createdCrop = cropDataService.getCropById(cropId);
        assertNotNull(createdCrop, "Crop setup failed.");

        // 1.2: Create a Supplier
        Contact supplier = new Contact(0, "مورد فواكه", "111-222-333", "سوق الجملة", true, false);
        Contact createdSupplier = contactDataService.addContact(supplier).orElse(null);
        assertNotNull(createdSupplier, "Supplier setup failed.");

        // 1.3: Get initial state of relevant financial accounts
        FinancialAccount inventoryAccount = financialAccountDataService.getAllAccounts().stream()
                .filter(a -> a.getAccountName().equals("المخزون"))
                .findFirst().orElseThrow(() -> new AssertionError("Inventory account not found."));

        FinancialAccount apAccount = financialAccountDataService.getAllAccounts().stream()
                .filter(a -> a.getAccountName().equals("الذمم الدائنة (الموردين)"))
                .findFirst().orElseThrow(() -> new AssertionError("Accounts Payable account not found."));

        double initialInventoryValue = inventoryAccount.getCurrentBalance();
        double initialApValue = apAccount.getCurrentBalance();
        System.out.println("Initial Inventory Value: " + initialInventoryValue);
        System.out.println("Initial AP Value: " + initialApValue);

        // == Phase 2: ACT (Perform Purchase) ==
        System.out.println("\nPHASE 2: Performing a purchase...");
        PurchaseRecord purchase = new PurchaseRecord();
        purchase.setCrop(createdCrop);
        purchase.setSupplier(createdSupplier);
        purchase.setPurchaseDate(LocalDate.now());
        purchase.setQuantityKg(100.0);
        purchase.setPricingUnit("كيلو");
        purchase.setSpecificFactor(1.0);
        purchase.setUnitPrice(5.0);
        purchase.setTotalCost(500.0); // 100kg * 5/kg
        purchase.setInvoiceNumber("INV-001");

        // Perform purchase on credit (no payment yet)
        int purchaseId = purchaseDataService.addPurchase(purchase, null, 0);
        assertTrue(purchaseId > 0, "Purchase was not created.");
        System.out.println("Purchase recorded with ID: " + purchaseId);

        // == Phase 3: ASSERT (Verify Impacts) ==
        System.out.println("\nPHASE 3: Verifying impacts of the purchase...");

        // 3.1: Verify Inventory Stock
        CropDataService.CropStatistics stats = cropDataService.getCropStatistics(cropId, null, null);
        assertNotNull(stats, "Could not retrieve crop statistics.");
        assertEquals(100.0, stats.getCurrentStock(), "Inventory stock quantity should increase by 100.");
        System.out.println("SUCCESS: Inventory stock is correct.");

        // 3.2: Verify Financial Account Balances
        FinancialAccount finalInventoryAccount = financialAccountDataService.getAllAccounts().stream()
            .filter(a -> a.getAccountId() == inventoryAccount.getAccountId()).findFirst().get();
        FinancialAccount finalApAccount = financialAccountDataService.getAllAccounts().stream()
            .filter(a -> a.getAccountId() == apAccount.getAccountId()).findFirst().get();

        assertEquals(initialInventoryValue + 500.0, finalInventoryAccount.getCurrentBalance(), "Inventory account balance should increase by purchase cost.");
        assertEquals(initialApValue + 500.0, finalApAccount.getCurrentBalance(), "Accounts Payable balance should increase by purchase cost.");
        System.out.println("SUCCESS: Financial account balances are correct.");

        System.out.println("Verification complete.");

    }
}
