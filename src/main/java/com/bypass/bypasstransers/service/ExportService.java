package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Export service to write transactions to an Excel file.
 */
@Service
public class ExportService {

    @Autowired
    private TransactionRepository txRepo;

    public void exportTransactions(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.xlsx");

        List<Transaction> list = txRepo.findAll();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Transactions");

            Row header = sheet.createRow(0);
            String[] cols = {"Date", "Type", "From", "To", "Amount", "Fee"};
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }

            int rowIdx = 1;
            for (Transaction tx : list) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(tx.getDate() != null ? tx.getDate().toString() : "");
                r.createCell(1).setCellValue(tx.getType() != null ? tx.getType().name() : "");
                r.createCell(2).setCellValue(tx.getFromAccount() != null ? tx.getFromAccount() : "");
                r.createCell(3).setCellValue(tx.getToAccount() != null ? tx.getToAccount() : "");
                
                // Convert BigDecimal to double for Excel cell compatibility
                r.createCell(4).setCellValue(tx.getAmount() != null ? tx.getAmount().doubleValue() : 0.0);
                r.createCell(5).setCellValue(tx.getFee() != null ? tx.getFee().doubleValue() : 0.0);
            }

            wb.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }
}