package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.enums.TransactionType;
import com.bypass.bypasstransers.util.ChargeCalculator;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Report generation utilities
 */
@Service
public class ReportService {

    @Autowired
    private TransactionRepository txRepo;

    public BigDecimal profit() {
        return txRepo.findAll()
                .stream()
                .filter(t -> t.getType() != null && t.getType() != TransactionType.INCOME)
                .map(t -> t.getAmount().multiply(ChargeCalculator.BASE_PROFIT_DEFAULT))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void generateDailyReport(LocalDate date, OutputStream out) throws Exception {

        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("DAILY TRANSACTION REPORT"));
        document.add(new Paragraph("Date: " + date));

        List<Transaction> txs = txRepo.findAll()
                .stream()
                .filter(t -> t.getDate() != null && t.getDate().toLocalDate().equals(date))
                .toList();

        for (Transaction tx : txs) {
            document.add(new Paragraph(
                    tx.getDate() + " | "
                    + (tx.getType() != null ? tx.getType().name() : "") + " | "
                    + tx.getAmount() + " | Fee: " + tx.getFee()
            ));
        }

        document.close();
    }
}