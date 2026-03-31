package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.service.TransactionMatchService;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles end-of-day transaction matching: pairing incoming (RUB received)
 * with outgoing (USD sent) transactions for admin/supervisor reconciliation.
 */
@Controller
@RequestMapping("/admin/transaction-matching")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUPERVISOR')")
public class TransactionMatchController {

    @Autowired
    private TransactionMatchService service;

    /** Show matching dashboard: unmatched transactions + existing match table. */
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("allTransactions", service.getAllTransactions());
        model.addAttribute("unmatchedTransactions", service.getUnmatchedTransactions());
        model.addAttribute("matches", service.getAllMatches());
        return "transaction-matching";
    }

    /** Create a new match between an incoming and outgoing transaction. */
    @PostMapping("/match")
    public String createMatch(
            @RequestParam Long incomingId,
            @RequestParam Long outgoingId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String note,
            Principal principal,
            RedirectAttributes ra) {

        try {
            service.createMatch(incomingId, outgoingId, description, note,
                    principal != null ? principal.getName() : "system");
            ra.addFlashAttribute("successMessage", "Transactions matched successfully.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/transaction-matching";
    }

    /** Update the status of a match (PENDING / CONFIRMED / DISPUTED). */
    @PostMapping("/status/{id}")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes ra) {

        try {
            service.updateStatus(id, status);
            ra.addFlashAttribute("successMessage", "Status updated to " + status + ".");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/transaction-matching";
    }

    /** Delete (unmatch) a match — both transactions return to the unmatched pool. */
    @PostMapping("/delete/{id}")
    public String deleteMatch(@PathVariable Long id, RedirectAttributes ra) {
        service.deleteMatch(id);
        ra.addFlashAttribute("successMessage", "Match removed. Transactions are now unmatched.");
        return "redirect:/admin/transaction-matching";
    }
}
