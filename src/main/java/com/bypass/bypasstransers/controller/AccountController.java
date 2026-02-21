package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.repository.AccountRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.DailyReconciliationRepository;
import com.bypass.bypasstransers.service.AlertService;
import com.bypass.bypasstransers.service.TransactionService;
import com.bypass.bypasstransers.service.ReconsiliationService;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;


@Controller
public class AccountController {

    @Autowired
    private AccountRepository accountRepo;

    @Autowired
    private TransactionRepository txRepo;

    @Autowired
    private TransactionService service;

    @Autowired
    private ReconsiliationService reconService;

    @Autowired
    private DailyReconciliationRepository dailyRepo;

    @Autowired
    private AlertService alertService;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("accounts", accountRepo.findAll());
        model.addAttribute("transactions", txRepo.findAll());
        model.addAttribute("totalFees", service.totalFees());
        model.addAttribute("lowBalanceAccounts", alertService.lowBalanceAccounts());
        model.addAttribute("reconciliations", dailyRepo.findAll());
        return "index";
    }

    // Explicit dashboard mapping used as post-login landing to avoid redirect loops
    @GetMapping("/dashboard")
    public String dashboardAlias(Model model) {
        return dashboard(model);
    }

    @PreAuthorize("hasAnyRole('STAFF','SUPERVISOR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/receive")
    public String receive(@RequestParam String account, @RequestParam double amount, RedirectAttributes ra) {
        try {
            service.receive(account, amount);
            ra.addFlashAttribute("success", "Received " + amount + " into " + account);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/";
    }

    @PreAuthorize("hasAnyRole('STAFF','SUPERVISOR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/send")
    public String send(@RequestParam String account, @RequestParam double amount, RedirectAttributes ra) {
        try {
            service.send(account, amount);
            ra.addFlashAttribute("success", "Sent " + amount + " from " + account);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/";
    }

    @PreAuthorize("hasAnyRole('STAFF','SUPERVISOR','ADMIN','SUPER_ADMIN')")
    @PostMapping("/transfer")
    public String transfer(@RequestParam String from, @RequestParam String to, @RequestParam double amount, RedirectAttributes ra) {
        try {
            service.transfer(from, to, amount);
            ra.addFlashAttribute("success", "Transferred " + amount + " from " + from + " to " + to);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/";
    }

    // Handle reconciliation form POST from the dashboard
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPERVISOR')")
    @PostMapping("/reconcile")
    public String reconcile(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam double actualBalance,
            RedirectAttributes redirectAttributes) {
        LocalDate d = date != null ? date : LocalDate.now();
        try {
            reconService.reconcile(d, actualBalance);
            redirectAttributes.addFlashAttribute("success", "Reconciliation saved for " + d);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Reconciliation failed: " + ex.getMessage());
        }
        return "redirect:/";
    }
}