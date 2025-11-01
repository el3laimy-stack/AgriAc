package accounting.util;

import accounting.model.*;
import accounting.service.CropDataService;
import accounting.service.PurchaseDataService;
import java.time.LocalDate;
import java.util.*;

/**
 * حاسبة الربحية المتقدمة مع تحليل السيناريوهات المختلفة
 * توفر تحليلاً شاملاً للربحية مع مراعاة عوامل متعددة
 */
public class AdvancedProfitabilityCalculator {
    
    private final CropDataService cropDataService;
    private final PurchaseDataService purchaseDataService;
    private final PredictiveAnalyticsEngine analyticsEngine;
    
    public AdvancedProfitabilityCalculator() {
        this.cropDataService = new CropDataService();
        this.purchaseDataService = new PurchaseDataService();
        this.analyticsEngine = new PredictiveAnalyticsEngine();
    }
    
    /**
     * حساب الربحية الشاملة لمحصول معين
     */
    public ComprehensiveProfitability calculateComprehensiveProfitability(
            int cropId, double investmentAmount, int timeHorizonMonths) {
        
        try {
            Crop crop = cropDataService.getCropById(cropId);
            if (crop == null) {
                throw new IllegalArgumentException("المحصول غير موجود");
            }
            
            // الحصول على البيانات التاريخية
            CropDataService.CropStatistics historicalStats = cropDataService.getCropStatistics(
                cropId, LocalDate.now().minusYears(2), LocalDate.now());
            
            // حساب التكاليف المختلفة
            CostBreakdown costs = calculateDetailedCosts(cropId, investmentAmount, timeHorizonMonths);
            
            // حساب الإيرادات المتوقعة
            RevenueProjection revenues = calculateProjectedRevenues(cropId, timeHorizonMonths);
            
            // تحليل السيناريوهات المختلفة
            ScenarioAnalysis scenarios = performScenarioAnalysis(cropId, investmentAmount, timeHorizonMonths);
            
            // حساب المؤشرات المالية
            FinancialMetrics metrics = calculateFinancialMetrics(costs, revenues, investmentAmount);
            
            // تحليل الحساسية
            SensitivityAnalysis sensitivity = performSensitivityAnalysis(cropId, investmentAmount, timeHorizonMonths);
            
            // تقييم المخاطر
            RiskProfile riskProfile = assessRiskProfile(cropId, historicalStats, scenarios);
            
            return new ComprehensiveProfitability(
                crop, investmentAmount, timeHorizonMonths, costs, revenues, 
                scenarios, metrics, sensitivity, riskProfile);
                
        } catch (Exception e) {
            throw new RuntimeException("خطأ في حساب الربحية: " + e.getMessage(), e);
        }
    }
    
    /**
     * مقارنة ربحية عدة محاصيل
     */
    public ProfitabilityComparison compareCropProfitability(
            List<Integer> cropIds, double investmentPerCrop, int timeHorizonMonths) {
        
        List<ComprehensiveProfitability> profitabilities = new ArrayList<>();
        
        for (int cropId : cropIds) {
            try {
                ComprehensiveProfitability profitability = calculateComprehensiveProfitability(
                    cropId, investmentPerCrop, timeHorizonMonths);
                profitabilities.add(profitability);
            } catch (Exception e) {
                // تسجيل الخطأ والمتابعة مع المحاصيل الأخرى
                System.err.println("خطأ في حساب ربحية المحصول " + cropId + ": " + e.getMessage());
            }
        }
        
        // ترتيب المحاصيل حسب العائد على الاستثمار
        profitabilities.sort((a, b) -> 
            Double.compare(b.getMetrics().getRoi(), a.getMetrics().getRoi()));
        
        // تحليل التنويع
        DiversificationAnalysis diversification = analyzeDiversificationBenefits(profitabilities);
        
        return new ProfitabilityComparison(profitabilities, diversification);
    }
    
