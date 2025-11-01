package accounting.util;

import accounting.model.*;
import accounting.service.CropDataService;
import accounting.service.PurchaseDataService;
import accounting.formatter.FormatUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * نظام التنبيهات الذكية المتقدم
 * يوفر تنبيهات استباقية ومخصصة لمساعدة المزارعين في اتخاذ قرارات أفضل
 */
public class SmartAlertSystem {
    
    private static final Logger LOGGER = Logger.getLogger(SmartAlertSystem.class.getName());
    
    private final CropDataService cropDataService;
    private final PurchaseDataService purchaseDataService;
    private final ImprovedDataManager dataManager;
    
    public SmartAlertSystem() {
        this.cropDataService = new CropDataService();
        this.purchaseDataService = new PurchaseDataService();
        this.dataManager = ImprovedDataManager.getInstance();
    }
    
    /**
     * الحصول على جميع التنبيهات النشطة
     */
    public List<SmartAlert> getAllActiveAlerts() {
        List<SmartAlert> alerts = new ArrayList<>();
        
        // تنبيهات المخزون
        alerts.addAll(getInventoryAlerts());
        
        // تنبيهات الأسعار
        alerts.addAll(getPriceAlerts());
        
        // تنبيهات الموسم
        alerts.addAll(getSeasonalAlerts());
        
        // تنبيهات الربحية
        alerts.addAll(getProfitabilityAlerts());
        
        // تنبيهات المدفوعات
        alerts.addAll(getPaymentAlerts());
        
        // تنبيهات الجودة
        alerts.addAll(getQualityAlerts());
        
        // ترتيب التنبيهات حسب الأولوية والتاريخ
        alerts.sort((a, b) -> {
            int priorityCompare = b.getPriority().ordinal() - a.getPriority().ordinal();
            if (priorityCompare != 0) return priorityCompare;
            return b.getCreatedDate().compareTo(a.getCreatedDate());
        });
        
        return alerts;
    }
    
