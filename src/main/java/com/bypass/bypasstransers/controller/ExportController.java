package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.service.ExportService;
import com.bypass.bypasstransers.service.AuditService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

// iText PDF imports
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

    /**
     * Export transactions to Excel with optional date range filter.
     * Only ADMIN, SUPERVISOR, and SUPER_ADMIN can access this.
     */
    @GetMapping("/excel")
    public void exportToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response) throws Exception {

        // Filter transactions by date range if provided
        List<Transaction> transactions;
        String filename = "transactions";
        
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            transactions = repo.findAll().stream()
                .filter(t -> t.getDate() != null && 
                    !t.getDate().isBefore(start) && 
                    !t.getDate().isAfter(end))
                .collect(Collectors.toList());
            filename = "transactions_" + startDate.format(DateTimeFormatter.ISO_DATE) + "_to_" + endDate.format(DateTimeFormatter.ISO_DATE);
        } else {
            transactions = repo.findAll();
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");

            // Create header row
            Row header = sheet.createRow(0);
            String[] cols = {"ID", "From Account", "To Account", "Amount", "Fee", "Net Amount", "Reference", "Date", "Status"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            // Populate data rows
            int rowIdx = 1;
            for (Transaction t : transactions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getId() != null ? t.getId() : 0);
                row.createCell(1).setCellValue(t.getFromAccount() != null ? t.getFromAccount() : "");
                row.createCell(2).setCellValue(t.getToAccount() != null ? t.getToAccount() : "");
                row.createCell(3).setCellValue(t.getAmount());
                row.createCell(4).setCellValue(t.getFee());
                row.createCell(5).setCellValue(t.getNetAmount());
                row.createCell(6).setCellValue(t.getReference() != null ? t.getReference() : "");
                row.createCell(7).setCellValue(t.getDate() != null ? t.getDate().toString() : "");
                row.createCell(8).setCellValue(t.getStatus() != null ? t.getStatus().toString() : "");
            }

            // Auto-size columns
            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename + ".xlsx");
            workbook.write(response.getOutputStream());
        }

        // Audit log
        try {
            auditService.logEntity("admin", "transactions", null, "EXPORT_EXCEL", null, 
                "Exported " + transactions.size() + " transactions" + (startDate != null ? " from " + startDate + " to " + endDate : ""));
        } catch (Exception e) {
            // ignore audit failures
        }
    }

    /**
     * Export transactions to PDF with optional date range filter.
     * Only ADMIN, SUPERVISOR, and SUPER_ADMIN can access this.
     */
    @GetMapping("/pdf")
    public void exportToPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response) throws Exception {

        // Filter transactions by date range if provided
        List<Transaction> transactions;
        String filename = "transactions";
        
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            transactions = repo.findAll().stream()
                .filter(t -> t.getDate() != null && 
                    !t.getDate().isBefore(start) && 
                    !t.getDate().isAfter(end))
                .collect(Collectors.toList());
            filename = "transactions_" + startDate.format(DateTimeFormatter.ISO_DATE) + "_to_" + endDate.format(DateTimeFormatter.ISO_DATE);
        } else {
            transactions = repo.findAll();
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename + ".pdf");

        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4.rotate());

        // Add title
        String title = "Transaction Report";
        if (startDate != null && endDate != null) {
            title += " (" + startDate.format(DateTimeFormatter.ISO_DATE) + " to " + endDate.format(DateTimeFormatter.ISO_DATE) + ")";
        }
        document.add(new Paragraph(title).setFontSize(18).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .setFontSize(10).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("\n"));

        // Create table
        float[] columnWidths = {50, 100, 100, 80, 60, 80, 120, 150, 80};
        Table table = new Table(columnWidths);

        // Add headers
        String[] headers = {"ID", "From Account", "To Account", "Amount", "Fee", "Net Amount", "Reference", "Date", "Status"};
        for (String header : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(header)).setTextAlignment(TextAlignment.CENTER));
        }

        // Add data rows
        for (Transaction t : transactions) {
            table.addCell(new Cell().add(new Paragraph(t.getId() != null ? t.getId().toString() : "")));
            table.addCell(new Cell().add(new Paragraph(t.getFromAccount() != null ? t.getFromAccount() : "")));
            table.addCell(new Cell().add(new Paragraph(t.getToAccount() != null ? t.getToAccount() : "")));
            table.addCell(new Cell().add(new Paragraph(String.format("%.2f", t.getAmount()))).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Cell().add(new Paragraph(String.format("%.2f", t.getFee()))).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Cell().add(new Paragraph(String.format("%.2f", t.getNetAmount()))).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Cell().add(new Paragraph(t.getReference() != null ? t.getReference() : "")));
            table.addCell(new Cell().add(new Paragraph(t.getDate() != null ? t.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "")));
            table.addCell(new Cell().add(new Paragraph(t.getStatus() != null ? t.getStatus().toString() : "")));
        }

        document.add(table);

        // Add summary
        document.add(new Paragraph("\n"));
        double totalAmount = transactions.stream().mapToDouble(Transaction::getAmount).sum();
        double totalFee = transactions.stream().mapToDouble(Transaction::getFee).sum();
        double totalNet = transactions.stream().mapToDouble(Transaction::getNetAmount).sum();
        
        document.add(new Paragraph("Summary:"));
        document.add(new Paragraph("Total Transactions: " + transactions.size()));
        document.add(new Paragraph("Total Amount: " + String.format("%.2f", totalAmount)));
        document.add(new Paragraph("Total Fees: " + String.format("%.2f", totalFee)));
        document.add(new Paragraph("Total Net: " + String.format("%.2f", totalNet)));

        document.close();

        // Audit log
        try {
            auditService.logEntity("admin", "transactions", null, "EXPORT_PDF", null, 
                "Exported " + transactions.size() + " transactions to PDF" + (startDate != null ? " from " + startDate + " to " + endDate : ""));
        } catch (Exception e) {
            // ignore audit failures
        }
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        response.setHeader("Content-Disposition", "attachment; filename=transactions.xlsx");
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
                row.createCell(2).setCellValue(log.getEntityName() != null ? log.getEntityName() : "");
                row.createCell(3).setCellValue(log.getEntityId() != null ? log.getEntityId() : 0L);
                row.createCell(4).setCellValue(log.getAction() != null ? log.getAction() : "");
                row.createCell(5).setCellValue(log.getPerformedAt() != null ? log.getPerformedAt().toString() : "");
                row.createCell(6).setCellValue(log.getNewValue() != null ? log.getNewValue() : "");
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=audit_logs.xlsx");
            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/audit/pdf")
    public void exportAuditPdf(HttpServletResponse response) throws Exception {
        List<com.bypass.bypasstransers.model.AuditLog> logs = auditLogRepo.findAll();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=audit_logs.pdf");

        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4.rotate());

        document.add(new Paragraph("System Audit Logs").setFontSize(18).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("\n"));

        float[] columnWidths = {50, 100, 100, 80, 100, 120, 200};
        Table table = new Table(columnWidths);
        String[] headers = {"ID", "User ID", "Entity", "Entity ID", "Action", "Date", "Details"};
        for (String h : headers) table.addHeaderCell(new Cell().add(new Paragraph(h)).setTextAlignment(TextAlignment.CENTER));

        for (com.bypass.bypasstransers.model.AuditLog log : logs) {
            table.addCell(new Cell().add(new Paragraph(log.getId() != null ? log.getId().toString() : "")));
            table.addCell(new Cell().add(new Paragraph(log.getPerformedBy() != null ? log.getPerformedBy().toString() : "")));
            table.addCell(new Cell().add(new Paragraph(log.getEntityName() != null ? log.getEntityName() : "")));
            table.addCell(new Cell().add(new Paragraph(log.getEntityId() != null ? log.getEntityId().toString() : "")));
            table.addCell(new Cell().add(new Paragraph(log.getAction() != null ? log.getAction() : "")));
            table.addCell(new Cell().add(new Paragraph(log.getPerformedAt() != null ? log.getPerformedAt().toString() : "")));
            table.addCell(new Cell().add(new Paragraph(log.getNewValue() != null ? log.getNewValue() : "")));
        }
        document.add(table);
        document.close();
    }
}