    /**
     * حساب التكاليف التفصيلية
     */
    private CostBreakdown calculateDetailedCosts(int cropId, double investmentAmount, int timeHorizonMonths) {
        
        // تكاليف الشراء الأولية
        double purchaseCosts = investmentAmount;
        
        // تكاليف التخزين (2% من قيمة المخزون شهرياً)
        double storageCosts = investmentAmount * 0.02 * timeHorizonMonths;
        
        // تكاليف التأمين (0.5% من قيمة المخزون سنوياً)
        double insuranceCosts = investmentAmount * 0.005 * (timeHorizonMonths / 12.0);
        
        // تكاليف التمويل (معدل فائدة 8% سنوياً)
        double financingCosts = investmentAmount * 0.08 * (timeHorizonMonths / 12.0);
        
        // تكاليف التسويق (3% من الإيرادات المتوقعة)
        double expectedRevenue = estimateRevenue(cropId, investmentAmount, timeHorizonMonths);
        double marketingCosts = expectedRevenue * 0.03;
        
        // تكاليف التشغيل (رواتب، مرافق، إلخ)
        double operationalCosts = 5000 * timeHorizonMonths; // تكلفة ثابتة شهرية
        
        // تكاليف الفقد والتلف (2% من قيمة المخزون)
        double wasteCosts = investmentAmount * 0.02;
        
        // تكاليف النقل والتوزيع
        double transportationCosts = expectedRevenue * 0.015; // 1.5% من الإيرادات
        
        double totalCosts = purchaseCosts + storageCosts + insuranceCosts + financingCosts + 
                           marketingCosts + operationalCosts + wasteCosts + transportationCosts;
        
        return new CostBreakdown(
            purchaseCosts, storageCosts, insuranceCosts, financingCosts,
            marketingCosts, operationalCosts, wasteCosts, transportationCosts, totalCosts);
    }
    
    /**
     * حساب الإيرادات المتوقعة
     */
    private RevenueProjection calculateProjectedRevenues(int cropId, int timeHorizonMonths) {
        
        // الحصول على توقعات الأسعار والطلب
        PredictiveAnalyticsEngine.PriceForecast priceForecast = 
            analyticsEngine.predictPrices(cropId, timeHorizonMonths);
        PredictiveAnalyticsEngine.DemandForecast demandForecast = 
            analyticsEngine.predictDemand(cropId, timeHorizonMonths);
        
        List<MonthlyRevenue> monthlyRevenues = new ArrayList<>();
        double totalRevenue = 0;
        double totalQuantitySold = 0;
        
        for (int i = 0; i < timeHorizonMonths && i < priceForecast.getPriceForecasts().size(); i++) {
            PredictiveAnalyticsEngine.MonthlyPriceForecast price = priceForecast.getPriceForecasts().get(i);
            PredictiveAnalyticsEngine.MonthlyForecast demand = demandForecast.getMonthlyForecasts().get(i);
            
            double monthlyQuantity = demand.getPredictedDemand();
            double monthlyPrice = price.getPredictedPrice();
            double monthlyRevenue = monthlyQuantity * monthlyPrice;
            
            monthlyRevenues.add(new MonthlyRevenue(
                price.getMonth(), monthlyQuantity, monthlyPrice, monthlyRevenue));
            
            totalRevenue += monthlyRevenue;
            totalQuantitySold += monthlyQuantity;
        }
        
        double averagePrice = totalQuantitySold > 0 ? totalRevenue / totalQuantitySold : 0;
        
        return new RevenueProjection(totalRevenue, totalQuantitySold, averagePrice, monthlyRevenues);
    }
    
    /**
     * تحليل السيناريوهات المختلفة
     */
    private ScenarioAnalysis performScenarioAnalysis(int cropId, double investmentAmount, int timeHorizonMonths) {
        
        // السيناريو المتفائل (أسعار مرتفعة، طلب عالي)
        Scenario optimisticScenario = calculateScenario(cropId, investmentAmount, timeHorizonMonths, 1.2, 1.15);
        
        // السيناريو الأساسي (التوقعات الحالية)
        Scenario baseScenario = calculateScenario(cropId, investmentAmount, timeHorizonMonths, 1.0, 1.0);
        
        // السيناريو المتشائم (أسعار منخفضة، طلب ضعيف)
        Scenario pessimisticScenario = calculateScenario(cropId, investmentAmount, timeHorizonMonths, 0.8, 0.85);
        
        return new ScenarioAnalysis(optimisticScenario, baseScenario, pessimisticScenario);
    }
    
