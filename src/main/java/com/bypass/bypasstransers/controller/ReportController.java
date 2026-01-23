package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Vickeller.01
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void dailyReport(HttpServletResponse response) throws Exception {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=daily-report.pdf");

        reportService.generateDailyReport(LocalDate.now(), response.getOutputStream());
    }

}