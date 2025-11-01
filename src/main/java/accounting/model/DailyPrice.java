package accounting.model;

import java.time.LocalDate;

/**
 * نموذج تتبع الأسعار اليومية للمحاصيل
 * مخصص لشركات تجارة المحاصيل الزراعية لتتبع تقلبات الأسعار
 */
public class DailyPrice {
    
    private int priceId;
    private int cropId;
    private Crop crop;
    private LocalDate priceDate;
    private double openingPrice;      // سعر الافتتاح
    private double highPrice;         // أعلى سعر في اليوم
    private double lowPrice;          // أقل سعر في اليوم
    private double closingPrice;      // سعر الإغلاق
    private double averagePrice;      // متوسط السعر
    private double tradingVolume;     // حجم التداول (بالكيلوجرام)
    private String marketCondition;   // حالة السوق (مرتفع/منخفض/مستقر)
    private String notes;             // ملاحظات إضافية
    private LocalDate createdAt;
    private LocalDate updatedAt;
    
    // Constructors
    public DailyPrice() {}
    
    public DailyPrice(int priceId, int cropId, LocalDate priceDate, double openingPrice, 
                     double highPrice, double lowPrice, double closingPrice, double averagePrice) {
        this.priceId = priceId;
        this.cropId = cropId;
        this.priceDate = priceDate;
        this.openingPrice = openingPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closingPrice = closingPrice;
        this.averagePrice = averagePrice;
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }
    
    // Getters and Setters
    public int getPriceId() {
        return priceId;
    }
    
    public void setPriceId(int priceId) {
        this.priceId = priceId;
    }
    
    public int getCropId() {
        return cropId;
    }
    
    public void setCropId(int cropId) {
        this.cropId = cropId;
    }
    
    public Crop getCrop() {
        return crop;
    }
    
    public void setCrop(Crop crop) {
        this.crop = crop;
        if (crop != null) {
            this.cropId = crop.getCropId();
        }
    }
    
    public LocalDate getPriceDate() {
        return priceDate;
    }
    
    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate;
    }
    
    public double getOpeningPrice() {
        return openingPrice;
    }
    
    public void setOpeningPrice(double openingPrice) {
        this.openingPrice = openingPrice;
    }
    
    public double getHighPrice() {
        return highPrice;
    }
    
    public void setHighPrice(double highPrice) {
        this.highPrice = highPrice;
    }
    
    public double getLowPrice() {
        return lowPrice;
    }
    
    public void setLowPrice(double lowPrice) {
        this.lowPrice = lowPrice;
    }
    
    public double getClosingPrice() {
        return closingPrice;
    }
    
    public void setClosingPrice(double closingPrice) {
        this.closingPrice = closingPrice;
    }
    
    public double getAveragePrice() {
        return averagePrice;
    }
    
    public void setAveragePrice(double averagePrice) {
        this.averagePrice = averagePrice;
    }
    
    public double getTradingVolume() {
        return tradingVolume;
    }
    
    public void setTradingVolume(double tradingVolume) {
        this.tradingVolume = tradingVolume;
    }
    
    public String getMarketCondition() {
        return marketCondition;
    }
    
    public void setMarketCondition(String marketCondition) {
        this.marketCondition = marketCondition;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDate getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDate getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // حساب التغيير في السعر
    public double getPriceChange() {
        return closingPrice - openingPrice;
    }
    
    // حساب نسبة التغيير في السعر
    public double getPriceChangePercentage() {
        if (openingPrice == 0) return 0;
        return ((closingPrice - openingPrice) / openingPrice) * 100;
    }
    
    // تحديد اتجاه السعر
    public String getPriceTrend() {
        double change = getPriceChange();
        if (change > 0) return "مرتفع";
        else if (change < 0) return "منخفض";
        else return "مستقر";
    }
    
    // التحقق من وجود تقلبات عالية
    public boolean isHighVolatility() {
        if (averagePrice == 0) return false;
        double priceRange = highPrice - lowPrice;
        double volatilityPercentage = (priceRange / averagePrice) * 100;
        return volatilityPercentage > 10; // أكثر من 10% تقلبات عالية
    }
    
    @Override
    public String toString() {
        return String.format("DailyPrice{cropId=%d, date=%s, open=%.2f, high=%.2f, low=%.2f, close=%.2f}", 
                           cropId, priceDate, openingPrice, highPrice, lowPrice, closingPrice);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DailyPrice that = (DailyPrice) o;
        
        if (priceId != that.priceId) return false;
        if (cropId != that.cropId) return false;
        return priceDate != null ? priceDate.equals(that.priceDate) : that.priceDate == null;
    }
    
    @Override
    public int hashCode() {
        int result = priceId;
        result = 31 * result + cropId;
        result = 31 * result + (priceDate != null ? priceDate.hashCode() : 0);
        return result;
    }
}