    /**
     * حساب سيناريو محدد
     */
    private Scenario calculateScenario(int cropId, double investmentAmount, int timeHorizonMonths, 
                                     double priceMultiplier, double demandMultiplier) {
        
        CostBreakdown costs = calculateDetailedCosts(cropId, investmentAmount, timeHorizonMonths);
        RevenueProjection revenues = calculateProjectedRevenues(cropId, timeHorizonMonths);
        
        // تعديل الإيرادات حسب السيناريو
        double adjustedRevenue = revenues.getTotalRevenue() * priceMultiplier * demandMultiplier;
        double adjustedProfit = adjustedRevenue - costs.getTotalCosts();
        double adjustedRoi = investmentAmount > 0 ? (adjustedProfit / investmentAmount) * 100 : 0;
        
        return new Scenario(adjustedRevenue, costs.getTotalCosts(), adjustedProfit, adjustedRoi);
    }
    
    /**
     * حساب المؤشرات المالية
     */
    private FinancialMetrics calculateFinancialMetrics(CostBreakdown costs, RevenueProjection revenues, 
                                                      double investmentAmount) {
        
        double totalProfit = revenues.getTotalRevenue() - costs.getTotalCosts();
        double roi = investmentAmount > 0 ? (totalProfit / investmentAmount) * 100 : 0;
        double profitMargin = revenues.getTotalRevenue() > 0 ? (totalProfit / revenues.getTotalRevenue()) * 100 : 0;
        
        // حساب فترة الاسترداد
        int paybackPeriod = calculatePaybackPeriod(revenues.getMonthlyRevenues(), costs.getTotalCosts());
        
        // حساب القيمة الحالية الصافية (NPV) بمعدل خصم 10%
        double npv = calculateNPV(revenues.getMonthlyRevenues(), costs.getTotalCosts(), 0.10);
        
        // حساب معدل العائد الداخلي (IRR) - تقدير مبسط
        double irr = estimateIRR(revenues.getMonthlyRevenues(), investmentAmount);
        
        return new FinancialMetrics(totalProfit, roi, profitMargin, paybackPeriod, npv, irr);
    }
    
    /**
     * تحليل الحساسية
     */
    private SensitivityAnalysis performSensitivityAnalysis(int cropId, double investmentAmount, int timeHorizonMonths) {
        
        // تحليل حساسية الأسعار
        Map<Double, Double> priceSensitivity = new HashMap<>();
        for (double priceChange = -0.3; priceChange <= 0.3; priceChange += 0.1) {
            Scenario scenario = calculateScenario(cropId, investmentAmount, timeHorizonMonths, 1 + priceChange, 1.0);
            priceSensitivity.put(priceChange * 100, scenario.getRoi());
        }
        
        // تحليل حساسية الطلب
        Map<Double, Double> demandSensitivity = new HashMap<>();
        for (double demandChange = -0.3; demandChange <= 0.3; demandChange += 0.1) {
            Scenario scenario = calculateScenario(cropId, investmentAmount, timeHorizonMonths, 1.0, 1 + demandChange);
            demandSensitivity.put(demandChange * 100, scenario.getRoi());
        }
        
        // تحليل حساسية التكاليف
        Map<Double, Double> costSensitivity = new HashMap<>();
        for (double costChange = -0.2; costChange <= 0.2; costChange += 0.1) {
            CostBreakdown adjustedCosts = calculateDetailedCosts(cropId, investmentAmount, timeHorizonMonths);
            RevenueProjection revenues = calculateProjectedRevenues(cropId, timeHorizonMonths);
            
            double adjustedTotalCosts = adjustedCosts.getTotalCosts() * (1 + costChange);
            double adjustedProfit = revenues.getTotalRevenue() - adjustedTotalCosts;
            double adjustedRoi = investmentAmount > 0 ? (adjustedProfit / investmentAmount) * 100 : 0;
            
            costSensitivity.put(costChange * 100, adjustedRoi);
        }
        
        return new SensitivityAnalysis(priceSensitivity, demandSensitivity, costSensitivity);
    }
    
