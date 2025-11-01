package accounting.util;

import accounting.model.Crop;
import accounting.service.CropDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CropDataService Integration Test")
class CropDataServiceTest {

    private ImprovedDataManager dataManager;
    private CropDataService cropDataService;

    @BeforeEach
    void setUp() {
        // Use a shared in-memory SQLite database for testing to ensure all connections
        // in the same process access the same database.
        ImprovedDataManager.reinitializeForTest("jdbc:sqlite:file::memory:?cache=shared");
        dataManager = ImprovedDataManager.getInstance();
        cropDataService = new CropDataService();
    }

    @AfterEach
    void tearDown() {
        dataManager.shutdown();
    }

    @Test
    @DisplayName("Test saving a new crop and retrieving it")
    void testSaveAndGetCrop() throws SQLException {
        // Arrange
        Crop newCrop = new Crop(0, "ذرة صفراء", List.of("طن", "كيلو"), Map.of("كيلو", List.of(1000.0)));

        // Act
        int savedCropId = cropDataService.addCrop(newCrop);
        Crop retrievedCrop = cropDataService.getCropById(savedCropId);

        // Assert
        assertTrue(savedCropId > 0);
        assertNotNull(retrievedCrop);
        assertEquals(savedCropId, retrievedCrop.getCropId());
        assertEquals("ذرة صفراء", retrievedCrop.getCropName());
    }

    @Test
    @DisplayName("Test getting all active crops")
    void testGetAllActiveCrops() throws SQLException {
        // Arrange
        cropDataService.addCrop(new Crop(0, "قمح", List.of("طن"), Map.of()));
        cropDataService.addCrop(new Crop(0, "شعير", List.of("طن"), Map.of()));
        int inactiveCropId = cropDataService.addCrop(new Crop(0, "برسيم", List.of("طن"), Map.of()));
        cropDataService.deleteCrop(inactiveCropId);

        // Act
        List<Crop> activeCrops = cropDataService.getAllActiveCrops();

        // Assert
        assertEquals(2, activeCrops.size());
        assertTrue(activeCrops.stream().anyMatch(c -> c.getCropName().equals("قمح")));
        assertFalse(activeCrops.stream().anyMatch(c -> c.getCropName().equals("برسيم")));
    }

    @Test
    @DisplayName("Test updating an existing crop")
    void testUpdateCrop() throws SQLException {
        // Arrange
        int cropId = cropDataService.addCrop(new Crop(0, "بطاطس", List.of("شوال"), Map.of()));
        Crop cropToUpdate = cropDataService.getCropById(cropId);
        
        // Act
        cropToUpdate.setCropName("بطاطس شيبسي");
        boolean result = cropDataService.updateCrop(cropToUpdate);
        Crop updatedCrop = cropDataService.getCropById(cropId);

        // Assert
        assertTrue(result);
        assertNotNull(updatedCrop);
        assertEquals("بطاطس شيبسي", updatedCrop.getCropName());
    }

    @Test
    @DisplayName("Test deleting and reactivating a crop")
    void testDeleteAndReactivateCrop() throws SQLException {
        // Arrange
        int cropId = cropDataService.addCrop(new Crop(0, "بصل", List.of("كيلو"), Map.of()));
        
        // Act 1: Delete
        boolean deleteResult = cropDataService.deleteCrop(cropId);
        
        // Assert 1: Should be deleted
        assertTrue(deleteResult);
        assertNull(cropDataService.getCropById(cropId), "Deleted crop should not be retrievable by ID");
        
        // Act 2: Reactivate
        boolean reactivateResult = cropDataService.reactivateCrop(cropId);
        
        // Assert 2: Should be active again
        assertTrue(reactivateResult);
        assertNotNull(cropDataService.getCropById(cropId), "Reactivated crop should be retrievable");
    }

    @Test
    @DisplayName("Test finding a crop by its exact name")
    void testFindCropByName() throws SQLException {
        // Arrange
        String cropName = "خيار";
        cropDataService.addCrop(new Crop(0, cropName, List.of("حبة"), Map.of()));
        
        // Act
        Crop foundCrop = cropDataService.findCropByName(cropName);
        Crop notFoundCrop = cropDataService.findCropByName("جزر");

        // Assert
        assertNotNull(foundCrop);
        assertEquals(cropName, foundCrop.getCropName());
        assertNull(notFoundCrop);
    }
}
