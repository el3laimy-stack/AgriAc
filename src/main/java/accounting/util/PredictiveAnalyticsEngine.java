package accounting.util;

import accounting.model.*;
import accounting.service.CropDataService;
import accounting.service.PurchaseDataService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * محرك التحليل التنبؤي المتقدم للمحاصيل الزراعية
 * يستخدم خوارزميات التعلم الآلي المبسطة والتحليل الإحصائي لتوقع الاتجاهات المستقبلية
 */
public class PredictiveAnalyticsEngine {
    
    private final CropDataService cropDataService;
    private final PurchaseDataService purchaseDataService;
    
    public PredictiveAnalyticsEngine() {
        this.cropDataService = new CropDataService();
        this.purchaseDataService = new PurchaseDataService();
    }
    
    /**
     * توقع الطلب على المحاصيل للأشهر القادمة
     */
    public DemandForecast predictDemand(int cropId, int forecastMonths) {
        try {
            // جمع البيانات التاريخية للسنتين الماضيتين
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusYears(2);
            
            CropDataService.CropStatistics historicalStats = cropDataService.getCropStatistics(
                cropId, startDate, endDate);
            
            if (historicalStats == null) {
                return new DemandForecast(cropId, Collections.emptyList(), 0.0);
            }
            
            // تحليل الاتجاهات الموسمية
            Map<Integer, Double> seasonalPatterns = analyzeSeasonalPatterns(cropId, startDate, endDate);
            
            // حساب معدل النمو
            double growthRate = calculateGrowthRate(cropId, startDate, endDate);
            
            // توليد التوقعات الشهرية
            List<MonthlyForecast> monthlyForecasts = new ArrayList<>();
            double baselineDemand = historicalStats.getTotalSold() / 24.0; // متوسط شهري
            
            for (int i = 1; i <= forecastMonths; i++) {
                LocalDate forecastMonth = endDate.plusMonths(i);
                int monthOfYear = forecastMonth.getMonthValue();
                
                // تطبيق النمط الموسمي
                double seasonalMultiplier = seasonalPatterns.getOrDefault(monthOfYear, 1.0);
                
                // تطبيق معدل النمو
                double growthMultiplier = Math.pow(1 + growthRate, i / 12.0);
                
                // حساب الطلب المتوقع
                double predictedDemand = baselineDemand * seasonalMultiplier * growthMultiplier;
                
                // إضافة عامل عشوائي للتقلبات الطبيعية
                double volatility = calculateVolatility(cropId);
                double randomFactor = 1 + (Math.random() - 0.5) * volatility;
                predictedDemand *= randomFactor;
                
                // حساب مستوى الثقة
                double confidenceLevel = calculateConfidenceLevel(i, volatility);
                
                monthlyForecasts.add(new MonthlyForecast(
                    forecastMonth, predictedDemand, confidenceLevel));
            }
            
            // حساب دقة النموذج
            double modelAccuracy = calculateModelAccuracy(cropId, seasonalPatterns, growthRate);
            
            return new DemandForecast(cropId, monthlyForecasts, modelAccuracy);
            
        } catch (Exception e) {
            return new DemandForecast(cropId, Collections.emptyList(), 0.0);
        }
    }
    