    /**
     * تقييم ملف المخاطر
     */
    private RiskProfile assessRiskProfile(int cropId, CropDataService.CropStatistics historicalStats, 
                                        ScenarioAnalysis scenarios) {
        
        // حساب التقلبات التاريخية
        double volatility = calculateVolatility(historicalStats);
        
        // تحليل المخاطر المختلفة
        double marketRisk = assessMarketRisk(scenarios);
        double operationalRisk = assessOperationalRisk(cropId);
        double liquidityRisk = assessLiquidityRisk(cropId);
        double creditRisk = assessCreditRisk();
        
        // حساب المخاطر الإجمالية
        double overallRisk = (marketRisk + operationalRisk + liquidityRisk + creditRisk) / 4.0;
        
        RiskLevel riskLevel;
        if (overallRisk < 0.3) riskLevel = RiskLevel.LOW;
        else if (overallRisk < 0.6) riskLevel = RiskLevel.MEDIUM;
        else riskLevel = RiskLevel.HIGH;
        
        List<String> riskFactors = identifyRiskFactors(marketRisk, operationalRisk, liquidityRisk, creditRisk);
        List<String> mitigationStrategies = suggestMitigationStrategies(riskLevel, riskFactors);
        
        return new RiskProfile(riskLevel, overallRisk, volatility, marketRisk, operationalRisk, 
                              liquidityRisk, creditRisk, riskFactors, mitigationStrategies);
    }
    
    /**
     * تحليل فوائد التنويع
     */
    private DiversificationAnalysis analyzeDiversificationBenefits(List<ComprehensiveProfitability> profitabilities) {
        
        if (profitabilities.size() < 2) {
            return new DiversificationAnalysis(0, 0, Collections.emptyList());
        }
        
        // حساب الارتباط بين المحاصيل
        double averageCorrelation = calculateAverageCorrelation(profitabilities);
        
        // حساب تقليل المخاطر من التنويع
        double riskReduction = calculateRiskReduction(profitabilities, averageCorrelation);
        
        // اقتراح محفظة مثلى
        List<PortfolioAllocation> optimalPortfolio = suggestOptimalPortfolio(profitabilities);
        
        return new DiversificationAnalysis(averageCorrelation, riskReduction, optimalPortfolio);
    }
    
    // الطرق المساعدة
    
    private double estimateRevenue(int cropId, double investmentAmount, int timeHorizonMonths) {
        // تقدير مبسط للإيرادات
        return investmentAmount * 1.2; // افتراض عائد 20%
    }
    
    private int calculatePaybackPeriod(List<MonthlyRevenue> monthlyRevenues, double totalCosts) {
        double cumulativeRevenue = 0;
        for (int i = 0; i < monthlyRevenues.size(); i++) {
            cumulativeRevenue += monthlyRevenues.get(i).getRevenue();
            if (cumulativeRevenue >= totalCosts) {
                return i + 1;
            }
        }
        return -1; // لم يتم الاسترداد خلال الفترة
    }
    
    private double calculateNPV(List<MonthlyRevenue> monthlyRevenues, double totalCosts, double discountRate) {
        double npv = -totalCosts; // الاستثمار الأولي
        double monthlyDiscountRate = discountRate / 12.0;
        
        for (int i = 0; i < monthlyRevenues.size(); i++) {
            double discountFactor = Math.pow(1 + monthlyDiscountRate, -(i + 1));
            npv += monthlyRevenues.get(i).getRevenue() * discountFactor;
        }
        
        return npv;
    }
    
    private double estimateIRR(List<MonthlyRevenue> monthlyRevenues, double investmentAmount) {
        // تقدير مبسط لمعدل العائد الداخلي
        double totalRevenue = monthlyRevenues.stream().mapToDouble(MonthlyRevenue::getRevenue).sum();
        double totalProfit = totalRevenue - investmentAmount;
        int periods = monthlyRevenues.size();
        
        if (periods == 0 || investmentAmount == 0) return 0;
        
        return Math.pow(totalRevenue / investmentAmount, 12.0 / periods) - 1;
    }
    
    private double calculateVolatility(CropDataService.CropStatistics stats) {
        // حساب التقلبات بناءً على البيانات التاريخية
        return 0.15; // 15% تقلبات افتراضية
    }
    
    private double assessMarketRisk(ScenarioAnalysis scenarios) {
        // تقييم مخاطر السوق بناءً على تباين السيناريوهات
        double roiRange = scenarios.getOptimisticScenario().getRoi() - scenarios.getPessimisticScenario().getRoi();
        return Math.min(1.0, roiRange / 100.0); // تطبيع القيمة بين 0 و 1
    }
    
    private double assessOperationalRisk(int cropId) {
        // تقييم المخاطر التشغيلية
        return 0.3; // 30% مخاطر تشغيلية افتراضية
    }
    
    private double assessLiquidityRisk(int cropId) {
        // تقييم مخاطر السيولة
        return 0.2; // 20% مخاطر سيولة افتراضية
    }
    
