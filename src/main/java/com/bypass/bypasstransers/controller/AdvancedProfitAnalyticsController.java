package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.enums.TransactionType;
import com.bypass.bypasstransers.service.SecurityService;
import com.bypass.bypasstransers.util.ChargeCalculator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.*;

@Controller
@RequestMapping("/admin/profit-analytics")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPERVISOR')")
public class AdvancedProfitAnalyticsController {

    private final TransactionRepository transactionRepository;
    private final SecurityService securityService;

    public AdvancedProfitAnalyticsController(TransactionRepository transactionRepository, 
                                           SecurityService securityService) {
        this.transactionRepository = transactionRepository;
        this.securityService = securityService;
    }

    @GetMapping({"/", ""})
    public String profitAnalyticsDashboard(@RequestParam(defaultValue = "daily") String period,
                                          @RequestParam(defaultValue = "7") int days,
                                          Model model) {
        User currentUser = securityService.getCurrentUser();
        
        List<Transaction> allTransactions = transactionRepository.findAll();
        List<Transaction> recentTransactions = filterRecentTransactions(allTransactions, days);
        
        Map<String, Object> profitData = calculateProfitData(recentTransactions, period);
        Map<String, Object> trendData = prepareTrendAnalysis(recentTransactions, period);
        
        model.addAttribute("profitData", profitData);
        model.addAttribute("trendData", trendData);
        model.addAttribute("peakDay", findPeakProfitDayLabel(recentTransactions));
        model.addAttribute("period", period);
        model.addAttribute("days", days);
        model.addAttribute("user", currentUser);
        model.addAttribute("isSuperAdmin", securityService.isSuperAdmin());
        
        return "advanced-profit-analytics";
    }

    @GetMapping("/visualizations")
    public String profitVisualizations(@RequestParam(defaultValue = "daily") String period,
                                      @RequestParam(defaultValue = "30") int days,
                                      Model model) {
        User currentUser = securityService.getCurrentUser();
        
        List<Transaction> allTransactions = transactionRepository.findAll();
        List<Transaction> recentTransactions = filterRecentTransactions(allTransactions, days);
        
        Map<String, Object> chartData = prepareChartData(recentTransactions, period);
        
        model.addAttribute("chartData", chartData);
        model.addAttribute("period", period);
        model.addAttribute("days", days);
        model.addAttribute("user", currentUser);
        
        return "profit-visualizations";
    }

    @GetMapping("/trend-analysis")
    public String trendAnalysis(@RequestParam(defaultValue = "daily") String period,
                                @RequestParam(defaultValue = "30") int days,
                                Model model) {
        User currentUser = securityService.getCurrentUser();
        
        List<Transaction> allTransactions = transactionRepository.findAll();
        List<Transaction> recentTransactions = filterRecentTransactions(allTransactions, days);
        
        Map<String, Object> trendData = prepareTrendAnalysis(recentTransactions, period);
        
        model.addAttribute("trendData", trendData);
        model.addAttribute("period", period);
        model.addAttribute("days", days);
        model.addAttribute("user", currentUser);
        
        return "trend-analysis";
    }

    private List<Transaction> filterRecentTransactions(List<Transaction> transactions, int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        return transactions.stream()
                .filter(t -> t.getDate() != null && t.getDate().toLocalDate().isAfter(cutoffDate))
                .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
                .toList();
    }

