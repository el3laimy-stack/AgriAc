package accounting.util;

import accounting.model.*;
import accounting.service.CropDataService;
import accounting.service.PurchaseDataService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * مولد التقارير الذكية والتحليلات المتقدمة
 * ينتج تقارير شاملة ومخصصة مع رؤى تحليلية عميقة
 */
public class SmartReportGenerator {
    
    private final CropDataService cropDataService;
    private final PurchaseDataService purchaseDataService;
    private final PredictiveAnalyticsEngine analyticsEngine;
    private final AdvancedProfitabilityCalculator profitabilityCalculator;
    
    public SmartReportGenerator() {
        this.cropDataService = new CropDataService();
        this.purchaseDataService = new PurchaseDataService();
        this.analyticsEngine = new PredictiveAnalyticsEngine();
        this.profitabilityCalculator = new AdvancedProfitabilityCalculator();
    }
    
    /**
     * إنتاج تقرير الأداء الشامل
     */
    public ComprehensivePerformanceReport generatePerformanceReport(LocalDate fromDate, LocalDate toDate) {
        try {
            // جمع البيانات الأساسية
            List<Crop> crops = cropDataService.getAllActiveCrops();
            
            // تحليل الأداء لكل محصول
            List<CropPerformanceAnalysis> cropAnalyses = new ArrayList<>();
            for (Crop crop : crops) {
                CropPerformanceAnalysis analysis = analyzeCropPerformance(crop, fromDate, toDate);
                cropAnalyses.add(analysis);
            }
            
            // تحليل الاتجاهات العامة
            MarketTrendsAnalysis marketTrends = analyzeMarketTrends(cropAnalyses, fromDate, toDate);
            
            // تحليل الربحية الإجمالية
            OverallProfitabilityAnalysis profitability = analyzeOverallProfitability(cropAnalyses);
            
            // توصيات استراتيجية
            List<StrategicRecommendation> recommendations = generateStrategicRecommendations(
                cropAnalyses, marketTrends, profitability);
            
            // مؤشرات الأداء الرئيسية
            KeyPerformanceIndicators kpis = calculateKPIs(cropAnalyses, fromDate, toDate);
            
            return new ComprehensivePerformanceReport(
                fromDate, toDate, cropAnalyses, marketTrends, profitability, 
                recommendations, kpis, LocalDate.now());
                
        } catch (Exception e) {
            throw new RuntimeException("خطأ في إنتاج تقرير الأداء: " + e.getMessage(), e);
        }
    }
    
    /**
     * إنتاج تقرير التوقعات والتنبؤات
     */
    public ForecastReport generateForecastReport(int forecastMonths) {
        try {
            List<Crop> crops = cropDataService.getAllActiveCrops();
            List<CropForecast> cropForecasts = new ArrayList<>();
            
            for (Crop crop : crops) {
                // توقعات الطلب والأسعار
                PredictiveAnalyticsEngine.DemandForecast demandForecast = 
                    analyticsEngine.predictDemand(crop.getCropId(), forecastMonths);
                PredictiveAnalyticsEngine.PriceForecast priceForecast = 
                    analyticsEngine.predictPrices(crop.getCropId(), forecastMonths);
                
                // تحليل الربحية المتوقعة
                PredictiveAnalyticsEngine.ProfitabilityAnalysis profitabilityAnalysis = 
                    analyticsEngine.analyzeProfitability(crop.getCropId(), 50000, forecastMonths);
                
                cropForecasts.add(new CropForecast(
                    crop, demandForecast, priceForecast, profitabilityAnalysis));
            }
            
            // توصيات الاستثمار
            List<PredictiveAnalyticsEngine.InvestmentRecommendation> investmentRecommendations = 
                analyticsEngine.generateInvestmentRecommendations();
            
            // تحليل المخاطر المستقبلية
            FutureRiskAssessment riskAssessment = assessFutureRisks(cropForecasts);
            
            // فرص السوق
            List<MarketOpportunity> marketOpportunities = identifyMarketOpportunities(cropForecasts);
            
            return new ForecastReport(
                forecastMonths, cropForecasts, investmentRecommendations, 
                riskAssessment, marketOpportunities, LocalDate.now());
                
        } catch (Exception e) {
            throw new RuntimeException("خطأ في إنتاج تقرير التوقعات: " + e.getMessage(), e);
        }
    }
    
    /**
     * إنتاج تقرير مقارنة الربحية
     */
    public ProfitabilityComparisonReport generateProfitabilityReport(
            List<Integer> cropIds, double investmentPerCrop, int analysisMonths) {
        
        try {
            // حساب الربحية لكل محصول
            AdvancedProfitabilityCalculator.ProfitabilityComparison comparison = 
                profitabilityCalculator.compareCropProfitability(cropIds, investmentPerCrop, analysisMonths);
            
            // تحليل المخاطر والعوائد
            RiskReturnAnalysis riskReturnAnalysis = analyzeRiskReturn(comparison.getProfitabilities());
            
            // توصيات التنويع
            DiversificationRecommendations diversificationRecommendations = 
                generateDiversificationRecommendations(comparison.getDiversificationAnalysis());
            
            // تحليل الحساسية الإجمالي
            OverallSensitivityAnalysis sensitivityAnalysis = 
                analyzeOverallSensitivity(comparison.getProfitabilities());
            
            return new ProfitabilityComparisonReport(
                comparison, riskReturnAnalysis, diversificationRecommendations, 
                sensitivityAnalysis, LocalDate.now());
                
        } catch (Exception e) {
            throw new RuntimeException("خطأ في إنتاج تقرير مقارنة الربحية: " + e.getMessage(), e);
        }
    }
    