    private double assessCreditRisk() {
        // تقييم مخاطر الائتمان
        return 0.1; // 10% مخاطر ائتمان افتراضية
    }
    
    private List<String> identifyRiskFactors(double marketRisk, double operationalRisk, 
                                           double liquidityRisk, double creditRisk) {
        List<String> factors = new ArrayList<>();
        
        if (marketRisk > 0.5) factors.add("تقلبات أسعار السوق");
        if (operationalRisk > 0.5) factors.add("مخاطر تشغيلية");
        if (liquidityRisk > 0.5) factors.add("صعوبة في السيولة");
        if (creditRisk > 0.5) factors.add("مخاطر ائتمانية");
        
        return factors;
    }
    
    private List<String> suggestMitigationStrategies(RiskLevel riskLevel, List<String> riskFactors) {
        List<String> strategies = new ArrayList<>();
        
        strategies.add("تنويع المحفظة الاستثمارية");
        strategies.add("التأمين على المحاصيل");
        strategies.add("استخدام العقود الآجلة للتحوط");
        strategies.add("بناء احتياطي نقدي");
        
        if (riskLevel == RiskLevel.HIGH) {
            strategies.add("تقليل حجم الاستثمار");
            strategies.add("البحث عن شركاء استثماريين");
        }
        
        return strategies;
    }
    
    private double calculateAverageCorrelation(List<ComprehensiveProfitability> profitabilities) {
        // حساب الارتباط المتوسط بين المحاصيل
        return 0.3; // 30% ارتباط افتراضي
    }
    
    private double calculateRiskReduction(List<ComprehensiveProfitability> profitabilities, double correlation) {
        // حساب تقليل المخاطر من التنويع
        int n = profitabilities.size();
        return (1 - correlation) * (1 - 1.0/n);
    }
    
    private List<PortfolioAllocation> suggestOptimalPortfolio(List<ComprehensiveProfitability> profitabilities) {
        List<PortfolioAllocation> allocations = new ArrayList<>();
        
        // توزيع متساوي مبسط
        double equalWeight = 1.0 / profitabilities.size();
        
        for (ComprehensiveProfitability prof : profitabilities) {
            allocations.add(new PortfolioAllocation(
                prof.getCrop().getCropId(),
                prof.getCrop().getCropName(),
                equalWeight,
                equalWeight * 100000 // مبلغ افتراضي
            ));
        }
        
        return allocations;
    }
    
    // الفئات المساعدة
    
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
    
    // باقي الفئات المساعدة ستكون في ملف منفصل لتوفير المساحة
    
    public static class ComprehensiveProfitability {
        private final Crop crop;
        private final double investmentAmount;
        private final int timeHorizonMonths;
        private final CostBreakdown costs;
        private final RevenueProjection revenues;
        private final ScenarioAnalysis scenarios;
        private final FinancialMetrics metrics;
        private final SensitivityAnalysis sensitivity;
        private final RiskProfile riskProfile;
        
        public ComprehensiveProfitability(Crop crop, double investmentAmount, int timeHorizonMonths,
                                        CostBreakdown costs, RevenueProjection revenues,
                                        ScenarioAnalysis scenarios, FinancialMetrics metrics,
                                        SensitivityAnalysis sensitivity, RiskProfile riskProfile) {
            this.crop = crop;
            this.investmentAmount = investmentAmount;
            this.timeHorizonMonths = timeHorizonMonths;
            this.costs = costs;
            this.revenues = revenues;
            this.scenarios = scenarios;
            this.metrics = metrics;
            this.sensitivity = sensitivity;
            this.riskProfile = riskProfile;
        }
        
        // Getters
        public Crop getCrop() { return crop; }
        public double getInvestmentAmount() { return investmentAmount; }
        public int getTimeHorizonMonths() { return timeHorizonMonths; }
        public CostBreakdown getCosts() { return costs; }
        public RevenueProjection getRevenues() { return revenues; }
        public ScenarioAnalysis getScenarios() { return scenarios; }
        public FinancialMetrics getMetrics() { return metrics; }
        public SensitivityAnalysis getSensitivity() { return sensitivity; }
        public RiskProfile getRiskProfile() { return riskProfile; }
    }
    
    public static class CostBreakdown {
        private final double purchaseCosts;
        private final double storageCosts;
        private final double insuranceCosts;
        private final double financingCosts;
        private final double marketingCosts;
        private final double operationalCosts;
        private final double wasteCosts;
        private final double transportationCosts;
        private final double totalCosts;
        
