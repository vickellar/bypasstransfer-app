package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.dto.StaffPerformanceDTO;
import com.bypass.bypasstransers.dto.AccountPerformanceDTO;
import com.bypass.bypasstransers.dto.ExpenditureDTO;
import com.bypass.bypasstransers.enums.TransactionType;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.service.AnalyticsService;
import com.bypass.bypasstransers.service.SecurityService;
import com.bypass.bypasstransers.util.ChargeCalculator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/analytics")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPERVISOR')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SecurityService securityService;
    private final TransactionRepository transactionRepository;

    public AnalyticsController(AnalyticsService analyticsService, SecurityService securityService,
                               TransactionRepository transactionRepository) {
        this.analyticsService = analyticsService;
        this.securityService = securityService;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping({"/", ""})
    public String analyticsDashboard(Model model) {
        User currentUser = securityService.getCurrentUser();
        
        List<StaffPerformanceDTO> staffPerformance = analyticsService.getStaffPerformance();
        model.addAttribute("staffPerformance", staffPerformance);
        
        BigDecimal totalStaffWalletBalance = staffPerformance.stream()
                .filter(Objects::nonNull)
                .map(StaffPerformanceDTO::getWalletBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalStaffWalletBalance", totalStaffWalletBalance.doubleValue());
        
        List<AccountPerformanceDTO> accountPerformance = analyticsService.getAccountPerformance();
        model.addAttribute("accountPerformance", accountPerformance);
        
        model.addAttribute("monthlyExpenditure", analyticsService.getMonthlyExpenditureSummary());
        model.addAttribute("totalExpenditureThisMonth", analyticsService.getTotalExpenditureThisMonth().doubleValue());
        
        model.addAttribute("topStaff", analyticsService.getTopPerformingStaff(5));
        model.addAttribute("topAccounts", analyticsService.getTopPerformingAccounts(3));
        
        model.addAttribute("user", currentUser);
        model.addAttribute("isSuperAdmin", securityService.isSuperAdmin());
        
        return "admin-analytics";
    }

    @GetMapping("/staff-performance")
    public String staffPerformanceDetail(Model model) {
        User currentUser = securityService.getCurrentUser();
        
        List<StaffPerformanceDTO> staffPerformance = analyticsService.getStaffPerformanceDetailed();
        model.addAttribute("staffPerformance", staffPerformance);
        
        int totalTransactions = staffPerformance.stream().mapToInt(StaffPerformanceDTO::getTotalTransactions).sum();
        BigDecimal totalAmount = staffPerformance.stream()
                .map(StaffPerformanceDTO::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFees = staffPerformance.stream()
                .map(StaffPerformanceDTO::getTotalFees)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWalletBalance = staffPerformance.stream()
                .map(StaffPerformanceDTO::getWalletBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        model.addAttribute("sumTransactions", totalTransactions);
        model.addAttribute("sumAmount", totalAmount.doubleValue());
        model.addAttribute("sumFees", totalFees.doubleValue());
        model.addAttribute("sumWalletBalance", totalWalletBalance.doubleValue());
        
        model.addAttribute("user", currentUser);
        
        return "staff-performance-detail";
    }

    @GetMapping("/account-performance")
    public String accountPerformanceDetail(Model model) {
        User currentUser = securityService.getCurrentUser();
        
        List<AccountPerformanceDTO> accountPerformance = analyticsService.getAccountPerformanceDetailed();
        model.addAttribute("accountPerformance", accountPerformance);
        
        int totalTransactions = accountPerformance.stream().mapToInt(AccountPerformanceDTO::getTotalTransactions).sum();
        BigDecimal totalAmount = accountPerformance.stream()
                .map(AccountPerformanceDTO::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalActiveUsers = accountPerformance.stream().mapToInt(AccountPerformanceDTO::getActiveUsers).sum();
        
        model.addAttribute("sumTransactions", totalTransactions);
        model.addAttribute("sumAmount", totalAmount.doubleValue());
        model.addAttribute("sumActiveUsers", totalActiveUsers);
        
        model.addAttribute("user", currentUser);
        
        return "account-performance-detail";
    }

    @GetMapping("/expenditures")
    public String expendituresList(Model model) {
        User currentUser = securityService.getCurrentUser();
        
        model.addAttribute("expenditures", analyticsService.getAllExpenditures());
        model.addAttribute("totalThisMonth", analyticsService.getTotalExpenditureThisMonth().doubleValue());
        model.addAttribute("totalThisYear", analyticsService.getTotalExpenditureThisYear().doubleValue());
        model.addAttribute("user", currentUser);
        
        return "expenditures-list";
    }

    @GetMapping("/expenditures/new")
    public String newExpenditureForm(Model model) {
        model.addAttribute("expenditure", new ExpenditureDTO());
        return "expenditure-form";
    }

    @PostMapping("/expenditures/save")
    public String saveExpenditure(@ModelAttribute ExpenditureDTO expenditure, RedirectAttributes ra) {
        try {
            analyticsService.saveExpenditure(expenditure);
            ra.addFlashAttribute("success", "Expenditure recorded");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed: " + e.getMessage());
        }
        return "redirect:/admin/analytics/expenditures";
    }

    @PostMapping("/expenditures/delete")
    public String deleteExpenditure(@RequestParam Long id, RedirectAttributes ra) {
        try {
            analyticsService.deleteExpenditure(id);
            ra.addFlashAttribute("success", "Deleted");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed: " + e.getMessage());
        }
        return "redirect:/admin/analytics/expenditures";
    }

    @GetMapping("/reports/expenditure")
    public String expenditureReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();
        
        model.addAttribute("expenditureReport", analyticsService.getExpenditureReport(startDate, endDate));
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("totalAmount", analyticsService.getTotalExpenditureForPeriod(startDate, endDate).doubleValue());
        
        return "expenditure-report";
    }
    
    @GetMapping("/profit-trends")
    public String profitTrends(@RequestParam(defaultValue = "30") int days, Model model) {
        model.addAttribute("user", securityService.getCurrentUser());
        model.addAttribute("isSuperAdmin", securityService.isSuperAdmin());

        LocalDate cutoff = LocalDate.now().minusDays(days);
        List<Transaction> recent = transactionRepository.findAll().stream()
                .filter(t -> t.getDate() != null && t.getDate().toLocalDate().isAfter(cutoff))
                .sorted(Comparator.comparing(Transaction::getDate))
                .collect(Collectors.toList());

        // Daily profit aggregation
        Map<LocalDate, BigDecimal> dailyProfit = new TreeMap<>();
        Map<LocalDate, BigDecimal> dailyVolume = new TreeMap<>();
        for (Transaction t : recent) {
            LocalDate d = t.getDate().toLocalDate();
            BigDecimal profit = profitFromTransaction(t);
            dailyProfit.merge(d, profit, BigDecimal::add);
            dailyVolume.merge(d, t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO, BigDecimal::add);
        }

        List<String> labels = new ArrayList<>();
        List<Double> profits = new ArrayList<>();
        List<Double> margins = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : dailyProfit.entrySet()) {
            labels.add(e.getKey().toString());
            profits.add(e.getValue().doubleValue());
            BigDecimal vol = dailyVolume.getOrDefault(e.getKey(), BigDecimal.ONE);
            if (vol.compareTo(BigDecimal.ZERO) == 0) vol = BigDecimal.ONE;
            margins.add(e.getValue().multiply(BigDecimal.valueOf(100))
                    .divide(vol, 2, RoundingMode.HALF_UP).doubleValue());
        }

        BigDecimal totalProfit = dailyProfit.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bestDay = dailyProfit.values().stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal worstDay = dailyProfit.values().stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        double growth = 0.0;
        if (profits.size() >= 2) {
            double first = profits.get(0);
            double last = profits.get(profits.size() - 1);
            if (first != 0) growth = ((last - first) / Math.abs(first)) * 100;
        }

        model.addAttribute("trendLabels", labels);
        model.addAttribute("trendProfits", profits);
        model.addAttribute("trendMargins", margins);
        model.addAttribute("totalProfit", totalProfit.doubleValue());
        model.addAttribute("profitGrowth", growth);
        model.addAttribute("bestDay", bestDay.doubleValue());
        model.addAttribute("worstDay", worstDay.doubleValue());
        model.addAttribute("days", days);

        return "profit-trends";
    }
    
    @GetMapping("/profit-forecast")
    public String profitForecast(@RequestParam(defaultValue = "30") int forecastDays, Model model) {
        model.addAttribute("user", securityService.getCurrentUser());
        model.addAttribute("isSuperAdmin", securityService.isSuperAdmin());

        // Use last 60 days of history to build forecast
        LocalDate cutoff = LocalDate.now().minusDays(60);
        List<Transaction> recent = transactionRepository.findAll().stream()
                .filter(t -> t.getDate() != null && t.getDate().toLocalDate().isAfter(cutoff))
                .sorted(Comparator.comparing(Transaction::getDate))
                .collect(Collectors.toList());

        Map<LocalDate, BigDecimal> dailyProfit = new TreeMap<>();
        for (Transaction t : recent) {
            LocalDate d = t.getDate().toLocalDate();
            dailyProfit.merge(d, profitFromTransaction(t), BigDecimal::add);
        }

        // Historical data
        List<String> histLabels = new ArrayList<>();
        List<Double> histValues = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : dailyProfit.entrySet()) {
            histLabels.add(e.getKey().toString());
            histValues.add(e.getValue().doubleValue());
        }

        // Simple linear forecast
        double avgDaily = histValues.isEmpty() ? 0 :
                histValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double projectedTotal = avgDaily * forecastDays;

        // Best/worst from history
        double peakDay = histValues.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double lowestDay = histValues.stream().mapToDouble(Double::doubleValue).min().orElse(0);

        // Forecast labels and values
        List<String> forecastLabels = new ArrayList<>();
        List<Double> forecastValues = new ArrayList<>();
        for (int i = 1; i <= forecastDays; i++) {
            LocalDate d = LocalDate.now().plusDays(i);
            forecastLabels.add(d.toString());
            forecastValues.add(avgDaily);
        }

        // Confidence based on sample size
        int sampleSize = histValues.size();
        int confidence = sampleSize >= 30 ? 90 : (sampleSize >= 14 ? 80 : (sampleSize >= 7 ? 70 : 50));

        // Scenarios
        double optimistic = projectedTotal * 1.15;
        double conservative = projectedTotal;
        double pessimistic = projectedTotal * 0.85;

        model.addAttribute("histLabels", histLabels);
        model.addAttribute("histValues", histValues);
        model.addAttribute("forecastLabels", forecastLabels);
        model.addAttribute("forecastValues", forecastValues);
        model.addAttribute("projectedTotal", projectedTotal);
        model.addAttribute("avgDaily", avgDaily);
        model.addAttribute("peakDay", peakDay);
        model.addAttribute("lowestDay", lowestDay);
        model.addAttribute("confidence", confidence);
        model.addAttribute("optimistic", optimistic);
        model.addAttribute("conservative", conservative);
        model.addAttribute("pessimistic", pessimistic);
        model.addAttribute("forecastDays", forecastDays);

        return "profit-forecast";
    }

    private BigDecimal profitFromTransaction(Transaction t) {
        if (t == null || t.getType() == null) return BigDecimal.ZERO;
        if (t.getType() == TransactionType.INCOME) return BigDecimal.ZERO;
        return (t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .multiply(ChargeCalculator.BASE_PROFIT_DEFAULT);
    }
}