    /**
     * إنتاج تقرير التنبيهات والتوصيات
     */
    public AlertsAndRecommendationsReport generateAlertsReport() {
        try {
            SmartAlertSystem alertSystem = new SmartAlertSystem();
            
            // الحصول على التنبيهات النشطة
            List<SmartAlertSystem.SmartAlert> activeAlerts = alertSystem.getAllActiveAlerts();
            
            // تصنيف التنبيهات
            Map<SmartAlertSystem.AlertType, List<SmartAlertSystem.SmartAlert>> categorizedAlerts = 
                activeAlerts.stream().collect(Collectors.groupingBy(SmartAlertSystem.SmartAlert::getType));
            
            // تحليل أولويات التنبيهات
            AlertPriorityAnalysis priorityAnalysis = analyzeAlertPriorities(activeAlerts);
            
            // توصيات الإجراءات
            List<ActionRecommendation> actionRecommendations = 
                generateActionRecommendations(activeAlerts);
            
            // تحليل الاتجاهات في التنبيهات
            AlertTrendsAnalysis trendsAnalysis = analyzeAlertTrends(activeAlerts);
            
            return new AlertsAndRecommendationsReport(
                activeAlerts, categorizedAlerts, priorityAnalysis, 
                actionRecommendations, trendsAnalysis, LocalDate.now());
                
        } catch (Exception e) {
            throw new RuntimeException("خطأ في إنتاج تقرير التنبيهات: " + e.getMessage(), e);
        }
    }
    