        public CostBreakdown(double purchaseCosts, double storageCosts, double insuranceCosts,
                           double financingCosts, double marketingCosts, double operationalCosts,
                           double wasteCosts, double transportationCosts, double totalCosts) {
            this.purchaseCosts = purchaseCosts;
            this.storageCosts = storageCosts;
            this.insuranceCosts = insuranceCosts;
            this.financingCosts = financingCosts;
            this.marketingCosts = marketingCosts;
            this.operationalCosts = operationalCosts;
            this.wasteCosts = wasteCosts;
            this.transportationCosts = transportationCosts;
            this.totalCosts = totalCosts;
        }
        
        // Getters
        public double getPurchaseCosts() { return purchaseCosts; }
        public double getStorageCosts() { return storageCosts; }
        public double getInsuranceCosts() { return insuranceCosts; }
        public double getFinancingCosts() { return financingCosts; }
        public double getMarketingCosts() { return marketingCosts; }
        public double getOperationalCosts() { return operationalCosts; }
        public double getWasteCosts() { return wasteCosts; }
        public double getTransportationCosts() { return transportationCosts; }
        public double getTotalCosts() { return totalCosts; }
    }
    
    public static class RevenueProjection {
        private final double totalRevenue;
        private final double totalQuantitySold;
        private final double averagePrice;
        private final List<MonthlyRevenue> monthlyRevenues;
        
        public RevenueProjection(double totalRevenue, double totalQuantitySold, double averagePrice,
                               List<MonthlyRevenue> monthlyRevenues) {
            this.totalRevenue = totalRevenue;
            this.totalQuantitySold = totalQuantitySold;
            this.averagePrice = averagePrice;
            this.monthlyRevenues = monthlyRevenues;
        }
        
        // Getters
        public double getTotalRevenue() { return totalRevenue; }
        public double getTotalQuantitySold() { return totalQuantitySold; }
        public double getAveragePrice() { return averagePrice; }
        public List<MonthlyRevenue> getMonthlyRevenues() { return monthlyRevenues; }
    }
    
    public static class MonthlyRevenue {
        private final LocalDate month;
        private final double quantity;
        private final double price;
        private final double revenue;
        
        public MonthlyRevenue(LocalDate month, double quantity, double price, double revenue) {
            this.month = month;
            this.quantity = quantity;
            this.price = price;
            this.revenue = revenue;
        }
        
        // Getters
        public LocalDate getMonth() { return month; }
        public double getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public double getRevenue() { return revenue; }
    }
    
    public static class ScenarioAnalysis {
        private final Scenario optimisticScenario;
        private final Scenario baseScenario;
        private final Scenario pessimisticScenario;
        
        public ScenarioAnalysis(Scenario optimisticScenario, Scenario baseScenario, Scenario pessimisticScenario) {
            this.optimisticScenario = optimisticScenario;
            this.baseScenario = baseScenario;
            this.pessimisticScenario = pessimisticScenario;
        }
        
        // Getters
        public Scenario getOptimisticScenario() { return optimisticScenario; }
        public Scenario getBaseScenario() { return baseScenario; }
        public Scenario getPessimisticScenario() { return pessimisticScenario; }
    }
    
    public static class Scenario {
        private final double revenue;
        private final double costs;
        private final double profit;
        private final double roi;
        
        public Scenario(double revenue, double costs, double profit, double roi) {
            this.revenue = revenue;
            this.costs = costs;
            this.profit = profit;
            this.roi = roi;
        }
        
        // Getters
        public double getRevenue() { return revenue; }
        public double getCosts() { return costs; }
        public double getProfit() { return profit; }
        public double getRoi() { return roi; }
    }
    
    public static class FinancialMetrics {
        private final double totalProfit;
        private final double roi;
        private final double profitMargin;
        private final int paybackPeriod;
        private final double npv;
        private final double irr;
        
        public FinancialMetrics(double totalProfit, double roi, double profitMargin,
                              int paybackPeriod, double npv, double irr) {
            this.totalProfit = totalProfit;
            this.roi = roi;
            this.profitMargin = profitMargin;
            this.paybackPeriod = paybackPeriod;
            this.npv = npv;
            this.irr = irr;
        }
        