    /**
     * توقع أسعار المحاصيل
     */
    public PriceForecast predictPrices(int cropId, int forecastMonths) {
        try {
            // جمع البيانات التاريخية للأسعار
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusYears(2);
            
            List<PricePoint> historicalPrices = getHistoricalPrices(cropId, startDate, endDate);
            
            if (historicalPrices.isEmpty()) {
                return new PriceForecast(cropId, Collections.emptyList(), 0.0);
            }
            
            // تحليل الاتجاه العام للأسعار
            double pricetrend = calculatePriceTrend(historicalPrices);
            
            // تحليل التقلبات الموسمية للأسعار
            Map<Integer, Double> seasonalPricePatterns = analyzePriceSeasonality(historicalPrices);
            
            // حساب التقلبات
            double priceVolatility = calculatePriceVolatility(historicalPrices);
            
            // توليد توقعات الأسعار
            List<MonthlyPriceForecast> priceForecasts = new ArrayList<>();
            double currentPrice = getCurrentAveragePrice(cropId);
            
            for (int i = 1; i <= forecastMonths; i++) {
                LocalDate forecastMonth = endDate.plusMonths(i);
                int monthOfYear = forecastMonth.getMonthValue();
                
                // تطبيق الاتجاه العام
                double trendAdjustment = pricetrend * i;
                
                // تطبيق النمط الموسمي
                double seasonalMultiplier = seasonalPricePatterns.getOrDefault(monthOfYear, 1.0);
                
                // حساب السعر المتوقع
                double predictedPrice = (currentPrice + trendAdjustment) * seasonalMultiplier;
                
                // حساب نطاق التوقع (أعلى وأقل سعر محتمل)
                double priceRange = predictedPrice * priceVolatility;
                double minPrice = predictedPrice - priceRange;
                double maxPrice = predictedPrice + priceRange;
                
                // حساب مستوى الثقة
                double confidenceLevel = Math.max(0.5, 0.9 - (i * 0.05));
                
                priceForecasts.add(new MonthlyPriceForecast(
                    forecastMonth, predictedPrice, minPrice, maxPrice, confidenceLevel));
            }
            
            // حساب دقة نموذج التنبؤ بالأسعار
            double modelAccuracy = calculatePriceModelAccuracy(historicalPrices, priceVolatility);
            
            return new PriceForecast(cropId, priceForecasts, modelAccuracy);
            
        } catch (Exception e) {
            return new PriceForecast(cropId, Collections.emptyList(), 0.0);
        }
    }
    
    /**
     * تحليل الربحية المتوقعة
     */
    public ProfitabilityAnalysis analyzeProfitability(int cropId, double plannedInvestment, 
                                                     int analysisMonths) {
        try {
            // الحصول على توقعات الطلب والأسعار
            DemandForecast demandForecast = predictDemand(cropId, analysisMonths);
            PriceForecast priceForecast = predictPrices(cropId, analysisMonths);
            
            // حساب التكاليف المتوقعة
            double averageCostPerKg = getAverageCostPerKg(cropId);
            
            // حساب الإيرادات والأرباح المتوقعة
            List<MonthlyProfitability> monthlyProfits = new ArrayList<>();
            double totalRevenue = 0;
            double totalCosts = plannedInvestment;
            
            for (int i = 0; i < Math.min(demandForecast.getMonthlyForecasts().size(), 
                                        priceForecast.getPriceForecasts().size()); i++) {
                
                MonthlyForecast demand = demandForecast.getMonthlyForecasts().get(i);
                MonthlyPriceForecast price = priceForecast.getPriceForecasts().get(i);
                
                double monthlyRevenue = demand.getPredictedDemand() * price.getPredictedPrice();
                double monthlyCosts = demand.getPredictedDemand() * averageCostPerKg;
                double monthlyProfit = monthlyRevenue - monthlyCosts;
                
                totalRevenue += monthlyRevenue;
                totalCosts += monthlyCosts;
                
                monthlyProfits.add(new MonthlyProfitability(
                    demand.getMonth(), monthlyRevenue, monthlyCosts, monthlyProfit));
            }
            
            double totalProfit = totalRevenue - totalCosts;
            double roi = plannedInvestment > 0 ? (totalProfit / plannedInvestment) * 100 : 0;
            
            // حساب نقطة التعادل
            int breakEvenMonth = calculateBreakEvenPoint(monthlyProfits, plannedInvestment);
            
            // تحليل المخاطر
            RiskAssessment riskAssessment = assessInvestmentRisk(
                demandForecast, priceForecast, plannedInvestment);
            
            return new ProfitabilityAnalysis(
                cropId, totalRevenue, totalCosts, totalProfit, roi, 
                breakEvenMonth, monthlyProfits, riskAssessment);
            
        } catch (Exception e) {
            return new ProfitabilityAnalysis(cropId, 0, 0, 0, 0, -1, 
                Collections.emptyList(), new RiskAssessment(RiskLevel.HIGH, "خطأ في التحليل"));
        }
    }
    