    /**
     * إنتاج تقرير مخصص
     */
    public CustomReport generateCustomReport(ReportConfiguration config) {
        try {
            CustomReportBuilder builder = new CustomReportBuilder(config);
            
            // إضافة الأقسام المطلوبة
            if (config.includePerformanceAnalysis()) {
                ComprehensivePerformanceReport performanceReport = 
                    generatePerformanceReport(config.getFromDate(), config.getToDate());
                builder.addPerformanceSection(performanceReport);
            }
            
            if (config.includeForecastAnalysis()) {
                ForecastReport forecastReport = generateForecastReport(config.getForecastMonths());
                builder.addForecastSection(forecastReport);
            }
            
            if (config.includeProfitabilityAnalysis()) {
                ProfitabilityComparisonReport profitabilityReport = 
                    generateProfitabilityReport(config.getCropIds(), 
                        config.getInvestmentAmount(), config.getAnalysisMonths());
                builder.addProfitabilitySection(profitabilityReport);
            }
            
            if (config.includeAlertsAnalysis()) {
                AlertsAndRecommendationsReport alertsReport = generateAlertsReport();
                builder.addAlertsSection(alertsReport);
            }
            
            // إضافة تحليلات مخصصة
            if (config.getCustomAnalyses() != null) {
                for (String analysisType : config.getCustomAnalyses()) {
                    builder.addCustomAnalysis(analysisType, performCustomAnalysis(analysisType, config));
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            throw new RuntimeException("خطأ في إنتاج التقرير المخصص: " + e.getMessage(), e);
        }
    }
    
    // الطرق المساعدة
    
    private CropPerformanceAnalysis analyzeCropPerformance(Crop crop, LocalDate fromDate, LocalDate toDate) {
        try {
            // الحصول على الإحصائيات
            CropDataService.CropStatistics stats = cropDataService.getCropStatistics(
                crop.getCropId(), fromDate, toDate);
            
            if (stats == null) {
                return new CropPerformanceAnalysis(crop, 0, 0, 0, 0, 0, 
                    PerformanceRating.POOR, Collections.emptyList());
            }
            
            // حساب المؤشرات
            double totalRevenue = stats.getTotalSold() * stats.getAverageSellingPrice();
            double totalCosts = stats.getTotalPurchased() * stats.getAveragePurchasePrice();
            double profit = totalRevenue - totalCosts;
            double profitMargin = totalRevenue > 0 ? (profit / totalRevenue) * 100 : 0;
            double roi = totalCosts > 0 ? (profit / totalCosts) * 100 : 0;
            
            // تقييم الأداء
            PerformanceRating rating = evaluatePerformance(profitMargin, roi, stats.getTurnoverRate());
            
            // تحديد نقاط التحسين
            List<String> improvementAreas = identifyImprovementAreas(stats, profitMargin, roi);
            
            return new CropPerformanceAnalysis(
                crop, totalRevenue, totalCosts, profit, profitMargin, roi, rating, improvementAreas);
                
        } catch (Exception e) {
            return new CropPerformanceAnalysis(crop, 0, 0, 0, 0, 0, 
                PerformanceRating.POOR, Arrays.asList("خطأ في تحليل البيانات"));
        }
    }
    
    private MarketTrendsAnalysis analyzeMarketTrends(List<CropPerformanceAnalysis> analyses, 
                                                   LocalDate fromDate, LocalDate toDate) {
        
        // تحليل الاتجاهات العامة
        double averageProfitMargin = analyses.stream()
            .mapToDouble(CropPerformanceAnalysis::getProfitMargin)
            .average().orElse(0);
        
        double averageROI = analyses.stream()
            .mapToDouble(CropPerformanceAnalysis::getRoi)
            .average().orElse(0);
        
        // تحديد المحاصيل الأكثر ربحية
        List<String> topPerformingCrops = analyses.stream()
            .sorted((a, b) -> Double.compare(b.getRoi(), a.getRoi()))
            .limit(3)
            .map(a -> a.getCrop().getCropName())
            .collect(Collectors.toList());
        
        // تحديد المحاصيل الأقل أداءً
        List<String> underPerformingCrops = analyses.stream()
            .filter(a -> a.getRating() == PerformanceRating.POOR)
            .map(a -> a.getCrop().getCropName())
            .collect(Collectors.toList());
        
        // تحليل الاتجاهات الموسمية
        Map<String, Double> seasonalTrends = analyzeSeasonalTrends(fromDate, toDate);
        
        return new MarketTrendsAnalysis(
            averageProfitMargin, averageROI, topPerformingCrops, 
            underPerformingCrops, seasonalTrends);
    }
    
    private OverallProfitabilityAnalysis analyzeOverallProfitability(List<CropPerformanceAnalysis> analyses) {
        
        double totalRevenue = analyses.stream().mapToDouble(CropPerformanceAnalysis::getTotalRevenue).sum();
        double totalCosts = analyses.stream().mapToDouble(CropPerformanceAnalysis::getTotalCosts).sum();
        double totalProfit = totalRevenue - totalCosts;
        double overallProfitMargin = totalRevenue > 0 ? (totalProfit / totalRevenue) * 100 : 0;
        double overallROI = totalCosts > 0 ? (totalProfit / totalCosts) * 100 : 0;
        
        // تحليل توزيع الربحية
        Map<String, Double> profitabilityDistribution = analyses.stream()
            .collect(Collectors.toMap(
                a -> a.getCrop().getCropName(),
                CropPerformanceAnalysis::getProfitMargin
            ));
        
        // تحديد مصادر الربح الرئيسية
        List<String> mainProfitSources = analyses.stream()
            .filter(a -> a.getProfitMargin() > overallProfitMargin)
            .map(a -> a.getCrop().getCropName())
            .collect(Collectors.toList());
        
        return new OverallProfitabilityAnalysis(
            totalRevenue, totalCosts, totalProfit, overallProfitMargin, 
            overallROI, profitabilityDistribution, mainProfitSources);
    }
    
    private List<StrategicRecommendation> generateStrategicRecommendations(
            List<CropPerformanceAnalysis> analyses, MarketTrendsAnalysis trends, 
            OverallProfitabilityAnalysis profitability) {
        
        List<StrategicRecommendation> recommendations = new ArrayList<>();
        
        // توصيات بناءً على الأداء
        if (profitability.getOverallROI() < 10) {
            recommendations.add(new StrategicRecommendation(
                RecommendationType.STRATEGIC,
                "تحسين الربحية الإجمالية",
                "العائد على الاستثمار الإجمالي منخفض (" + 
                String.format("%.1f%%", profitability.getOverallROI()) + ")",
                "مراجعة استراتيجية التسعير وتحسين كفاءة العمليات",
                RecommendationPriority.HIGH
            ));
        }
        
        // توصيات للمحاصيل عالية الأداء
        for (String crop : trends.getTopPerformingCrops()) {
            recommendations.add(new StrategicRecommendation(
                RecommendationType.INVESTMENT,
                "زيادة الاستثمار في " + crop,
                "يحقق هذا المحصول أداءً ممتازاً",
                "زيادة حجم الاستثمار في هذا المحصول لتعظيم الأرباح",
                RecommendationPriority.MEDIUM
            ));
        }
        
        // توصيات للمحاصيل ضعيفة الأداء
        for (String crop : trends.getUnderPerformingCrops()) {
            recommendations.add(new StrategicRecommendation(
                RecommendationType.RISK_MANAGEMENT,
                "مراجعة استراتيجية " + crop,
                "أداء ضعيف يتطلب تدخلاً",
                "دراسة إمكانية تحسين الأداء أو تقليل الاستثمار",
                RecommendationPriority.HIGH
            ));
        }
        
        return recommendations;
    }
    
    private KeyPerformanceIndicators calculateKPIs(List<CropPerformanceAnalysis> analyses, 
                                                 LocalDate fromDate, LocalDate toDate) {
        
        int totalCrops = analyses.size();
        int profitableCrops = (int) analyses.stream()
            .filter(a -> a.getProfitMargin() > 0)
            .count();
        
        double averageROI = analyses.stream()
            .mapToDouble(CropPerformanceAnalysis::getRoi)
            .average().orElse(0);
        
        double totalRevenue = analyses.stream()
            .mapToDouble(CropPerformanceAnalysis::getTotalRevenue)
            .sum();
        
        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate);
        double revenuePerDay = periodDays > 0 ? totalRevenue / periodDays : 0;
        
        return new KeyPerformanceIndicators(
            totalCrops, profitableCrops, averageROI, totalRevenue, revenuePerDay);
    }
    
    private PerformanceRating evaluatePerformance(double profitMargin, double roi, double turnoverRate) {
        double score = (profitMargin * 0.4) + (roi * 0.4) + (turnoverRate * 0.2);
        
        if (score >= 20) return PerformanceRating.EXCELLENT;
        if (score >= 15) return PerformanceRating.GOOD;
        if (score >= 10) return PerformanceRating.AVERAGE;
        if (score >= 5) return PerformanceRating.BELOW_AVERAGE;
        return PerformanceRating.POOR;
    }
    
    private List<String> identifyImprovementAreas(CropDataService.CropStatistics stats, 
                                                double profitMargin, double roi) {
        List<String> areas = new ArrayList<>();
        
        if (profitMargin < 10) areas.add("تحسين هامش الربح");
        if (roi < 15) areas.add("زيادة العائد على الاستثمار");
        if (stats.getTurnoverRate() < 2) areas.add("تحسين معدل دوران المخزون");
        if (stats.getCurrentStock() > stats.getTotalSold() * 0.5) areas.add("تحسين إدارة المخزون");
        
        return areas;
    }
    
    private Map<String, Double> analyzeSeasonalTrends(LocalDate fromDate, LocalDate toDate) {
        // تحليل مبسط للاتجاهات الموسمية
        Map<String, Double> trends = new HashMap<>();
        trends.put("الربيع", 1.1);
        trends.put("الصيف", 0.9);
        trends.put("الخريف", 1.2);
        trends.put("الشتاء", 0.8);
        return trends;
    }
    
    private FutureRiskAssessment assessFutureRisks(List<CropForecast> forecasts) {
        // تقييم المخاطر المستقبلية
        double averageRisk = forecasts.stream()
            .mapToDouble(f -> f.getProfitabilityAnalysis().getRiskAssessment().getRiskLevel().ordinal())
            .average().orElse(1.0);
        
        List<String> riskFactors = Arrays.asList(
            "تقلبات أسعار السوق",
            "تغيرات الطلب",
            "مخاطر الطقس",
            "المنافسة"
        );
        
        return new FutureRiskAssessment(averageRisk, riskFactors);
    }
    
    private List<MarketOpportunity> identifyMarketOpportunities(List<CropForecast> forecasts) {
        List<MarketOpportunity> opportunities = new ArrayList<>();
        
        for (CropForecast forecast : forecasts) {
            if (forecast.getProfitabilityAnalysis().getRoi() > 20) {
                opportunities.add(new MarketOpportunity(
                    forecast.getCrop().getCropName(),
                    "فرصة استثمار عالية العائد",
                    forecast.getProfitabilityAnalysis().getRoi()
                ));
            }
        }
        
        return opportunities;
    }
    
    private RiskReturnAnalysis analyzeRiskReturn(List<AdvancedProfitabilityCalculator.ComprehensiveProfitability> profitabilities) {
        // تحليل العلاقة بين المخاطر والعوائد
        Map<String, Double> riskReturnMap = profitabilities.stream()
            .collect(Collectors.toMap(
                p -> p.getCrop().getCropName(),
                p -> p.getMetrics().getRoi() / (p.getRiskProfile().getOverallRisk() + 0.1)
            ));
        
        return new RiskReturnAnalysis(riskReturnMap);
    }
    
    private DiversificationRecommendations generateDiversificationRecommendations(
            AdvancedProfitabilityCalculator.DiversificationAnalysis analysis) {
        
        List<String> recommendations = new ArrayList<>();
        
        if (analysis.getAverageCorrelation() > 0.7) {
            recommendations.add("تنويع المحفظة لتقليل الارتباط بين المحاصيل");
        }
        
        if (analysis.getRiskReduction() < 0.2) {
            recommendations.add("إضافة محاصيل جديدة لتحسين التنويع");
        }
        
        return new DiversificationRecommendations(recommendations, analysis.getOptimalPortfolio());
    }
    
    private OverallSensitivityAnalysis analyzeOverallSensitivity(
            List<AdvancedProfitabilityCalculator.ComprehensiveProfitability> profitabilities) {
        
        // تحليل الحساسية الإجمالي
        double avgPriceSensitivity = profitabilities.stream()
            .flatMap(p -> p.getSensitivity().getPriceSensitivity().values().stream())
            .mapToDouble(Double::doubleValue)
            .average().orElse(0);
        
        return new OverallSensitivityAnalysis(avgPriceSensitivity);
    }
    
    private AlertPriorityAnalysis analyzeAlertPriorities(List<SmartAlertSystem.SmartAlert> alerts) {
        Map<SmartAlertSystem.AlertPriority, Long> priorityCounts = alerts.stream()
            .collect(Collectors.groupingBy(
                SmartAlertSystem.SmartAlert::getPriority,
                Collectors.counting()
            ));
        
        return new AlertPriorityAnalysis(priorityCounts);
    }
    
    private List<ActionRecommendation> generateActionRecommendations(List<SmartAlertSystem.SmartAlert> alerts) {
        return alerts.stream()
            .filter(alert -> alert.getPriority() == SmartAlertSystem.AlertPriority.HIGH || 
                           alert.getPriority() == SmartAlertSystem.AlertPriority.CRITICAL)
            .map(alert -> new ActionRecommendation(
                alert.getTitle(),
                alert.getRecommendation(),
                alert.getPriority().getArabicName()
            ))
            .collect(Collectors.toList());
    }
    
    private AlertTrendsAnalysis analyzeAlertTrends(List<SmartAlertSystem.SmartAlert> alerts) {
        // تحليل اتجاهات التنبيهات
        Map<SmartAlertSystem.AlertType, Long> typeCounts = alerts.stream()
            .collect(Collectors.groupingBy(
                SmartAlertSystem.SmartAlert::getType,
                Collectors.counting()
            ));
        
        return new AlertTrendsAnalysis(typeCounts);
    }
    
    private Object performCustomAnalysis(String analysisType, ReportConfiguration config) {
        // تنفيذ تحليلات مخصصة
        switch (analysisType) {
            case "SEASONAL_ANALYSIS":
                return analyzeSeasonalTrends(config.getFromDate(), config.getToDate());
            case "INVENTORY_OPTIMIZATION":
                return "تحليل تحسين المخزون";
            default:
                return "تحليل مخصص غير محدد";
        }
    }
    
    // الفئات المساعدة
    
    public enum PerformanceRating {
        EXCELLENT("ممتاز"),
        GOOD("جيد"),
        AVERAGE("متوسط"),
        BELOW_AVERAGE("أقل من المتوسط"),
        POOR("ضعيف");
        
        private final String arabicName;
        
        PerformanceRating(String arabicName) {
            this.arabicName = arabicName;
        }
        
        public String getArabicName() { return arabicName; }
    }
    
    public enum RecommendationType {
        STRATEGIC("استراتيجية"),
        INVESTMENT("استثمار"),
        OPERATIONAL("تشغيلية"),
        RISK_MANAGEMENT("إدارة مخاطر");
        
        private final String arabicName;
        
        RecommendationType(String arabicName) {
            this.arabicName = arabicName;
        }
        
        public String getArabicName() { return arabicName; }
    }
    
    public enum RecommendationPriority {
        LOW("منخفضة"),
        MEDIUM("متوسطة"),
        HIGH("عالية"),
        CRITICAL("حرجة");
        
        private final String arabicName;
        
        RecommendationPriority(String arabicName) {
            this.arabicName = arabicName;
        }
        
        public String getArabicName() { return arabicName; }
    }
    
    // فئات التقارير والتحليلات
    
    public static class ComprehensivePerformanceReport {
        private final LocalDate fromDate;
        private final LocalDate toDate;
        private final List<CropPerformanceAnalysis> cropAnalyses;
        private final MarketTrendsAnalysis marketTrends;
        private final OverallProfitabilityAnalysis profitability;
        private final List<StrategicRecommendation> recommendations;
        private final KeyPerformanceIndicators kpis;
        private final LocalDate generatedDate;
        
        public ComprehensivePerformanceReport(LocalDate fromDate, LocalDate toDate,
                                            List<CropPerformanceAnalysis> cropAnalyses,
                                            MarketTrendsAnalysis marketTrends,
                                            OverallProfitabilityAnalysis profitability,
                                            List<StrategicRecommendation> recommendations,
                                            KeyPerformanceIndicators kpis,
                                            LocalDate generatedDate) {
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.cropAnalyses = cropAnalyses;
            this.marketTrends = marketTrends;
            this.profitability = profitability;
            this.recommendations = recommendations;
            this.kpis = kpis;
            this.generatedDate = generatedDate;
        }
        
        // Getters
        public LocalDate getFromDate() { return fromDate; }
        public LocalDate getToDate() { return toDate; }
        public List<CropPerformanceAnalysis> getCropAnalyses() { return cropAnalyses; }
        public MarketTrendsAnalysis getMarketTrends() { return marketTrends; }
        public OverallProfitabilityAnalysis getProfitability() { return profitability; }
        public List<StrategicRecommendation> getRecommendations() { return recommendations; }
        public KeyPerformanceIndicators getKpis() { return kpis; }
        public LocalDate getGeneratedDate() { return generatedDate; }
    }
    
    public static class CropPerformanceAnalysis {
        private final Crop crop;
        private final double totalRevenue;
        private final double totalCosts;
        private final double profit;
        private final double profitMargin;
        private final double roi;
        private final PerformanceRating rating;
        private final List<String> improvementAreas;
        
        public CropPerformanceAnalysis(Crop crop, double totalRevenue, double totalCosts,
                                     double profit, double profitMargin, double roi,
                                     PerformanceRating rating, List<String> improvementAreas) {
            this.crop = crop;
            this.totalRevenue = totalRevenue;
            this.totalCosts = totalCosts;
            this.profit = profit;
            this.profitMargin = profitMargin;
            this.roi = roi;
            this.rating = rating;
            this.improvementAreas = improvementAreas;
        }
        
        // Getters
        public Crop getCrop() { return crop; }
        public double getTotalRevenue() { return totalRevenue; }
        public double getTotalCosts() { return totalCosts; }
        public double getProfit() { return profit; }
        public double getProfitMargin() { return profitMargin; }
        public double getRoi() { return roi; }
        public PerformanceRating getRating() { return rating; }
        public List<String> getImprovementAreas() { return improvementAreas; }
    }
    
    public static class MarketTrendsAnalysis {
        private final double averageProfitMargin;
        private final double averageROI;
        private final List<String> topPerformingCrops;
        private final List<String> underPerformingCrops;
        private final Map<String, Double> seasonalTrends;
        
        public MarketTrendsAnalysis(double averageProfitMargin, double averageROI,
                                  List<String> topPerformingCrops, List<String> underPerformingCrops,
                                  Map<String, Double> seasonalTrends) {
            this.averageProfitMargin = averageProfitMargin;
            this.averageROI = averageROI;
            this.topPerformingCrops = topPerformingCrops;
            this.underPerformingCrops = underPerformingCrops;
            this.seasonalTrends = seasonalTrends;
        }
        
        // Getters
        public double getAverageProfitMargin() { return averageProfitMargin; }
        public double getAverageROI() { return averageROI; }
        public List<String> getTopPerformingCrops() { return topPerformingCrops; }
        public List<String> getUnderPerformingCrops() { return underPerformingCrops; }
        public Map<String, Double> getSeasonalTrends() { return seasonalTrends; }
    }
    
    public static class OverallProfitabilityAnalysis {
        private final double totalRevenue;
        private final double totalCosts;
        private final double totalProfit;
        private final double overallProfitMargin;
        private final double overallROI;
        private final Map<String, Double> profitabilityDistribution;
        private final List<String> mainProfitSources;
        
        public OverallProfitabilityAnalysis(double totalRevenue, double totalCosts, double totalProfit,
                                          double overallProfitMargin, double overallROI,
                                          Map<String, Double> profitabilityDistribution,
                                          List<String> mainProfitSources) {
            this.totalRevenue = totalRevenue;
            this.totalCosts = totalCosts;
            this.totalProfit = totalProfit;
            this.overallProfitMargin = overallProfitMargin;
            this.overallROI = overallROI;
            this.profitabilityDistribution = profitabilityDistribution;
            this.mainProfitSources = mainProfitSources;
        }
        
        // Getters
        public double getTotalRevenue() { return totalRevenue; }
        public double getTotalCosts() { return totalCosts; }
        public double getTotalProfit() { return totalProfit; }
        public double getOverallProfitMargin() { return overallProfitMargin; }
        public double getOverallROI() { return overallROI; }
        public Map<String, Double> getProfitabilityDistribution() { return profitabilityDistribution; }
        public List<String> getMainProfitSources() { return mainProfitSources; }
    }
    
    public static class StrategicRecommendation {
        private final RecommendationType type;
        private final String title;
        private final String rationale;
        private final String action;
        private final RecommendationPriority priority;
        
        public StrategicRecommendation(RecommendationType type, String title, String rationale,
                                     String action, RecommendationPriority priority) {
            this.type = type;
            this.title = title;
            this.rationale = rationale;
            this.action = action;
            this.priority = priority;
        }
        
        // Getters
        public RecommendationType getType() { return type; }
        public String getTitle() { return title; }
        public String getRationale() { return rationale; }
        public String getAction() { return action; }
        public RecommendationPriority getPriority() { return priority; }
    }
    
    public static class KeyPerformanceIndicators {
        private final int totalCrops;
        private final int profitableCrops;
        private final double averageROI;
        private final double totalRevenue;
        private final double revenuePerDay;
        
        public KeyPerformanceIndicators(int totalCrops, int profitableCrops, double averageROI,
                                      double totalRevenue, double revenuePerDay) {
            this.totalCrops = totalCrops;
            this.profitableCrops = profitableCrops;
            this.averageROI = averageROI;
            this.totalRevenue = totalRevenue;
            this.revenuePerDay = revenuePerDay;
        }
        
        // Getters
        public int getTotalCrops() { return totalCrops; }
        public int getProfitableCrops() { return profitableCrops; }
        public double getAverageROI() { return averageROI; }
        public double getTotalRevenue() { return totalRevenue; }
        public double getRevenuePerDay() { return revenuePerDay; }
        
        public double getProfitabilityRate() {
            return totalCrops > 0 ? (double) profitableCrops / totalCrops * 100 : 0;
        }
    }
    
    // باقي الفئات المساعدة...
    
    public static class ForecastReport {
        private final int forecastMonths;
        private final List<CropForecast> cropForecasts;
        private final List<PredictiveAnalyticsEngine.InvestmentRecommendation> investmentRecommendations;
        private final FutureRiskAssessment riskAssessment;
        private final List<MarketOpportunity> marketOpportunities;
        private final LocalDate generatedDate;
        
        public ForecastReport(int forecastMonths, List<CropForecast> cropForecasts,
                            List<PredictiveAnalyticsEngine.InvestmentRecommendation> investmentRecommendations,
                            FutureRiskAssessment riskAssessment, List<MarketOpportunity> marketOpportunities,
                            LocalDate generatedDate) {
            this.forecastMonths = forecastMonths;
            this.cropForecasts = cropForecasts;
            this.investmentRecommendations = investmentRecommendations;
            this.riskAssessment = riskAssessment;
            this.marketOpportunities = marketOpportunities;
            this.generatedDate = generatedDate;
        }
        
        // Getters
        public int getForecastMonths() { return forecastMonths; }
        public List<CropForecast> getCropForecasts() { return cropForecasts; }
        public List<PredictiveAnalyticsEngine.InvestmentRecommendation> getInvestmentRecommendations() { return investmentRecommendations; }
        public FutureRiskAssessment getRiskAssessment() { return riskAssessment; }
        public List<MarketOpportunity> getMarketOpportunities() { return marketOpportunities; }
        public LocalDate getGeneratedDate() { return generatedDate; }
    }
    
    public static class CropForecast {
        private final Crop crop;
        private final PredictiveAnalyticsEngine.DemandForecast demandForecast;
        private final PredictiveAnalyticsEngine.PriceForecast priceForecast;
        private final PredictiveAnalyticsEngine.ProfitabilityAnalysis profitabilityAnalysis;
        
        public CropForecast(Crop crop, PredictiveAnalyticsEngine.DemandForecast demandForecast,
                          PredictiveAnalyticsEngine.PriceForecast priceForecast,
                          PredictiveAnalyticsEngine.ProfitabilityAnalysis profitabilityAnalysis) {
            this.crop = crop;
            this.demandForecast = demandForecast;
            this.priceForecast = priceForecast;
            this.profitabilityAnalysis = profitabilityAnalysis;
        }
        
        // Getters
        public Crop getCrop() { return crop; }
        public PredictiveAnalyticsEngine.DemandForecast getDemandForecast() { return demandForecast; }
        public PredictiveAnalyticsEngine.PriceForecast getPriceForecast() { return priceForecast; }
        public PredictiveAnalyticsEngine.ProfitabilityAnalysis getProfitabilityAnalysis() { return profitabilityAnalysis; }
    }
    
    public static class FutureRiskAssessment {
        private final double averageRisk;
        private final List<String> riskFactors;
        
        public FutureRiskAssessment(double averageRisk, List<String> riskFactors) {
            this.averageRisk = averageRisk;
            this.riskFactors = riskFactors;
        }
        
        // Getters
        public double getAverageRisk() { return averageRisk; }
        public List<String> getRiskFactors() { return riskFactors; }
    }
    
    public static class MarketOpportunity {
        private final String cropName;
        private final String description;
        private final double expectedReturn;
        
        public MarketOpportunity(String cropName, String description, double expectedReturn) {
            this.cropName = cropName;
            this.description = description;
            this.expectedReturn = expectedReturn;
        }
        
        // Getters
        public String getCropName() { return cropName; }
        public String getDescription() { return description; }
        public double getExpectedReturn() { return expectedReturn; }
    }
    
    // فئات إضافية للتقارير الأخرى...
    
    public static class ProfitabilityComparisonReport {
        private final AdvancedProfitabilityCalculator.ProfitabilityComparison comparison;
        private final RiskReturnAnalysis riskReturnAnalysis;
        private final DiversificationRecommendations diversificationRecommendations;
        private final OverallSensitivityAnalysis sensitivityAnalysis;
        private final LocalDate generatedDate;
        
        public ProfitabilityComparisonReport(AdvancedProfitabilityCalculator.ProfitabilityComparison comparison,
                                           RiskReturnAnalysis riskReturnAnalysis,
                                           DiversificationRecommendations diversificationRecommendations,
                                           OverallSensitivityAnalysis sensitivityAnalysis,
                                           LocalDate generatedDate) {
            this.comparison = comparison;
            this.riskReturnAnalysis = riskReturnAnalysis;
            this.diversificationRecommendations = diversificationRecommendations;
            this.sensitivityAnalysis = sensitivityAnalysis;
            this.generatedDate = generatedDate;
        }
        
        // Getters
        public AdvancedProfitabilityCalculator.ProfitabilityComparison getComparison() { return comparison; }
        public RiskReturnAnalysis getRiskReturnAnalysis() { return riskReturnAnalysis; }
        public DiversificationRecommendations getDiversificationRecommendations() { return diversificationRecommendations; }
        public OverallSensitivityAnalysis getSensitivityAnalysis() { return sensitivityAnalysis; }
        public LocalDate getGeneratedDate() { return generatedDate; }
    }
    
    public static class AlertsAndRecommendationsReport {
        private final List<SmartAlertSystem.SmartAlert> activeAlerts;
        private final Map<SmartAlertSystem.AlertType, List<SmartAlertSystem.SmartAlert>> categorizedAlerts;
        private final AlertPriorityAnalysis priorityAnalysis;
        private final List<ActionRecommendation> actionRecommendations;
        private final AlertTrendsAnalysis trendsAnalysis;
        private final LocalDate generatedDate;
        
        public AlertsAndRecommendationsReport(List<SmartAlertSystem.SmartAlert> activeAlerts,
                                            Map<SmartAlertSystem.AlertType, List<SmartAlertSystem.SmartAlert>> categorizedAlerts,
                                            AlertPriorityAnalysis priorityAnalysis,
                                            List<ActionRecommendation> actionRecommendations,
                                            AlertTrendsAnalysis trendsAnalysis,
                                            LocalDate generatedDate) {
            this.activeAlerts = activeAlerts;
            this.categorizedAlerts = categorizedAlerts;
            this.priorityAnalysis = priorityAnalysis;
            this.actionRecommendations = actionRecommendations;
            this.trendsAnalysis = trendsAnalysis;
            this.generatedDate = generatedDate;
        }
        
        // Getters
        public List<SmartAlertSystem.SmartAlert> getActiveAlerts() { return activeAlerts; }
        public Map<SmartAlertSystem.AlertType, List<SmartAlertSystem.SmartAlert>> getCategorizedAlerts() { return categorizedAlerts; }
        public AlertPriorityAnalysis getPriorityAnalysis() { return priorityAnalysis; }
        public List<ActionRecommendation> getActionRecommendations() { return actionRecommendations; }
        public AlertTrendsAnalysis getTrendsAnalysis() { return trendsAnalysis; }
        public LocalDate getGeneratedDate() { return generatedDate; }
    }
    
    // فئات مساعدة إضافية
    
    public static class RiskReturnAnalysis {
        private final Map<String, Double> riskReturnMap;
        
        public RiskReturnAnalysis(Map<String, Double> riskReturnMap) {
            this.riskReturnMap = riskReturnMap;
        }
        
        public Map<String, Double> getRiskReturnMap() { return riskReturnMap; }
    }
    
    public static class DiversificationRecommendations {
        private final List<String> recommendations;
        private final List<AdvancedProfitabilityCalculator.PortfolioAllocation> optimalPortfolio;
        
        public DiversificationRecommendations(List<String> recommendations,
                                            List<AdvancedProfitabilityCalculator.PortfolioAllocation> optimalPortfolio) {
            this.recommendations = recommendations;
            this.optimalPortfolio = optimalPortfolio;
        }
        
        public List<String> getRecommendations() { return recommendations; }
        public List<AdvancedProfitabilityCalculator.PortfolioAllocation> getOptimalPortfolio() { return optimalPortfolio; }
    }
    
    public static class OverallSensitivityAnalysis {
        private final double avgPriceSensitivity;
        
        public OverallSensitivityAnalysis(double avgPriceSensitivity) {
            this.avgPriceSensitivity = avgPriceSensitivity;
        }
        
        public double getAvgPriceSensitivity() { return avgPriceSensitivity; }
    }
    
    public static class AlertPriorityAnalysis {
        private final Map<SmartAlertSystem.AlertPriority, Long> priorityCounts;
        
        public AlertPriorityAnalysis(Map<SmartAlertSystem.AlertPriority, Long> priorityCounts) {
            this.priorityCounts = priorityCounts;
        }
        
        public Map<SmartAlertSystem.AlertPriority, Long> getPriorityCounts() { return priorityCounts; }
    }
    
    public static class ActionRecommendation {
        private final String title;
        private final String action;
        private final String priority;
        
        public ActionRecommendation(String title, String action, String priority) {
            this.title = title;
            this.action = action;
            this.priority = priority;
        }
        
        public String getTitle() { return title; }
        public String getAction() { return action; }
        public String getPriority() { return priority; }
    }
    
    public static class AlertTrendsAnalysis {
        private final Map<SmartAlertSystem.AlertType, Long> typeCounts;
        
        public AlertTrendsAnalysis(Map<SmartAlertSystem.AlertType, Long> typeCounts) {
            this.typeCounts = typeCounts;
        }
        
        public Map<SmartAlertSystem.AlertType, Long> getTypeCounts() { return typeCounts; }
    }
    
    public static class CustomReport {
        private final Map<String, Object> sections;
        private final LocalDate generatedDate;
        
        public CustomReport(Map<String, Object> sections, LocalDate generatedDate) {
            this.sections = sections;
            this.generatedDate = generatedDate;
        }
        
        public Map<String, Object> getSections() { return sections; }
        public LocalDate getGeneratedDate() { return generatedDate; }
    }
    
    public static class ReportConfiguration {
        private final LocalDate fromDate;
        private final LocalDate toDate;
        private final List<Integer> cropIds;
        private final double investmentAmount;
        private final int analysisMonths;
        private final int forecastMonths;
        private final boolean includePerformanceAnalysis;
        private final boolean includeForecastAnalysis;
        private final boolean includeProfitabilityAnalysis;
        private final boolean includeAlertsAnalysis;
        private final List<String> customAnalyses;
        
        public ReportConfiguration(LocalDate fromDate, LocalDate toDate, List<Integer> cropIds,
                                 double investmentAmount, int analysisMonths, int forecastMonths,
                                 boolean includePerformanceAnalysis, boolean includeForecastAnalysis,
                                 boolean includeProfitabilityAnalysis, boolean includeAlertsAnalysis,
                                 List<String> customAnalyses) {
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.cropIds = cropIds;
            this.investmentAmount = investmentAmount;
            this.analysisMonths = analysisMonths;
            this.forecastMonths = forecastMonths;
            this.includePerformanceAnalysis = includePerformanceAnalysis;
            this.includeForecastAnalysis = includeForecastAnalysis;
            this.includeProfitabilityAnalysis = includeProfitabilityAnalysis;
            this.includeAlertsAnalysis = includeAlertsAnalysis;
            this.customAnalyses = customAnalyses;
        }
        
        // Getters
        public LocalDate getFromDate() { return fromDate; }
        public LocalDate getToDate() { return toDate; }
        public List<Integer> getCropIds() { return cropIds; }
        public double getInvestmentAmount() { return investmentAmount; }
        public int getAnalysisMonths() { return analysisMonths; }
        public int getForecastMonths() { return forecastMonths; }
        public boolean includePerformanceAnalysis() { return includePerformanceAnalysis; }
        public boolean includeForecastAnalysis() { return includeForecastAnalysis; }
        public boolean includeProfitabilityAnalysis() { return includeProfitabilityAnalysis; }
        public boolean includeAlertsAnalysis() { return includeAlertsAnalysis; }
        public List<String> getCustomAnalyses() { return customAnalyses; }
    }
    
    public static class CustomReportBuilder {
        private final ReportConfiguration config;
        private final Map<String, Object> sections;
        
        public CustomReportBuilder(ReportConfiguration config) {
            this.config = config;
            this.sections = new HashMap<>();
        }
        
        public void addPerformanceSection(ComprehensivePerformanceReport report) {
            sections.put("performance", report);
        }
        
        public void addForecastSection(ForecastReport report) {
            sections.put("forecast", report);
        }
        
        public void addProfitabilitySection(ProfitabilityComparisonReport report) {
            sections.put("profitability", report);
        }
        
        public void addAlertsSection(AlertsAndRecommendationsReport report) {
            sections.put("alerts", report);
        }
        
        public void addCustomAnalysis(String analysisType, Object analysis) {
            sections.put("custom_" + analysisType, analysis);
        }
        
        public CustomReport build() {
            return new CustomReport(sections, LocalDate.now());
        }
    }
}