        // Getters
        public double getTotalProfit() { return totalProfit; }
        public double getRoi() { return roi; }
        public double getProfitMargin() { return profitMargin; }
        public int getPaybackPeriod() { return paybackPeriod; }
        public double getNpv() { return npv; }
        public double getIrr() { return irr; }
    }
    
    public static class SensitivityAnalysis {
        private final Map<Double, Double> priceSensitivity;
        private final Map<Double, Double> demandSensitivity;
        private final Map<Double, Double> costSensitivity;
        
        public SensitivityAnalysis(Map<Double, Double> priceSensitivity,
                                 Map<Double, Double> demandSensitivity,
                                 Map<Double, Double> costSensitivity) {
            this.priceSensitivity = priceSensitivity;
            this.demandSensitivity = demandSensitivity;
            this.costSensitivity = costSensitivity;
        }
        
        // Getters
        public Map<Double, Double> getPriceSensitivity() { return priceSensitivity; }
        public Map<Double, Double> getDemandSensitivity() { return demandSensitivity; }
        public Map<Double, Double> getCostSensitivity() { return costSensitivity; }
    }
    
    public static class RiskProfile {
        private final RiskLevel riskLevel;
        private final double overallRisk;
        private final double volatility;
        private final double marketRisk;
        private final double operationalRisk;
        private final double liquidityRisk;
        private final double creditRisk;
        private final List<String> riskFactors;
        private final List<String> mitigationStrategies;
        
        public RiskProfile(RiskLevel riskLevel, double overallRisk, double volatility,
                         double marketRisk, double operationalRisk, double liquidityRisk,
                         double creditRisk, List<String> riskFactors, List<String> mitigationStrategies) {
            this.riskLevel = riskLevel;
            this.overallRisk = overallRisk;
            this.volatility = volatility;
            this.marketRisk = marketRisk;
            this.operationalRisk = operationalRisk;
            this.liquidityRisk = liquidityRisk;
            this.creditRisk = creditRisk;
            this.riskFactors = riskFactors;
            this.mitigationStrategies = mitigationStrategies;
        }
        
        // Getters
        public RiskLevel getRiskLevel() { return riskLevel; }
        public double getOverallRisk() { return overallRisk; }
        public double getVolatility() { return volatility; }
        public double getMarketRisk() { return marketRisk; }
        public double getOperationalRisk() { return operationalRisk; }
        public double getLiquidityRisk() { return liquidityRisk; }
        public double getCreditRisk() { return creditRisk; }
        public List<String> getRiskFactors() { return riskFactors; }
        public List<String> getMitigationStrategies() { return mitigationStrategies; }
    }
    
    public static class ProfitabilityComparison {
        private final List<ComprehensiveProfitability> profitabilities;
        private final DiversificationAnalysis diversificationAnalysis;
        
        public ProfitabilityComparison(List<ComprehensiveProfitability> profitabilities,
                                     DiversificationAnalysis diversificationAnalysis) {
            this.profitabilities = profitabilities;
            this.diversificationAnalysis = diversificationAnalysis;
        }
        
        // Getters
        public List<ComprehensiveProfitability> getProfitabilities() { return profitabilities; }
        public DiversificationAnalysis getDiversificationAnalysis() { return diversificationAnalysis; }
    }
    
    public static class DiversificationAnalysis {
        private final double averageCorrelation;
        private final double riskReduction;
        private final List<PortfolioAllocation> optimalPortfolio;
        
        public DiversificationAnalysis(double averageCorrelation, double riskReduction,
                                     List<PortfolioAllocation> optimalPortfolio) {
            this.averageCorrelation = averageCorrelation;
            this.riskReduction = riskReduction;
            this.optimalPortfolio = optimalPortfolio;
        }
        
        // Getters
        public double getAverageCorrelation() { return averageCorrelation; }
        public double getRiskReduction() { return riskReduction; }
        public List<PortfolioAllocation> getOptimalPortfolio() { return optimalPortfolio; }
    }
    
    public static class PortfolioAllocation {
        private final int cropId;
        private final String cropName;
        private final double weight;
        private final double amount;
        
        public PortfolioAllocation(int cropId, String cropName, double weight, double amount) {
            this.cropId = cropId;
            this.cropName = cropName;
            this.weight = weight;
            this.amount = amount;
        }
        
        // Getters
        public int getCropId() { return cropId; }
        public String getCropName() { return cropName; }
        public double getWeight() { return weight; }
        public double getAmount() { return amount; }
    }
}