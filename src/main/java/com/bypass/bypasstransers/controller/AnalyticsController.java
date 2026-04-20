package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.dto.StaffPerformanceDTO;
import com.bypass.bypasstransers.dto.AccountPerformanceDTO;
import com.bypass.bypasstransers.dto.ExpenditureDTO;
import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.service.AnalyticsService;
import com.bypass.bypasstransers.service.SecurityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/admin/analytics")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPERVISOR')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SecurityService securityService;

    public AnalyticsController(AnalyticsService analyticsService, SecurityService securityService) {
        this.analyticsService = analyticsService;
        this.securityService = securityService;
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
    public String profitTrends(Model model) {
        model.addAttribute("user", securityService.getCurrentUser());
        model.addAttribute("isSuperAdmin", securityService.isSuperAdmin());
        return "profit-trends";
    }
    
    @GetMapping("/profit-forecast")
    public String profitForecast(Model model) {
        model.addAttribute("user", securityService.getCurrentUser());
        model.addAttribute("isSuperAdmin", securityService.isSuperAdmin());
        return "profit-forecast";
    }
}