    private Map<String, Object> calculateProfitData(List<Transaction> transactions, String period) {
        Map<String, Object> profitData = new HashMap<>();
        
        BigDecimal totalProfit = transactions.stream()
                .map(this::profitFromTransaction)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalVolume = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long transactionCount = transactions.size();
        
        BigDecimal avgProfitPerTransaction = transactionCount > 0 ? 
                totalProfit.divide(BigDecimal.valueOf(transactionCount), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        profitData.put("totalProfit", totalProfit.doubleValue());
        profitData.put("totalVolume", totalVolume.doubleValue());
        profitData.put("transactionCount", transactionCount);
        profitData.put("avgProfitPerTransaction", avgProfitPerTransaction.doubleValue());
        
        return profitData;
    }

    private Map<String, Object> prepareChartData(List<Transaction> transactions, String period) {
        Map<String, Object> chartData = new HashMap<>();
        
        if ("daily".equalsIgnoreCase(period)) {
            chartData.put("labels", getDailyLabels(transactions));
            chartData.put("datasets", getDailyProfitData(transactions));
        } else if ("weekly".equalsIgnoreCase(period)) {
            chartData.put("labels", getWeeklyLabels(transactions));
            chartData.put("datasets", getWeeklyProfitData(transactions));
        } else if ("monthly".equalsIgnoreCase(period)) {
            chartData.put("labels", getMonthlyLabels(transactions));
            chartData.put("datasets", getMonthlyProfitData(transactions));
        }
        
        return chartData;
    }

    private Map<String, Object> prepareTrendAnalysis(List<Transaction> transactions, String period) {
        Map<String, Object> trendData = new HashMap<>();
        trendData.put("trendDirection", "insufficient_data");
        trendData.put("volatility", 0.0);
        trendData.put("currentAverage", 0.0);
        trendData.put("seasonalPattern", null);
        trendData.put("predictedWeeklyProfit", 0.0);
        trendData.put("confidenceLevel", "Low");
        trendData.put("peakDayPrediction", "N/A");
        
        if ("daily".equalsIgnoreCase(period)) {
            List<BigDecimal> dailyProfits = getDailyProfitValues(transactions);
            trendData.put("trendDirection", calculateTrend(dailyProfits));
            BigDecimal vol = calculateVolatility(dailyProfits);
            BigDecimal avg = dailyProfits.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(dailyProfits.isEmpty() ? BigDecimal.ONE : BigDecimal.valueOf(dailyProfits.size()), 4, RoundingMode.HALF_UP);
            trendData.put("volatility", vol.doubleValue());
            trendData.put("currentAverage", avg.doubleValue());
            trendData.put("predictedWeeklyProfit", avg.multiply(BigDecimal.valueOf(7)).doubleValue());
            trendData.put("confidenceLevel", confidenceLabel(dailyProfits.size()));
            trendData.put("seasonalPattern", seasonalityLabel(avg, vol));
        } else if ("weekly".equalsIgnoreCase(period)) {
            List<BigDecimal> weeklyProfits = getWeeklyProfitValues(transactions);
            trendData.put("trendDirection", calculateTrend(weeklyProfits));
            BigDecimal vol = calculateVolatility(weeklyProfits);
            BigDecimal avg = weeklyProfits.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(weeklyProfits.isEmpty() ? BigDecimal.ONE : BigDecimal.valueOf(weeklyProfits.size()), 4, RoundingMode.HALF_UP);
            trendData.put("volatility", vol.doubleValue());
            trendData.put("currentAverage", avg.doubleValue());
            trendData.put("predictedWeeklyProfit", avg.doubleValue());
            trendData.put("confidenceLevel", confidenceLabel(weeklyProfits.size()));
            trendData.put("seasonalPattern", seasonalityLabel(avg, vol));
        } else if ("monthly".equalsIgnoreCase(period)) {
            List<BigDecimal> monthlyProfits = getMonthlyProfitValues(transactions);
            trendData.put("trendDirection", calculateTrend(monthlyProfits));
            BigDecimal vol = calculateVolatility(monthlyProfits);
            BigDecimal avg = monthlyProfits.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(monthlyProfits.isEmpty() ? BigDecimal.ONE : BigDecimal.valueOf(monthlyProfits.size()), 4, RoundingMode.HALF_UP);
            trendData.put("volatility", vol.doubleValue());
            trendData.put("currentAverage", avg.doubleValue());
            trendData.put("predictedWeeklyProfit", avg.divide(BigDecimal.valueOf(4), 4, RoundingMode.HALF_UP).doubleValue());
            trendData.put("confidenceLevel", confidenceLabel(monthlyProfits.size()));
            trendData.put("seasonalPattern", seasonalityLabel(avg, vol));
        }

        trendData.put("peakDayPrediction", predictPeakDayOfWeek(transactions));
        
        return trendData;
    }

    private String findPeakProfitDayLabel(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) return "N/A";

        Map<LocalDate, BigDecimal> daily = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getDate() == null) continue;
            LocalDate d = t.getDate().toLocalDate();
            daily.merge(d, profitFromTransaction(t), BigDecimal::add);
        }
        return daily.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().toString())
                .orElse("N/A");
    }

    private String predictPeakDayOfWeek(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) return "N/A";

        Map<DayOfWeek, BigDecimal> byDow = new EnumMap<>(DayOfWeek.class);
        for (Transaction t : transactions) {
            if (t.getDate() == null) continue;
            DayOfWeek dow = t.getDate().toLocalDate().getDayOfWeek();
            byDow.merge(dow, profitFromTransaction(t), BigDecimal::add);
        }
        return byDow.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().toString())
                .orElse("N/A");
    }

    private BigDecimal profitFromTransaction(Transaction t) {
        if (t == null || t.getType() == null) return BigDecimal.ZERO;
        if (t.getType() == TransactionType.INCOME) return BigDecimal.ZERO;
        return t.getAmount().multiply(ChargeCalculator.BASE_PROFIT_DEFAULT);
    }

    private String confidenceLabel(int sampleSize) {
        if (sampleSize >= 12) return "High";
        if (sampleSize >= 6) return "Medium";
        return "Low";
    }

    private String seasonalityLabel(BigDecimal mean, BigDecimal stdev) {
        if (mean.compareTo(BigDecimal.ZERO) <= 0) return null;
        BigDecimal cv = stdev.divide(mean, 4, RoundingMode.HALF_UP);
        if (cv.compareTo(new BigDecimal("0.6")) >= 0) return "high";
        if (cv.compareTo(new BigDecimal("0.3")) >= 0) return "medium";
        return "low";
    }

    private List<String> getDailyLabels(List<Transaction> transactions) {
        Set<LocalDate> dates = new TreeSet<>();
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                dates.add(t.getDate().toLocalDate());
            }
        }
        List<String> labels = new ArrayList<>();
        for (LocalDate date : dates) {
            labels.add(date.toString());
        }
        return labels;
    }

    private List<Map<String, Object>> getDailyProfitData(List<Transaction> transactions) {
        Map<LocalDate, BigDecimal> dailyProfits = new HashMap<>();
        
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                LocalDate date = t.getDate().toLocalDate();
                dailyProfits.merge(date, profitFromTransaction(t), BigDecimal::add);
            }
        }
        
        List<Map<String, Object>> datasets = new ArrayList<>();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Daily Profit");
        dataset.put("data", dailyProfits.values().stream().map(BigDecimal::doubleValue).toList());
        dataset.put("backgroundColor", "rgba(75, 192, 192, 0.2)");
        dataset.put("borderColor", "rgba(75, 192, 192, 1)");
        dataset.put("borderWidth", 2);
        datasets.add(dataset);
        
        return datasets;
    }

    private List<String> getWeeklyLabels(List<Transaction> transactions) {
        Map<String, String> weeks = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                LocalDate date = t.getDate().toLocalDate();
                LocalDate startOfWeek = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                String weekLabel = startOfWeek.toString();
                weeks.put(weekLabel, weekLabel);
            }
        }
        List<String> result = new ArrayList<>(weeks.values());
        Collections.sort(result);
        return result;
    }

    private List<Map<String, Object>> getWeeklyProfitData(List<Transaction> transactions) {
        Map<String, BigDecimal> weeklyProfits = new HashMap<>();
        
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                LocalDate date = t.getDate().toLocalDate();
                LocalDate startOfWeek = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                String weekLabel = startOfWeek.toString();
                weeklyProfits.merge(weekLabel, profitFromTransaction(t), BigDecimal::add);
            }
        }
        
        List<Map<String, Object>> datasets = new ArrayList<>();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Weekly Profit");
        dataset.put("data", weeklyProfits.values().stream().map(BigDecimal::doubleValue).toList());
        dataset.put("backgroundColor", "rgba(153, 102, 255, 0.2)");
        dataset.put("borderColor", "rgba(153, 102, 255, 1)");
        dataset.put("borderWidth", 2);
        datasets.add(dataset);
        
        return datasets;
    }

    private List<String> getMonthlyLabels(List<Transaction> transactions) {
        Map<String, String> months = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                YearMonth yearMonth = YearMonth.from(t.getDate().toLocalDate());
                String monthLabel = yearMonth.toString();
                months.put(yearMonth.toString(), monthLabel);
            }
        }
        List<String> result = new ArrayList<>(months.values());
        Collections.sort(result);
        return result;
    }

    private List<Map<String, Object>> getMonthlyProfitData(List<Transaction> transactions) {
        Map<String, BigDecimal> monthlyProfits = new HashMap<>();
        
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                YearMonth yearMonth = YearMonth.from(t.getDate().toLocalDate());
                String monthLabel = yearMonth.toString();
                monthlyProfits.merge(monthLabel, profitFromTransaction(t), BigDecimal::add);
            }
        }
        
        List<Map<String, Object>> datasets = new ArrayList<>();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Monthly Profit");
        dataset.put("data", monthlyProfits.values().stream().map(BigDecimal::doubleValue).toList());
        dataset.put("backgroundColor", "rgba(255, 159, 64, 0.2)");
        dataset.put("borderColor", "rgba(255, 159, 64, 1)");
        dataset.put("borderWidth", 2);
        datasets.add(dataset);
        
        return datasets;
    }

    private List<BigDecimal> getDailyProfitValues(List<Transaction> transactions) {
        Map<LocalDate, BigDecimal> dailyProfits = new HashMap<>();
        
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                LocalDate date = t.getDate().toLocalDate();
                dailyProfits.merge(date, profitFromTransaction(t), BigDecimal::add);
            }
        }
        
        return new ArrayList<>(dailyProfits.values());
    }

    private List<BigDecimal> getWeeklyProfitValues(List<Transaction> transactions) {
        Map<String, BigDecimal> weeklyProfits = new HashMap<>();
        
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                LocalDate date = t.getDate().toLocalDate();
                LocalDate startOfWeek = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                String weekLabel = startOfWeek.toString();
                weeklyProfits.merge(weekLabel, profitFromTransaction(t), BigDecimal::add);
            }
        }
        
        return new ArrayList<>(weeklyProfits.values());
    }

    private List<BigDecimal> getMonthlyProfitValues(List<Transaction> transactions) {
        Map<String, BigDecimal> monthlyProfits = new HashMap<>();
        
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                YearMonth yearMonth = YearMonth.from(t.getDate().toLocalDate());
                String monthLabel = yearMonth.toString();
                monthlyProfits.merge(monthLabel, profitFromTransaction(t), BigDecimal::add);
            }
        }
        
        return new ArrayList<>(monthlyProfits.values());
    }

    private String calculateTrend(List<BigDecimal> values) {
        if (values.size() < 2) {
            return "insufficient_data";
        }
        
        BigDecimal firstHalfSum = BigDecimal.ZERO;
        BigDecimal secondHalfSum = BigDecimal.ZERO;
        int midPoint = values.size() / 2;
        
        for (int i = 0; i < midPoint; i++) {
            firstHalfSum = firstHalfSum.add(values.get(i));
        }
        BigDecimal firstHalfAvg = firstHalfSum.divide(BigDecimal.valueOf(midPoint), 4, RoundingMode.HALF_UP);
        
        for (int i = midPoint; i < values.size(); i++) {
            secondHalfSum = secondHalfSum.add(values.get(i));
        }
        BigDecimal secondHalfAvg = secondHalfSum.divide(BigDecimal.valueOf(values.size() - midPoint), 4, RoundingMode.HALF_UP);
        
        if (secondHalfAvg.compareTo(firstHalfAvg.multiply(new BigDecimal("1.05"))) > 0) {
            return "increasing";
        } else if (secondHalfAvg.compareTo(firstHalfAvg.multiply(new BigDecimal("0.95"))) < 0) {
            return "decreasing";
        } else {
            return "stable";
        }
    }

    private BigDecimal calculateVolatility(List<BigDecimal> values) {
        if (values.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
        
        BigDecimal varianceSum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            BigDecimal diff = v.subtract(mean);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }
        BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
        
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(4, RoundingMode.HALF_UP);
    }
}