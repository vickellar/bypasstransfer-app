package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.dto.MatchedTransactionExportDTO;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.TransactionMatch;
import com.bypass.bypasstransers.service.TransactionMatchService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for viewing and exporting matched transactions.
 */
@Controller
@RequestMapping("/transactions/matched")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPERVISOR')")
public class MatchedTransactionViewController {

    private final TransactionMatchService transactionMatchService;

    public MatchedTransactionViewController(TransactionMatchService transactionMatchService) {
        this.transactionMatchService = transactionMatchService;
    }

    /**
     * Display all matched transactions with filters.
     */
    @GetMapping
    public String listMatchedTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        
        List<TransactionMatch> matches = transactionMatchService.getAllMatches();
        
        // Apply status filter if provided
        if (status != null && !status.isEmpty()) {
            matches = matches.stream()
                .filter(m -> m.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
        }
        
        // Apply date filters if provided
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            LocalDate start = LocalDate.parse(startDate, formatter);
            LocalDate end = LocalDate.parse(endDate, formatter);
            
            matches = matches.stream()
                .filter(m -> {
                    LocalDate matchDate = m.getMatchedAt().toLocalDate();
                    return !matchDate.isBefore(start) && !matchDate.isAfter(end);
                })
                .collect(Collectors.toList());
        }
        
        // Convert to DTOs for display
        List<MatchedTransactionExportDTO> displayMatches = matches.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        model.addAttribute("matches", displayMatches);
        model.addAttribute("matchCount", displayMatches.size());
        model.addAttribute("confirmedCount", displayMatches.stream().filter(m -> "CONFIRMED".equals(m.getStatus())).count());
        model.addAttribute("pendingCount", displayMatches.stream().filter(m -> "PENDING".equals(m.getStatus())).count());
        model.addAttribute("disputedCount", displayMatches.stream().filter(m -> "DISPUTED".equals(m.getStatus())).count());
        
        return "matched-transactions-view";
    }

    /**
     * View details of a specific matched transaction.
     */
    @GetMapping("/{matchId}")
    public String viewMatchDetails(@PathVariable Long matchId, Model model) {
        // Fetch match details (you'll need to add a get method to the service)
        model.addAttribute("matchId", matchId);
        return "matched-transaction-detail";
    }

    /**
     * Export matched transactions as CSV.
     */
    @GetMapping("/export/csv")
    public void exportMatchedTransactionsCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {
        
        List<TransactionMatch> matches = transactionMatchService.getAllMatches();
        
        // Apply filters
        if (status != null && !status.isEmpty()) {
            matches = matches.stream()
                .filter(m -> m.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
        }
        
        // Convert to DTOs
        List<MatchedTransactionExportDTO> exportData = matches.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        // Set response headers
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", 
            "attachment; filename=matched-transactions-" + System.currentTimeMillis() + ".csv");
        
        // Write CSV
        try (CSVPrinter printer = new CSVPrinter(response.getWriter(), CSVFormat.DEFAULT)) {
            // Header
            printer.printRecord(
                "Match ID", "Incoming ID", "Outgoing ID", "Incoming Amount", "Outgoing Amount",
                "Currency", "Matched By", "Matched At", "Status", "Discrepancy", "Description"
            );
            
            // Data
            for (MatchedTransactionExportDTO match : exportData) {
                printer.printRecord(
                    match.getMatchId(),
                    match.getIncomingTransactionId(),
                    match.getOutgoingTransactionId(),
                    match.getIncomingAmount(),
                    match.getOutgoingAmount(),
                    match.getIncomingCurrency(),
                    match.getMatchedBy(),
                    match.getMatchedAt(),
                    match.getStatus(),
                    match.getDiscrepancy(),
                    match.getDescription()
                );
            }
        }
    }

    /**
     * Update match status (PENDING -> CONFIRMED or DISPUTED).
     */
    @PostMapping("/{matchId}/status")
    @ResponseBody
    public String updateMatchStatus(
            @PathVariable Long matchId,
            @RequestParam String status,
            @RequestParam(required = false) String notes) {
        
        try {
            transactionMatchService.updateStatus(matchId, status);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    /**
     * Delete a match (unlink the pair).
     */
    @DeleteMapping("/{matchId}")
    @ResponseBody
    public String deleteMatch(@PathVariable Long matchId) {
        try {
            transactionMatchService.deleteMatch(matchId);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    // Helper method to convert TransactionMatch to DTO
    private MatchedTransactionExportDTO convertToDTO(TransactionMatch match) {
        MatchedTransactionExportDTO dto = new MatchedTransactionExportDTO();
        
        Transaction incoming = match.getIncomingTransaction();
        Transaction outgoing = match.getOutgoingTransaction();
        
        dto.setMatchId(match.getId());
        dto.setIncomingTransactionId(incoming.getId());
        dto.setOutgoingTransactionId(outgoing.getId());
        dto.setIncomingAmount(incoming.getAmount());
        dto.setOutgoingAmount(outgoing.getAmount());
        dto.setIncomingCurrency(incoming.getCurrency());
        dto.setOutgoingCurrency(outgoing.getCurrency());
        dto.setIncomingDate(incoming.getDate());
        dto.setOutgoingDate(outgoing.getDate());
        dto.setMatchedBy(match.getMatchedBy());
        dto.setMatchedAt(match.getMatchedAt());
        dto.setDescription(match.getDescription());
        dto.setNote(match.getNote());
        dto.setStatus(match.getStatus());
        dto.setDiscrepancy(incoming.getAmount().subtract(outgoing.getAmount()).abs());
        
        return dto;
    }
}
