package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.DailyReconciliation;
import com.bypass.bypasstransers.repository.DailyReconciliationRepository;
import com.bypass.bypasstransers.service.ReconsiliationService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
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
import jakarta.servlet.http.HttpServletResponse;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

/**
 * Controller for administrative reconciliation tasks.
 */
@Controller
@RequestMapping("/reconciliations")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUPERVISOR')")
public class ReconciliationAdminController {

    @Autowired
    private DailyReconciliationRepository repo;

    @Autowired
    private ReconsiliationService reconService;

    @GetMapping
    public String list(Model model,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String staff) {
        List<DailyReconciliation> reconciliations;

        if (status != null && !status.isEmpty()) {
            reconciliations = repo.findByStatusOrderByCreatedAtDesc(status);
        } else {
            reconciliations = repo.findAllByOrderByCreatedAtDesc();
        }

        // Filter by staff if specified
        if (staff != null && !staff.isEmpty()) {
            final String staffFilter = staff;
            reconciliations = reconciliations.stream()
                    .filter(r -> r.getReconciledBy() != null && r.getReconciledBy().equalsIgnoreCase(staffFilter))
                    .toList();
        }

        long pendingCount = reconService.countPendingAndFlagged();

        model.addAttribute("reconciliations", reconciliations);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedStaff", staff);
        return "reconciliations";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable(required = true) Long id,
                          @RequestParam(required = false) String reviewNotes,
                          RedirectAttributes ra) {
        try {
            Objects.requireNonNull(id, "Reconciliation ID must not be null");
            reconService.approve(id, reviewNotes);
            ra.addFlashAttribute("success", "Reconciliation #" + id + " approved.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to approve: " + e.getMessage());
        }
        return "redirect:/reconciliations";
    }

    @PostMapping("/{id}/flag")
    public String flag(@PathVariable(required = true) Long id,
                       @RequestParam(required = false) String reviewNotes,
                       RedirectAttributes ra) {
        try {
            Objects.requireNonNull(id, "Reconciliation ID must not be null");
            reconService.flag(id, reviewNotes);
            ra.addFlashAttribute("success", "Reconciliation #" + id + " flagged for further review.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to flag: " + e.getMessage());
        }
        return "redirect:/reconciliations";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable(required = true) Long id, RedirectAttributes ra) {
        try {
            Objects.requireNonNull(id, "Reconciliation ID must not be null");
            repo.deleteById(id);
            ra.addFlashAttribute("success", "Reconciliation deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete: " + e.getMessage());
        }
        return "redirect:/reconciliations";
    }

    @GetMapping("/export/csv")
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=reconciliations.csv");
        List<DailyReconciliation> list = repo.findAllByOrderByCreatedAtDesc();
        try (PrintWriter w = response.getWriter()) {
            w.println("Date,Account,SystemBalance,ActualBalance,Difference,Status,ReconciledBy,Week,Year");
            for (DailyReconciliation r : list) {
                w.printf("%s,%s,%.2f,%.2f,%.2f,%s,%s,%s,%s\n",
                        r.getDate(),
                        r.getAccountName() != null ? r.getAccountName() : "",
                        r.getSystemBalance() != null ? r.getSystemBalance().doubleValue() : 0.0, 
                        r.getActualBalance() != null ? r.getActualBalance().doubleValue() : 0.0, 
                        r.getDifference() != null ? r.getDifference().doubleValue() : 0.0,
                        r.getStatus() != null ? r.getStatus() : "",
                        r.getReconciledBy() != null ? r.getReconciledBy() : "",
                        r.getWeekNumber() != null ? r.getWeekNumber() : "",
                        r.getYear() != null ? r.getYear() : "");
            }
        }
    }

    @GetMapping("/export/pdf")
    public void exportPdf(HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reconciliations.pdf");
        List<DailyReconciliation> list = repo.findAllByOrderByCreatedAtDesc();
        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.add(new Paragraph("ByPass Transfers — Reconciliation Report"));
        document.add(new Paragraph(" "));
        for (DailyReconciliation r : list) {
            String line = String.format("%s | %s | System: $%.2f | Actual: $%.2f | Diff: $%.2f | %s | By: %s | W%s/%s",
                    r.getDate(),
                    r.getAccountName() != null ? r.getAccountName() : "N/A",
                    r.getSystemBalance() != null ? r.getSystemBalance().doubleValue() : 0.0, 
                    r.getActualBalance() != null ? r.getActualBalance().doubleValue() : 0.0, 
                    r.getDifference() != null ? r.getDifference().doubleValue() : 0.0,
                    r.getStatus() != null ? r.getStatus() : "N/A",
                    r.getReconciledBy() != null ? r.getReconciledBy() : "N/A",
                    r.getWeekNumber() != null ? r.getWeekNumber() : "?",
                    r.getYear() != null ? r.getYear() : "?");
            document.add(new Paragraph(line));
        }
        document.close();
    }
}
