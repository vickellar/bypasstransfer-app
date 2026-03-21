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
        
        // Calculate profit data based on period
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
        
        // Prepare chart data
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
        
        // Prepare trend analysis data
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
        
        // Calculate total profit
        double totalProfit = transactions.stream()
                .mapToDouble(this::profitFromTransaction)
                .sum();
        
        // Calculate total volume
        double totalVolume = transactions.stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        // Calculate transaction count
        long transactionCount = transactions.size();
        
        // Calculate average profit per transaction
        double avgProfitPerTransaction = transactionCount > 0 ? totalProfit / transactionCount : 0;
        
        profitData.put("totalProfit", totalProfit);
        profitData.put("totalVolume", totalVolume);
        profitData.put("transactionCount", transactionCount);
        profitData.put("avgProfitPerTransaction", avgProfitPerTransaction);
        
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
        // Defaults to prevent template errors
        trendData.put("trendDirection", "insufficient_data");
        trendData.put("volatility", 0.0);
        trendData.put("currentAverage", 0.0);
        trendData.put("seasonalPattern", null);
        trendData.put("predictedWeeklyProfit", 0.0);
        trendData.put("confidenceLevel", "Low");
        trendData.put("peakDayPrediction", "N/A");
        
        // Calculate profit trend
        if ("daily".equalsIgnoreCase(period)) {
            List<Double> dailyProfits = getDailyProfitValues(transactions);
            trendData.put("trendDirection", calculateTrend(dailyProfits));
            double vol = calculateVolatility(dailyProfits);
            double avg = dailyProfits.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            trendData.put("volatility", vol);
            trendData.put("currentAverage", avg);
            trendData.put("predictedWeeklyProfit", avg * 7);
            trendData.put("confidenceLevel", confidenceLabel(dailyProfits.size()));
            trendData.put("seasonalPattern", seasonalityLabel(avg, vol));
        } else if ("weekly".equalsIgnoreCase(period)) {
            List<Double> weeklyProfits = getWeeklyProfitValues(transactions);
            trendData.put("trendDirection", calculateTrend(weeklyProfits));
            double vol = calculateVolatility(weeklyProfits);
            double avg = weeklyProfits.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            trendData.put("volatility", vol);
            trendData.put("currentAverage", avg);
            // weekly average already represents a week
            trendData.put("predictedWeeklyProfit", avg);
            trendData.put("confidenceLevel", confidenceLabel(weeklyProfits.size()));
            trendData.put("seasonalPattern", seasonalityLabel(avg, vol));
        } else if ("monthly".equalsIgnoreCase(period)) {
            List<Double> monthlyProfits = getMonthlyProfitValues(transactions);
            trendData.put("trendDirection", calculateTrend(monthlyProfits));
            double vol = calculateVolatility(monthlyProfits);
            double avg = monthlyProfits.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            trendData.put("volatility", vol);
            trendData.put("currentAverage", avg);
            // rough weekly estimate from monthly average
            trendData.put("predictedWeeklyProfit", avg / 4.0);
            trendData.put("confidenceLevel", confidenceLabel(monthlyProfits.size()));
            trendData.put("seasonalPattern", seasonalityLabel(avg, vol));
        }

        trendData.put("peakDayPrediction", predictPeakDayOfWeek(transactions));
        
        return trendData;
    }

    private String findPeakProfitDayLabel(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) return "N/A";

        Map<LocalDate, Double> daily = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getDate() == null) continue;
            LocalDate d = t.getDate().toLocalDate();
            daily.merge(d, profitFromTransaction(t), Double::sum);
        }
        return daily.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().toString())
                .orElse("N/A");
    }

    private String predictPeakDayOfWeek(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) return "N/A";

        Map<DayOfWeek, Double> byDow = new EnumMap<>(DayOfWeek.class);
        for (Transaction t : transactions) {
            if (t.getDate() == null) continue;
            DayOfWeek dow = t.getDate().toLocalDate().getDayOfWeek();
            byDow.merge(dow, profitFromTransaction(t), Double::sum);
        }
        return byDow.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().toString())
                .orElse("N/A");
    }

    private double profitFromTransaction(Transaction t) {
        if (t == null || t.getType() == null) return 0.0;
        // Profit is the company 5% component on chargeable transactions.
        if (t.getType() == TransactionType.INCOME) return 0.0;
        return t.getAmount() * ChargeCalculator.BASE_PROFIT_DEFAULT;
    }

    private String confidenceLabel(int sampleSize) {
        if (sampleSize >= 12) return "High";
        if (sampleSize >= 6) return "Medium";
        return "Low";
    }

    private String seasonalityLabel(double mean, double stdev) {
        if (mean <= 0) return null;
        double cv = stdev / mean; // coefficient of variation
        if (cv >= 0.6) return "high";
        if (cv >= 0.3) return "medium";
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
        Map<LocalDate, Double> dailyProfits = new HashMap<>();
        
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                LocalDate date = t.getDate().toLocalDate();
                dailyProfits.merge(date, profitFromTransaction(t), Double::sum);
            }
        }
        
        List<Map<String, Object>> datasets = new ArrayList<>();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Daily Profit");
        dataset.put("data", dailyProfits.values());
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
                // Calculate week number using TemporalAdjusters
                LocalDate startOfWeek = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                String weekLabel = startOfWeek.toString(); // Use the Monday date as week identifier
                weeks.put(weekLabel, weekLabel);
            }
        }
        List<String> result = new ArrayList<>(weeks.values());
        Collections.sort(result);
        return result;
    }

    private List<Map<String, Object>> getWeeklyProfitData(List<Transaction> transactions) {
        Map<String, Double> weeklyProfits = new HashMap<>();
        
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                LocalDate date = t.getDate().toLocalDate();
                LocalDate startOfWeek = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                String weekLabel = startOfWeek.toString();
                weeklyProfits.merge(weekLabel, profitFromTransaction(t), Double::sum);
            }
        }
        
        List<Map<String, Object>> datasets = new ArrayList<>();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Weekly Profit");
        dataset.put("data", weeklyProfits.values());
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
                String monthLabel = yearMonth.toString(); // Format: YYYY-MM
                months.put(yearMonth.toString(), monthLabel);
            }
        }
        List<String> result = new ArrayList<>(months.values());
        Collections.sort(result);
        return result;
    }

    private List<Map<String, Object>> getMonthlyProfitData(List<Transaction> transactions) {
        Map<String, Double> monthlyProfits = new HashMap<>();
        
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                YearMonth yearMonth = YearMonth.from(t.getDate().toLocalDate());
                String monthLabel = yearMonth.toString();
                monthlyProfits.merge(monthLabel, profitFromTransaction(t), Double::sum);
            }
        }
        
        List<Map<String, Object>> datasets = new ArrayList<>();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Monthly Profit");
        dataset.put("data", monthlyProfits.values());
        dataset.put("backgroundColor", "rgba(255, 159, 64, 0.2)");
        dataset.put("borderColor", "rgba(255, 159, 64, 1)");
        dataset.put("borderWidth", 2);
        datasets.add(dataset);
        
        return datasets;
    }

    private List<Double> getDailyProfitValues(List<Transaction> transactions) {
        Map<LocalDate, Double> dailyProfits = new HashMap<>();
        
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                LocalDate date = t.getDate().toLocalDate();
                dailyProfits.merge(date, profitFromTransaction(t), Double::sum);
            }
        }
        
        return new ArrayList<>(dailyProfits.values());
    }

    private List<Double> getWeeklyProfitValues(List<Transaction> transactions) {
        Map<String, Double> weeklyProfits = new HashMap<>();
        
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                LocalDate date = t.getDate().toLocalDate();
                LocalDate startOfWeek = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                String weekLabel = startOfWeek.toString();
                weeklyProfits.merge(weekLabel, profitFromTransaction(t), Double::sum);
            }
        }
        
        return new ArrayList<>(weeklyProfits.values());
    }

    private List<Double> getMonthlyProfitValues(List<Transaction> transactions) {
        Map<String, Double> monthlyProfits = new HashMap<>();
        
        for (Transaction t : transactions) {
            if (t.getDate() != null) {
                YearMonth yearMonth = YearMonth.from(t.getDate().toLocalDate());
                String monthLabel = yearMonth.toString();
                monthlyProfits.merge(monthLabel, profitFromTransaction(t), Double::sum);
            }
        }
        
        return new ArrayList<>(monthlyProfits.values());
    }

    private String calculateTrend(List<Double> values) {
        if (values.size() < 2) {
            return "insufficient_data";
        }
        
        double firstHalfAvg = 0;
        double secondHalfAvg = 0;
        int midPoint = values.size() / 2;
        
        for (int i = 0; i < midPoint; i++) {
            firstHalfAvg += values.get(i);
        }
        firstHalfAvg /= midPoint;
        
        for (int i = midPoint; i < values.size(); i++) {
            secondHalfAvg += values.get(i);
        }
        secondHalfAvg /= (values.size() - midPoint);
        
        if (secondHalfAvg > firstHalfAvg * 1.05) {
            return "increasing";
        } else if (secondHalfAvg < firstHalfAvg * 0.95) {
            return "decreasing";
        } else {
            return "stable";
        }
    }

    private double calculateVolatility(List<Double> values) {
        if (values.size() < 2) {
            return 0;
        }
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        
        return Math.sqrt(variance);
    }
}