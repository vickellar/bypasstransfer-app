package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.service.ExportService;
import com.bypass.bypasstransers.service.AuditService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.geom.PageSize;

/**
 * Excel and PDF export for transactions. Restricted to ADMIN and SUPERVISOR roles.
 */
@RestController
@RequestMapping("/api/export")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'SUPER_ADMIN')")
public class ExportController {

    @Autowired
    private TransactionRepository repo;

    @Autowired
    private ExportService exportService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private com.bypass.bypasstransers.repository.AuditLogRepository auditLogRepo;

    @GetMapping("/excel")
    public void exportToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response) throws Exception {

        List<Transaction> transactions;
        String filename = "transactions";
        
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            transactions = repo.findAll().stream()
                .filter(t -> t.getDate() != null && !t.getDate().isBefore(start) && !t.getDate().isAfter(end))
                .collect(Collectors.toList());
            filename = "transactions_" + startDate + "_to_" + endDate;
        } else {
            transactions = repo.findAll();
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");
            Row header = sheet.createRow(0);
            String[] cols = {"ID", "From Account", "To Account", "Amount", "Fee", "Net Amount", "Reference", "Date", "Status"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

            int rowIdx = 1;
            for (Transaction t : transactions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getId() != null ? t.getId() : 0);
                row.createCell(1).setCellValue(t.getFromAccount() != null ? t.getFromAccount() : "");
                row.createCell(2).setCellValue(t.getToAccount() != null ? t.getToAccount() : "");
                row.createCell(3).setCellValue(t.getAmount().toString());
                row.createCell(4).setCellValue(t.getFee().toString());
                row.createCell(5).setCellValue(t.getNetAmount().toString());
                row.createCell(6).setCellValue(t.getReference() != null ? t.getReference() : "");
                row.createCell(7).setCellValue(t.getDate() != null ? t.getDate().toString() : "");
                row.createCell(8).setCellValue(t.getStatus() != null ? t.getStatus().toString() : "");
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename + ".xlsx");
            workbook.write(response.getOutputStream());
        }

        auditService.logEntity("admin", "transactions", null, "EXPORT_EXCEL", null, "Exported " + transactions.size() + " rows");
    }

    @GetMapping("/pdf")
    public void exportToPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response) throws Exception {

        List<Transaction> transactions;
        String filename = "transactions";
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            transactions = repo.findAll().stream()
                .filter(t -> t.getDate() != null && !t.getDate().isBefore(start) && !t.getDate().isAfter(end))
                .collect(Collectors.toList());
            filename = "transactions_" + startDate + "_to_" + endDate;
        } else {
            transactions = repo.findAll();
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename + ".pdf");

        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4.rotate());

        document.add(new Paragraph("Transaction Report").setFontSize(18).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("\n"));

        float[] columnWidths = {50, 100, 100, 80, 60, 80, 120, 150, 80};
        Table table = new Table(columnWidths);
        String[] headers = {"ID", "From Account", "To Account", "Amount", "Fee", "Net Amount", "Reference", "Date", "Status"};
        for (String h : headers) table.addHeaderCell(new Cell().add(new Paragraph(h)).setTextAlignment(TextAlignment.CENTER));

        for (Transaction t : transactions) {
            table.addCell(new Cell().add(new Paragraph(t.getId() != null ? t.getId().toString() : "")));
            table.addCell(new Cell().add(new Paragraph(t.getFromAccount() != null ? t.getFromAccount() : "")));
            table.addCell(new Cell().add(new Paragraph(t.getToAccount() != null ? t.getToAccount() : "")));
            table.addCell(new Cell().add(new Paragraph(t.getAmount().toString())).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Cell().add(new Paragraph(t.getFee().toString())).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Cell().add(new Paragraph(t.getNetAmount().toString())).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Cell().add(new Paragraph(t.getReference() != null ? t.getReference() : "")));
            table.addCell(new Cell().add(new Paragraph(t.getDate() != null ? t.getDate().toString() : "")));
            table.addCell(new Cell().add(new Paragraph(t.getStatus() != null ? t.getStatus().toString() : "")));
        }
        document.add(table);

        BigDecimal totalAmount = transactions.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFee = transactions.stream().map(Transaction::getFee).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = transactions.stream().map(Transaction::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        document.add(new Paragraph("\nSummary:"));
        document.add(new Paragraph("Count: " + transactions.size()));
        document.add(new Paragraph("Total Amount: " + totalAmount));
        document.add(new Paragraph("Total Fees: " + totalFee));
        document.add(new Paragraph("Total Net: " + totalNet));
        document.close();
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        exportService.exportTransactions(response);
    }

    @GetMapping("/audit/excel")
    public void exportAuditExcel(HttpServletResponse response) throws Exception {
        List<com.bypass.bypasstransers.model.AuditLog> logs = auditLogRepo.findAll();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Audit Logs");
            Row header = sheet.createRow(0);
            String[] cols = {"ID", "User", "Entity", "Entity ID", "Action", "Date", "Details"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

            int rowIdx = 1;
            for (com.bypass.bypasstransers.model.AuditLog log : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(log.getId() != null ? log.getId() : 0L);
                row.createCell(1).setCellValue(log.getPerformedBy() != null ? log.getPerformedBy().toString() : "");
                row.createCell(4).setCellValue(log.getAction() != null ? log.getAction() : "");
                row.createCell(5).setCellValue(log.getPerformedAt() != null ? log.getPerformedAt().toString() : "");
                row.createCell(6).setCellValue(log.getNewValue() != null ? log.getNewValue() : "");
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=audit_logs.xlsx");
            workbook.write(response.getOutputStream());
        }
    }
}