    /**
     * تنبيهات المخزون الذكية
     */
    private List<SmartAlert> getInventoryAlerts() {
        List<SmartAlert> alerts = new ArrayList<>();
        
        try {
            List<Crop> crops = cropDataService.getAllActiveCrops();
            
            for (Crop crop : crops) {
                CropDataService.CropStatistics stats = cropDataService.getCropStatistics(
                    crop.getCropId(), 
                    LocalDate.now().minusMonths(6), 
                    LocalDate.now()
                );
                
                if (stats != null) {
                    // تنبيه نفاد المخزون
                    if (stats.getCurrentStock() <= 0) {
                        alerts.add(new SmartAlert(
                            AlertType.INVENTORY_EMPTY,
                            AlertPriority.HIGH,
                            "نفاد مخزون " + crop.getCropName(),
                            "المخزون الحالي لمحصول " + crop.getCropName() + " قد نفد تماماً. " +
                            "يُنصح بالشراء فوراً لتجنب فقدان الفرص التجارية.",
                            generateInventoryRecommendation(crop, stats),
                            LocalDate.now()
                        ));
                    }
                    // تنبيه انخفاض المخزون
                    else if (stats.getCurrentStock() < calculateMinimumStock(crop, stats)) {
                        alerts.add(new SmartAlert(
                            AlertType.INVENTORY_LOW,
                            AlertPriority.MEDIUM,
                            "انخفاض مخزون " + crop.getCropName(),
                            String.format("المخزون الحالي %.2f كجم أقل من الحد الأدنى المطلوب %.2f كجم.",
                                stats.getCurrentStock(), calculateMinimumStock(crop, stats)),
                            generateInventoryRecommendation(crop, stats),
                            LocalDate.now()
                        ));
                    }
                    // تنبيه فائض المخزون
                    else if (stats.getCurrentStock() > calculateMaximumStock(crop, stats)) {
                        alerts.add(new SmartAlert(
                            AlertType.INVENTORY_EXCESS,
                            AlertPriority.LOW,
                            "فائض مخزون " + crop.getCropName(),
                            String.format("المخزون الحالي %.2f كجم أعلى من الحد الأقصى المُوصى به %.2f كجم. " +
                                "قد يؤدي هذا إلى تكاليف تخزين إضافية أو تلف المنتج.",
                                stats.getCurrentStock(), calculateMaximumStock(crop, stats)),
                            "يُنصح ببيع جزء من المخزون أو تطوير استراتيجيات تسويق جديدة.",
                            LocalDate.now()
                        ));
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("خطأ في إنشاء تنبيهات المخزون: " + e.getMessage());
        }
        
        return alerts;
    }
    
    /**
     * تنبيهات الأسعار الذكية
     */
    private List<SmartAlert> getPriceAlerts() {
        List<SmartAlert> alerts = new ArrayList<>();
        
        try {
            List<Crop> crops = cropDataService.getAllActiveCrops();
            
            for (Crop crop : crops) {
                // أصبح تحليل الأسعار يعتمد على بيانات حقيقية
                PriceAnalysis analysis = analyzePriceTrends(crop.getCropId());
                
                if (analysis != null && analysis.isSignificantPriceIncrease()) {
                    alerts.add(new SmartAlert(
                        AlertType.PRICE_OPPORTUNITY,
                        AlertPriority.HIGH,
                        "فرصة بيع مربحة لـ " + crop.getCropName(),
                        String.format("ارتفعت أسعار %s بنسبة %.1f%% خلال الفترة الأخيرة. السعر الحالي %.2f مقارنة بالمتوسط %.2f.",
                            crop.getCropName(), analysis.getPriceChangePercentage(),
                            analysis.getCurrentPrice(), analysis.getAveragePrice()),
                        "يُنصح بالبيع الآن للاستفادة من الأسعار المرتفعة.",
                        LocalDate.now()
                    ));
                }
                
                if (analysis != null && analysis.isSignificantPriceDecrease()) {
                    // ... (منطق التحذير من انخفاض السعر)
                }
            }
        } catch (Exception e) {
            LOGGER.warning("خطأ في إنشاء تنبيهات الأسعار: " + e.getMessage());
        }
        
        return alerts;
    }
    
    /**
     * تنبيهات الموسم الزراعي
     */
    private List<SmartAlert> getSeasonalAlerts() {
        List<SmartAlert> alerts = new ArrayList<>();
        
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        
        // تنبيهات موسمية مخصصة للمحاصيل المصرية
        if (currentMonth >= 10 && currentMonth <= 12) { // موسم زراعة الشتاء
            alerts.add(new SmartAlert(
                AlertType.SEASONAL_REMINDER,
                AlertPriority.MEDIUM,
                "موسم زراعة محاصيل الشتاء",
                "حان وقت زراعة محاصيل الشتاء مثل القمح والشعير والبرسيم. " +
                "تأكد من توفر البذور والأسمدة اللازمة.",
                "راجع خطة الزراعة وتأكد من جاهزية الأراضي والمعدات.",
                now
            ));
        }
        
        if (currentMonth >= 3 && currentMonth <= 5) { // موسم زراعة الصيف
            alerts.add(new SmartAlert(
                AlertType.SEASONAL_REMINDER,
                AlertPriority.MEDIUM,
                "موسم زراعة محاصيل الصيف",
                "حان وقت زراعة محاصيل الصيف مثل الذرة والقطن والأرز. " +
                "تأكد من توفر مياه الري والبذور المحسنة.",
                "راجع خطة الري وتأكد من صيانة شبكات الري.",
                now
            ));
        }
        
        return alerts;
    }
    
    /**
     * تنبيهات الربحية
     */
    private List<SmartAlert> getProfitabilityAlerts() {
        List<SmartAlert> alerts = new ArrayList<>();
        
        try {
            List<Crop> crops = cropDataService.getAllActiveCrops();
            
            for (Crop crop : crops) {
                CropDataService.CropStatistics stats = cropDataService.getCropStatistics(
                    crop.getCropId(), 
                    LocalDate.now().minusMonths(3), 
                    LocalDate.now()
                );
                
                if (stats != null && stats.getProfitMargin() < 10) { // هامش ربح أقل من 10%
                    alerts.add(new SmartAlert(
                        AlertType.PROFITABILITY_WARNING,
                        AlertPriority.HIGH,
                        "انخفاض ربحية " + crop.getCropName(),
                        String.format("هامش الربح لمحصول %s منخفض (%.1f%%). " +
                            "قد تحتاج لمراجعة استراتيجية التسعير أو تقليل التكاليف.",
                            crop.getCropName(), stats.getProfitMargin()),
                        "راجع تكاليف الإنتاج وابحث عن طرق لتحسين الكفاءة أو زيادة أسعار البيع.",
                        LocalDate.now()
                    ));
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("خطأ في إنشاء تنبيهات الربحية: " + e.getMessage());
        }
        
        return alerts;
    }
    
    /**
     * تنبيهات المدفوعات والذمم
     */
    private List<SmartAlert> getPaymentAlerts() {
        List<SmartAlert> alerts = new ArrayList<>();
        
        // هذه ستحتاج لتطوير نظام إدارة الذمم في المستقبل
        // يمكن إضافة تنبيهات للفواتير المستحقة والمدفوعات المتأخرة
        
        return alerts;
    }
    
    /**
     * تنبيهات الجودة والتخزين
     */
    private List<SmartAlert> getQualityAlerts() {
        List<SmartAlert> alerts = new ArrayList<>();
        
        // تنبيهات عامة للجودة والتخزين
        LocalDate now = LocalDate.now();
        
        if (now.getMonthValue() >= 6 && now.getMonthValue() <= 8) { // أشهر الصيف
            alerts.add(new SmartAlert(
                AlertType.QUALITY_WARNING,
                AlertPriority.MEDIUM,
                "تحذير من حرارة الصيف",
                "درجات الحرارة المرتفعة قد تؤثر على جودة المحاصيل المخزنة. " +
                "تأكد من توفر التهوية والتبريد المناسب.",
                "راجع أنظمة التخزين وتأكد من مراقبة درجة الحرارة والرطوبة.",
                now
            ));
        }
        
        return alerts;
    }
    
    // الطرق المساعدة
    
    private double calculateMinimumStock(Crop crop, CropDataService.CropStatistics stats) {
        // حساب الحد الأدنى للمخزون بناءً على معدل البيع التاريخي
        double averageMonthlySales = stats.getTotalSold() / 6.0; // متوسط 6 أشهر
        return averageMonthlySales * 0.5; // مخزون لمدة أسبوعين
    }
    
    private double calculateMaximumStock(Crop crop, CropDataService.CropStatistics stats) {
        // حساب الحد الأقصى للمخزون
        double averageMonthlySales = stats.getTotalSold() / 6.0;
        return averageMonthlySales * 3.0; // مخزون لمدة 3 أشهر
    }
    
    private String generateInventoryRecommendation(Crop crop, CropDataService.CropStatistics stats) {
        double recommendedPurchase = calculateMinimumStock(crop, stats) * 2;
        return String.format("يُنصح بشراء %.2f كجم من %s لضمان استمرارية العمل.",
            recommendedPurchase, crop.getCropName());
    }
    
    private PriceAnalysis analyzePriceTrends(int cropId) {
        try {
            // جلب متوسط السعر لآخر 30 يوم (السعر الحالي)
            PurchaseDataService.PurchaseStatistics currentStats = purchaseDataService.getPurchaseStatistics(
                LocalDate.now().minusDays(30), LocalDate.now(), cropId, null);
            double currentPrice = currentStats.getAverageCostPerKg();

            // جلب متوسط السعر لآخر 6 أشهر (المتوسط التاريخي)
            PurchaseDataService.PurchaseStatistics historicalStats = purchaseDataService.getPurchaseStatistics(
                LocalDate.now().minusMonths(6), LocalDate.now(), cropId, null);
            double averagePrice = historicalStats.getAverageCostPerKg();

            if (averagePrice == 0) return null; // تجنب القسمة على صفر

            double priceChangePercentage = ((currentPrice - averagePrice) / averagePrice) * 100;

            boolean significantIncrease = priceChangePercentage > 15; // فرصة إذا زاد السعر بأكثر من 15%
            boolean significantDecrease = priceChangePercentage < -10; // تحذير إذا انخفض بأكثر من 10%

            return new PriceAnalysis(currentPrice, averagePrice, priceChangePercentage, significantIncrease, significantDecrease);

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "فشل في تحليل اتجاهات الأسعار للمحصول " + cropId, e);
            return null;
        }
    }
    
    // الفئات المساعدة
    
    public enum AlertType {
        INVENTORY_EMPTY("نفاد المخزون"),
        INVENTORY_LOW("انخفاض المخزون"),
        INVENTORY_EXCESS("فائض المخزون"),
        PRICE_OPPORTUNITY("فرصة سعرية"),
        PRICE_WARNING("تحذير سعري"),
        SEASONAL_REMINDER("تذكير موسمي"),
        PROFITABILITY_WARNING("تحذير ربحية"),
        PAYMENT_DUE("استحقاق دفع"),
        QUALITY_WARNING("تحذير جودة");
        
        private final String arabicName;
        
        AlertType(String arabicName) {
            this.arabicName = arabicName;
        }
        
        public String getArabicName() {
            return arabicName;
        }
    }
    
    public enum AlertPriority {
        LOW("منخفضة"),
        MEDIUM("متوسطة"),
        HIGH("عالية"),
        CRITICAL("حرجة");
        
        private final String arabicName;
        
        AlertPriority(String arabicName) {
            this.arabicName = arabicName;
        }
        
        public String getArabicName() {
            return arabicName;
        }
    }
    
    public static class SmartAlert {
        private final AlertType type;
        private final AlertPriority priority;
        private final String title;
        private final String message;
        private final String recommendation;
        private final LocalDate createdDate;
        private boolean isRead;
        private boolean isActioned;
        
        public SmartAlert(AlertType type, AlertPriority priority, String title, 
                         String message, String recommendation, LocalDate createdDate) {
            this.type = type;
            this.priority = priority;
            this.title = title;
            this.message = message;
            this.recommendation = recommendation;
            this.createdDate = createdDate;
            this.isRead = false;
            this.isActioned = false;
        }
        
        // Getters and Setters
        public AlertType getType() { return type; }
        public AlertPriority getPriority() { return priority; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getRecommendation() { return recommendation; }
        public LocalDate getCreatedDate() { return createdDate; }
        public boolean isRead() { return isRead; }
        public void setRead(boolean read) { isRead = read; }
        public boolean isActioned() { return isActioned; }
        public void setActioned(boolean actioned) { isActioned = actioned; }
        
        public String getFormattedDate() {
            return FormatUtils.formatDateForDisplay(createdDate);
        }
        
        public long getDaysOld() {
            return ChronoUnit.DAYS.between(createdDate, LocalDate.now());
        }
        
        public String getPriorityColor() {
            switch (priority) {
                case CRITICAL: return "#dc3545"; // أحمر
                case HIGH: return "#fd7e14"; // برتقالي
                case MEDIUM: return "#ffc107"; // أصفر
                case LOW: return "#28a745"; // أخضر
                default: return "#6c757d"; // رمادي
            }
        }
    }
    
    private static class PriceAnalysis {
        private final double currentPrice;
        private final double averagePrice;
        private final double priceChangePercentage;
        private final boolean significantIncrease;
        private final boolean significantDecrease;
        
        public PriceAnalysis(double currentPrice, double averagePrice, double priceChangePercentage,
                           boolean significantIncrease, boolean significantDecrease) {
            this.currentPrice = currentPrice;
            this.averagePrice = averagePrice;
            this.priceChangePercentage = priceChangePercentage;
            this.significantIncrease = significantIncrease;
            this.significantDecrease = significantDecrease;
        }
        
        public double getCurrentPrice() { return currentPrice; }
        public double getAveragePrice() { return averagePrice; }
        public double getPriceChangePercentage() { return priceChangePercentage; }
        public boolean isSignificantPriceIncrease() { return significantIncrease; }
        public boolean isSignificantPriceDecrease() { return significantDecrease; }
    }
}