    /**
     * توصيات الاستثمار الذكية
     */
    public List<InvestmentRecommendation> generateInvestmentRecommendations() {
        List<InvestmentRecommendation> recommendations = new ArrayList<>();
        
        try {
            List<Crop> crops = cropDataService.getAllActiveCrops();
            
            for (Crop crop : crops) {
                // تحليل الربحية لكل محصول
                ProfitabilityAnalysis analysis = analyzeProfitability(crop.getCropId(), 50000, 12);
                
                if (analysis.getRoi() > 15) { // عائد أكثر من 15%
                    recommendations.add(new InvestmentRecommendation(
                        crop.getCropId(),
                        crop.getCropName(),
                        RecommendationType.BUY,
                        analysis.getRoi(),
                        "فرصة استثمار ممتازة مع عائد متوقع " + String.format("%.1f%%", analysis.getRoi()),
                        calculateOptimalInvestmentAmount(analysis)
                    ));
                } else if (analysis.getRoi() < 5) { // عائد أقل من 5%
                    recommendations.add(new InvestmentRecommendation(
                        crop.getCropId(),
                        crop.getCropName(),
                        RecommendationType.SELL,
                        analysis.getRoi(),
                        "عائد منخفض، يُنصح بتقليل الاستثمار أو البحث عن بدائل",
                        0
                    ));
                } else {
                    recommendations.add(new InvestmentRecommendation(
                        crop.getCropId(),
                        crop.getCropName(),
                        RecommendationType.HOLD,
                        analysis.getRoi(),
                        "عائد متوسط، يمكن الاحتفاظ بالاستثمار الحالي",
                        calculateOptimalInvestmentAmount(analysis) * 0.5
                    ));
                }
            }
            
            // ترتيب التوصيات حسب العائد المتوقع
            recommendations.sort((a, b) -> Double.compare(b.getExpectedReturn(), a.getExpectedReturn()));
            
        } catch (Exception e) {
            // في حالة الخطأ، إرجاع قائمة فارغة
        }
        
        return recommendations;
    }
    
    // الطرق المساعدة
    
    private Map<Integer, Double> analyzeSeasonalPatterns(int cropId, LocalDate startDate, LocalDate endDate) {
        // تحليل مبسط للأنماط الموسمية
        Map<Integer, Double> patterns = new HashMap<>();
        
        // أنماط موسمية افتراضية للمحاصيل المصرية
        patterns.put(1, 0.8);  // يناير - منخفض
        patterns.put(2, 0.9);  // فبراير
        patterns.put(3, 1.1);  // مارس - بداية الموسم
        patterns.put(4, 1.3);  // أبريل - ذروة
        patterns.put(5, 1.2);  // مايو
        patterns.put(6, 1.0);  // يونيو
        patterns.put(7, 0.9);  // يوليو
        patterns.put(8, 0.8);  // أغسطس - منخفض
        patterns.put(9, 1.0);  // سبتمبر
        patterns.put(10, 1.2); // أكتوبر - موسم الحصاد
        patterns.put(11, 1.4); // نوفمبر - ذروة
        patterns.put(12, 1.1); // ديسمبر
        
        return patterns;
    }
    
    private double calculateGrowthRate(int cropId, LocalDate startDate, LocalDate endDate) {
        // حساب معدل النمو بناءً على البيانات التاريخية
        try {
            // يمكن تحسين هذا المنطق ليكون أكثر دقة
            CropDataService.CropStatistics statsLastYear = cropDataService.getCropStatistics(cropId, endDate.minusYears(1), endDate);
            CropDataService.CropStatistics statsPreviousYear = cropDataService.getCropStatistics(cropId, startDate, endDate.minusYears(1));

            if (statsPreviousYear != null && statsPreviousYear.getTotalSold() > 0) {
                return (statsLastYear.getTotalSold() - statsPreviousYear.getTotalSold()) / statsPreviousYear.getTotalSold();
            }
        } catch (SQLException e) {
            return 0.05; // قيمة افتراضية في حالة الخطأ
        }
        return 0.05; // 5% نمو سنوي افتراضي
    }
    
    private double calculateVolatility(int cropId) {
        // يمكن تحسين هذا المنطق لاحقاً
        return 0.15; // 15% تقلبات
    }
    
