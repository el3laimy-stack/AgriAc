package accounting.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * نموذج بيانات المحصول المحسن
 * يمثل محصولاً زراعياً مع وحدات القياس وعوامل التحويل الخاصة به
 */
public class Crop {
    private int cropId;
    private String cropName;
    private List<String> allowedPricingUnits;
    private Map<String, List<Double>> conversionFactors;

    // Constructors
    public Crop() {}

    public Crop(int cropId, String cropName, List<String> allowedPricingUnits, 
                Map<String, List<Double>> conversionFactors) {
        this.cropId = cropId;
        this.cropName = cropName;
        this.allowedPricingUnits = allowedPricingUnits;
        this.conversionFactors = conversionFactors;
    }

    // Getters and Setters
    public int getCropId() { return cropId; }
    public void setCropId(int cropId) { this.cropId = cropId; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public List<String> getAllowedPricingUnits() { return allowedPricingUnits; }
    public void setAllowedPricingUnits(List<String> allowedPricingUnits) { 
        this.allowedPricingUnits = allowedPricingUnits; 
    }

    public Map<String, List<Double>> getConversionFactors() { return conversionFactors; }
    public void setConversionFactors(Map<String, List<Double>> conversionFactors) { 
        this.conversionFactors = conversionFactors; 
    }

    /**
     * يحصل على عامل التحويل الأول لوحدة معينة
     * @param unit الوحدة المطلوبة
     * @return عامل التحويل أو 1.0 إذا لم توجد الوحدة
     */
    public double getFirstConversionFactor(String unit) {
        if (conversionFactors != null && conversionFactors.containsKey(unit)) {
            List<Double> factors = conversionFactors.get(unit);
            if (factors != null && !factors.isEmpty()) {
                return factors.get(0);
            }
        }
        return 1.0; // القيمة الافتراضية
    }

    @Override
    public String toString() {
        return cropName != null ? cropName : "محصول غير محدد";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Crop crop = (Crop) obj;
        return cropId == crop.cropId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cropId);
    }
    private boolean isActive;

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}

