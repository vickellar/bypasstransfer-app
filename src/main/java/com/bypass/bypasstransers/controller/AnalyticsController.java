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
        
        // Staff Performance Data
        List<StaffPerformanceDTO> staffPerformance = analyticsService.getStaffPerformance();
        model.addAttribute("staffPerformance", staffPerformance);
        double totalStaffWalletBalance = staffPerformance.stream()
                .filter(Objects::nonNull)
                .mapToDouble(StaffPerformanceDTO::getWalletBalance)
                .sum();
        model.addAttribute("totalStaffWalletBalance", totalStaffWalletBalance);
        
        // Account Performance Data
        List<AccountPerformanceDTO> accountPerformance = analyticsService.getAccountPerformance();
        model.addAttribute("accountPerformance", accountPerformance);
        
        // Expenditure Summary
        model.addAttribute("monthlyExpenditure", analyticsService.getMonthlyExpenditureSummary());
        model.addAttribute("totalExpenditureThisMonth", analyticsService.getTotalExpenditureThisMonth());
        
        // Top Performers
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
        model.addAttribute("user", currentUser);
        
        return "staff-performance-detail";
    }

    @GetMapping("/account-performance")
    public String accountPerformanceDetail(Model model) {
        User currentUser = securityService.getCurrentUser();
        
        List<AccountPerformanceDTO> accountPerformance = analyticsService.getAccountPerformanceDetailed();
        model.addAttribute("accountPerformance", accountPerformance);
        model.addAttribute("user", currentUser);
        
        return "account-performance-detail";
    }

    // Expenditure Management
    @GetMapping("/expenditures")
    public String expendituresList(Model model) {
        User currentUser = securityService.getCurrentUser();
        
        model.addAttribute("expenditures", analyticsService.getAllExpenditures());
        model.addAttribute("totalThisMonth", analyticsService.getTotalExpenditureThisMonth());
        model.addAttribute("totalThisYear", analyticsService.getTotalExpenditureThisYear());
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
            ra.addFlashAttribute("success", "Expenditure recorded successfully");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to save expenditure: " + e.getMessage());
        }
        return "redirect:/admin/analytics/expenditures";
    }

    @PostMapping("/expenditures/delete")
    public String deleteExpenditure(@RequestParam Long id, RedirectAttributes ra) {
        try {
            analyticsService.deleteExpenditure(id);
            ra.addFlashAttribute("success", "Expenditure deleted");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete: " + e.getMessage());
        }
        return "redirect:/admin/analytics/expenditures";
    }

    // Reports
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
        model.addAttribute("totalAmount", analyticsService.getTotalExpenditureForPeriod(startDate, endDate));
        
        return "expenditure-report";
    }
    
    // Advanced Profit Analytics Endpoints
    @GetMapping("/profit-trends")
    public String profitTrends(Model model) {
        User currentUser = securityService.getCurrentUser();
        
        // Calculate profit trends
        model.addAttribute("user", currentUser);
        model.addAttribute("isSuperAdmin", securityService.isSuperAdmin());
        
        return "profit-trends";
    }
    
    @GetMapping("/profit-forecast")
    public String profitForecast(Model model) {
        User currentUser = securityService.getCurrentUser();
        
        // Calculate profit forecasts
        model.addAttribute("user", currentUser);
        model.addAttribute("isSuperAdmin", securityService.isSuperAdmin());
        
        return "profit-forecast";
    }
}
