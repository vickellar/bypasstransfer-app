package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.DailyReconciliation;
import com.bypass.bypasstransers.repository.DailyReconciliationRepository;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

@Controller
@RequestMapping("/reconciliations")
public class ReconciliationAdminController {

    @Autowired
    private DailyReconciliationRepository repo;

    @GetMapping
    public String list(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size) {
        Pageable p = PageRequest.of(page, size);
        Page<DailyReconciliation> pg = repo.findAll(p);
        model.addAttribute("reconciliationsPage", pg);
        model.addAttribute("reconciliations", pg.getContent());
        model.addAttribute("currentPage", page);
        return "reconciliations";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        repo.deleteById(id);
        return "redirect:/reconciliations";
    }

    @GetMapping("/export/csv")
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=reconciliations.csv");
        List<DailyReconciliation> list = repo.findAll();
        try (PrintWriter w = response.getWriter()) {
            w.println("Date,SystemBalance,ActualBalance,Difference");
            for (DailyReconciliation r : list) {
                w.printf("%s,%.2f,%.2f,%.2f\n", r.getDate(), r.getSystemBalance(), r.getActualBalance(), r.getDifference());
            }
        }
    }

    @GetMapping("/export/pdf")
    public void exportPdf(HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reconciliations.pdf");
        List<DailyReconciliation> list = repo.findAll();
        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.add(new Paragraph("Reconciliations"));
        for (DailyReconciliation r : list) {
            document.add(new Paragraph(r.getDate() + " | system=" + r.getSystemBalance() + " | actual=" + r.getActualBalance() + " | diff=" + r.getDifference()));
        }
        document.close();
    }
}
