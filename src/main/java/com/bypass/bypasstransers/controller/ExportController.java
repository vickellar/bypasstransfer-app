package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Vickeller.01
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private TransactionRepository repo;

    @Autowired
    private ExportService exportService;

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        response.setHeader("Content-Disposition", "attachment; filename=transactions.xlsx");
        exportService.exportTransactions(response);
    }

    @GetMapping("/excel")
    public void exportToExcel(HttpServletResponse response) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");

            Row header = sheet.createRow(0);
            String[] cols = {"ID", "Account", "Direction", "Amount", "Charge", "Net", "Reference", "Date"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            List<Transaction> list = repo.findAll();
            int rowIdx = 1;
            for (Transaction t : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getId() != null ? t.getId() : 0);
                row.createCell(1).setCellValue(t.getFromAccount() != null ? t.getFromAccount() : "");
                row.createCell(2).setCellValue(t.getToAccount() != null ? t.getToAccount() : "");
                row.createCell(3).setCellValue(t.getAmount());
                row.createCell(4).setCellValue(t.getFee());
                row.createCell(5).setCellValue(t.getNetAmount());
                row.createCell(6).setCellValue(t.getReference() != null ? t.getReference() : "");
                row.createCell(7).setCellValue(t.getDate() != null ? t.getDate().toString() : "");
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=transactions.xlsx");
            workbook.write(response.getOutputStream());
        }
    }
}