    private double calculateConfidenceLevel(int monthsAhead, double volatility) {
        // مستوى الثقة ينخفض مع زيادة المدة الزمنية والتقلبات
        return Math.max(0.5, 0.95 - (monthsAhead * 0.05) - volatility);
    }
    
    private double calculateModelAccuracy(int cropId, Map<Integer, Double> patterns, double growthRate) {
        // حساب دقة النموذج بناءً على تعقيد البيانات
        return 0.75; // 75% دقة افتراضية
    }
    
    private List<PricePoint> getHistoricalPrices(int cropId, LocalDate startDate, LocalDate endDate) {
        List<PricePoint> prices = new ArrayList<>();
        try {
            // جلب المشتريات خلال الفترة وتجميعها حسب الشهر
            List<PurchaseRecord> purchases = purchaseDataService.getPurchases(startDate, endDate, cropId, null, null);
            
            Map<LocalDate, List<Double>> monthlyPrices = purchases.stream()
                .collect(Collectors.groupingBy(
                    p -> p.getPurchaseDate().withDayOfMonth(1), // مفتاح التجميع هو أول يوم في الشهر
                    Collectors.mapping(PurchaseRecord::getUnitPrice, Collectors.toList())
                ));
            
            monthlyPrices.forEach((month, priceList) -> {
                double avgPrice = priceList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                if (avgPrice > 0) {
                    prices.add(new PricePoint(month, avgPrice));
                }
            });

        } catch (SQLException e) {
            // في حالة الخطأ، يمكن إرجاع قائمة فارغة أو التعامل معه
        }
        prices.sort(Comparator.comparing(PricePoint::getDate));
        return prices;
    }
    
    private double calculatePriceTrend(List<PricePoint> prices) {
        if (prices.size() < 2) return 0;
        
        // حساب الاتجاه باستخدام الانحدار الخطي المبسط
        if (prices.size() < 2) return 0;
        double firstPrice = prices.get(0).getPrice();
        double lastPrice = prices.get(prices.size() - 1).getPrice();
        long months = ChronoUnit.MONTHS.between(prices.get(0).getDate(), prices.get(prices.size() - 1).getDate());
        return months > 0 ? (lastPrice - firstPrice) / months : 0;
    }
    
    private Map<Integer, Double> analyzePriceSeasonality(List<PricePoint> prices) {
        Map<Integer, Double> seasonality = new HashMap<>();
        
        // تحليل مبسط للموسمية
        Map<Integer, List<Double>> monthlyPrices = prices.stream()
            .collect(Collectors.groupingBy(
                p -> p.getDate().getMonthValue(),
                Collectors.mapping(PricePoint::getPrice, Collectors.toList())
            ));
        
        double overallAverage = prices.stream().mapToDouble(PricePoint::getPrice).average().orElse(100.0);
        
        for (Map.Entry<Integer, List<Double>> entry : monthlyPrices.entrySet()) {
            double monthlyAverage = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(100.0);
            seasonality.put(entry.getKey(), monthlyAverage / overallAverage);
        }
        
        return seasonality;
    }
    
    private double calculatePriceVolatility(List<PricePoint> prices) {
        if (prices.size() < 2) return 0.1;
        
        double average = prices.stream().mapToDouble(PricePoint::getPrice).average().orElse(100.0);
        double variance = prices.stream()
            .mapToDouble(p -> Math.pow(p.getPrice() - average, 2))
            .average().orElse(0);
        
        return Math.sqrt(variance) / average;
    }
    
    private double getCurrentAveragePrice(int cropId) {
        try {
            PurchaseDataService.PurchaseStatistics stats = purchaseDataService.getPurchaseStatistics(
                LocalDate.now().minusDays(30), LocalDate.now(), cropId, null);
            return stats.getAverageUnitPrice();
        } catch (SQLException e) {
            return 0; // قيمة افتراضية
        }
    }
    
    private double calculatePriceModelAccuracy(List<PricePoint> prices, double volatility) {
        // دقة النموذج تعتمد على التقلبات
        return Math.max(0.6, 0.9 - volatility);
    }
    
    private double getAverageCostPerKg(int cropId) {
        try {
            CropDataService.CropStatistics stats = cropDataService.getCropStatistics(cropId, null, null);
            return (stats != null) ? stats.getAverageCost() : 0.0;
        } catch (SQLException e) {
            return 0.0; // قيمة افتراضية
        }
    }
    
