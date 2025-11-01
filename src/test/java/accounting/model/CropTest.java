package accounting.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

@DisplayName("Crop Model Test")
class CropTest {

    @Test
    @DisplayName("Test Crop Getters and Setters")
    void testCropProperties() {
        // 1. Setup
        Crop crop = new Crop();
        
        // 2. Act
        crop.setCropId(1);
        crop.setCropName("قمح");
        crop.setActive(true);
        crop.setAllowedPricingUnits(List.of("طن", "كيلو"));
        crop.setConversionFactors(Map.of("كيلو", List.of(1000.0)));

        // 3. Assert
        assertEquals(1, crop.getCropId(), "getCropId should return the correct id.");
        assertEquals("قمح", crop.getCropName(), "getCropName should return the correct name.");
        assertTrue(crop.isActive(), "isActive should return true.");
        assertEquals("قمح", crop.toString(), "toString should return the crop name.");
        assertEquals(2, crop.getAllowedPricingUnits().size(), "Should be 2 allowed units.");
        assertEquals(1000.0, crop.getFirstConversionFactor("كيلو"), "First conversion factor for 'كيلو' should be 1000.0.");
    }

    @Test
    @DisplayName("Test Constructor")
    void testCropConstructor() {
        // 1. Setup
        List<String> units = List.of("صندوق");
        Map<String, List<Double>> factors = Map.of("صندوق", List.of(25.0));

        // 2. Act
        Crop crop = new Crop(10, "طماطم", units, factors);
        crop.setActive(false);

        // 3. Assert
        assertEquals(10, crop.getCropId());
        assertEquals("طماطم", crop.getCropName());
        assertEquals(units, crop.getAllowedPricingUnits());
        assertEquals(factors, crop.getConversionFactors());
        assertFalse(crop.isActive());
    }
    
    @Test
    @DisplayName("Test default conversion factor")
    void testDefaultConversionFactor() {
        Crop crop = new Crop();
        assertEquals(1.0, crop.getFirstConversionFactor("وحدة غير موجودة"), "Should return 1.0 for a non-existent unit.");
    }
}