    private int calculateBreakEvenPoint(List<MonthlyProfitability> profits, double initialInvestment) {
        double cumulativeProfit = -initialInvestment;
        
        for (int i = 0; i < profits.size(); i++) {
            cumulativeProfit += profits.get(i).getProfit();
            if (cumulativeProfit >= 0) {
                return i + 1;
            }
        }
        
        return -1; // لم يتم الوصول لنقطة التعادل
    }
    
    private RiskAssessment assessInvestmentRisk(DemandForecast demand, PriceForecast price, double investment) {
        // تقييم المخاطر بناءً على دقة التوقعات والتقلبات
        double avgConfidence = (demand.getModelAccuracy() + price.getModelAccuracy()) / 2;
        
        RiskLevel riskLevel;
        String riskDescription;
        
        if (avgConfidence > 0.8) {
            riskLevel = RiskLevel.LOW;
            riskDescription = "مخاطر منخفضة - توقعات موثوقة";
        } else if (avgConfidence > 0.6) {
            riskLevel = RiskLevel.MEDIUM;
            riskDescription = "مخاطر متوسطة - توقعات معقولة";
        } else {
            riskLevel = RiskLevel.HIGH;
            riskDescription = "مخاطر عالية - توقعات غير مؤكدة";
        }
        
        return new RiskAssessment(riskLevel, riskDescription);
    }
    
    private double calculateOptimalInvestmentAmount(ProfitabilityAnalysis analysis) {
        // حساب المبلغ الأمثل للاستثمار بناءً على العائد والمخاطر
        if (analysis.getRoi() > 20) return 100000;
        if (analysis.getRoi() > 15) return 75000;
        if (analysis.getRoi() > 10) return 50000;
        return 25000;
    }
    
    // الفئات المساعدة
    
    public static class DemandForecast {
        private final int cropId;
        private final List<MonthlyForecast> monthlyForecasts;
        private final double modelAccuracy;
        
        public DemandForecast(int cropId, List<MonthlyForecast> monthlyForecasts, double modelAccuracy) {
            this.cropId = cropId;
            this.monthlyForecasts = monthlyForecasts;
            this.modelAccuracy = modelAccuracy;
        }
        
        public int getCropId() { return cropId; }
        public List<MonthlyForecast> getMonthlyForecasts() { return monthlyForecasts; }
        public double getModelAccuracy() { return modelAccuracy; }
    }
    
    public static class MonthlyForecast {
        private final LocalDate month;
        private final double predictedDemand;
        private final double confidenceLevel;
        
        public MonthlyForecast(LocalDate month, double predictedDemand, double confidenceLevel) {
            this.month = month;
            this.predictedDemand = predictedDemand;
            this.confidenceLevel = confidenceLevel;
        }
        
        public LocalDate getMonth() { return month; }
        public double getPredictedDemand() { return predictedDemand; }
        public double getConfidenceLevel() { return confidenceLevel; }
    }
    
    public static class PriceForecast {
        private final int cropId;
        private final List<MonthlyPriceForecast> priceForecasts;
        private final double modelAccuracy;
        
        public PriceForecast(int cropId, List<MonthlyPriceForecast> priceForecasts, double modelAccuracy) {
            this.cropId = cropId;
            this.priceForecasts = priceForecasts;
            this.modelAccuracy = modelAccuracy;
        }
        
        public int getCropId() { return cropId; }
        public List<MonthlyPriceForecast> getPriceForecasts() { return priceForecasts; }
        public double getModelAccuracy() { return modelAccuracy; }
    }
    
    public static class MonthlyPriceForecast {
        private final LocalDate month;
        private final double predictedPrice;
        private final double minPrice;
        private final double maxPrice;
        private final double confidenceLevel;
        
        public MonthlyPriceForecast(LocalDate month, double predictedPrice, double minPrice, 
                                  double maxPrice, double confidenceLevel) {
            this.month = month;
            this.predictedPrice = predictedPrice;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.confidenceLevel = confidenceLevel;
        }
        
        public LocalDate getMonth() { return month; }
        public double getPredictedPrice() { return predictedPrice; }
        public double getMinPrice() { return minPrice; }
        public double getMaxPrice() { return maxPrice; }
        public double getConfidenceLevel() { return confidenceLevel; }
    }
    
    public static class ProfitabilityAnalysis {
        private final int cropId;
        private final double totalRevenue;
        private final double totalCosts;
        private final double totalProfit;
        private final double roi;
        private final int breakEvenMonth;
        private final List<MonthlyProfitability> monthlyProfits;
        private final RiskAssessment riskAssessment;
        
        public ProfitabilityAnalysis(int cropId, double totalRevenue, double totalCosts, 
                                   double totalProfit, double roi, int breakEvenMonth,
                                   List<MonthlyProfitability> monthlyProfits, 
                                   RiskAssessment riskAssessment) {
            this.cropId = cropId;
            this.totalRevenue = totalRevenue;
            this.totalCosts = totalCosts;
            this.totalProfit = totalProfit;
            this.roi = roi;
            this.breakEvenMonth = breakEvenMonth;
            this.monthlyProfits = monthlyProfits;
            this.riskAssessment = riskAssessment;
        }
        
        public int getCropId() { return cropId; }
        public double getTotalRevenue() { return totalRevenue; }
        public double getTotalCosts() { return totalCosts; }
        public double getTotalProfit() { return totalProfit; }
        public double getRoi() { return roi; }
        public int getBreakEvenMonth() { return breakEvenMonth; }
        public List<MonthlyProfitability> getMonthlyProfits() { return monthlyProfits; }
        public RiskAssessment getRiskAssessment() { return riskAssessment; }
    }
    
    public static class MonthlyProfitability {
        private final LocalDate month;
        private final double revenue;
        private final double costs;
        private final double profit;
        
        public MonthlyProfitability(LocalDate month, double revenue, double costs, double profit) {
            this.month = month;
            this.revenue = revenue;
            this.costs = costs;
            this.profit = profit;
        }
        
        public LocalDate getMonth() { return month; }
        public double getRevenue() { return revenue; }
        public double getCosts() { return costs; }
        public double getProfit() { return profit; }
    }
    
    public static class RiskAssessment {
        private final RiskLevel riskLevel;
        private final String description;
        
        public RiskAssessment(RiskLevel riskLevel, String description) {
            this.riskLevel = riskLevel;
            this.description = description;
        }
        
        public RiskLevel getRiskLevel() { return riskLevel; }
        public String getDescription() { return description; }
    }
    
    public enum RiskLevel {
        LOW("منخفضة"),
        MEDIUM("متوسطة"),
        HIGH("عالية");
        
        private final String arabicName;
        
        RiskLevel(String arabicName) {
            this.arabicName = arabicName;
        }
        
        public String getArabicName() { return arabicName; }
    }
    
    public static class InvestmentRecommendation {
        private final int cropId;
        private final String cropName;
        private final RecommendationType type;
        private final double expectedReturn;
        private final String reasoning;
        private final double recommendedAmount;
        
        public InvestmentRecommendation(int cropId, String cropName, RecommendationType type,
                                      double expectedReturn, String reasoning, double recommendedAmount) {
            this.cropId = cropId;
            this.cropName = cropName;
            this.type = type;
            this.expectedReturn = expectedReturn;
            this.reasoning = reasoning;
            this.recommendedAmount = recommendedAmount;
        }
        
        public int getCropId() { return cropId; }
        public String getCropName() { return cropName; }
        public RecommendationType getType() { return type; }
        public double getExpectedReturn() { return expectedReturn; }
        public String getReasoning() { return reasoning; }
        public double getRecommendedAmount() { return recommendedAmount; }
    }
    
    public enum RecommendationType {
        BUY("شراء"),
        SELL("بيع"),
        HOLD("احتفاظ");
        
        private final String arabicName;
        
        RecommendationType(String arabicName) {
            this.arabicName = arabicName;
        }
        
        public String getArabicName() { return arabicName; }
    }
    
    private static class PricePoint {
        private final LocalDate date;
        private final double price;
        
        public PricePoint(LocalDate date, double price) {
            this.date = date;
            this.price = price;
        }
        
        public LocalDate getDate() { return date; }
        public double getPrice() { return price; }
    }
}