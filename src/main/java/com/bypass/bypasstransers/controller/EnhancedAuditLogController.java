package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.dto.AuditLogFilterDTO;
import com.bypass.bypasstransers.model.AuditLog;
import com.bypass.bypasstransers.repository.AuditLogRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Enhanced Audit Log Controller with filtering, pagination, and export capabilities.
 */
@Controller
@RequestMapping("/audit")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class EnhancedAuditLogController {

    private final AuditLogRepository auditLogRepository;

    public EnhancedAuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Display audit logs with pagination and filtering.
     */
    @GetMapping("/logs")
    public String listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        
        // Build filter
        AuditLogFilterDTO filter = new AuditLogFilterDTO();
        filter.setEntityName(entityName);
        filter.setAction(action);
        filter.setPage(page);
        filter.setSize(size);
        
        // Parse dates if provided
        if (startDate != null && !startDate.isEmpty()) {
            filter.setStartDate(LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (endDate != null && !endDate.isEmpty()) {
            filter.setEndDate(LocalDateTime.parse(endDate + "T23:59:59"));
        }
        
        // Fetch logs with filter
        List<AuditLog> logs = fetchFilteredLogs(filter);
        
        model.addAttribute("logs", logs);
        model.addAttribute("filter", filter);
        model.addAttribute("totalRecords", auditLogRepository.count());
        
        return "audit-logs-enhanced";
    }

    /**
     * Export audit logs as CSV.
     */
    @GetMapping("/export-csv")
    public void exportAuditLogsCsv(
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {
        
        // Build filter
        AuditLogFilterDTO filter = new AuditLogFilterDTO();
        filter.setEntityName(entityName);
        filter.setAction(action);
        if (startDate != null) filter.setStartDate(LocalDateTime.parse(startDate + "T00:00:00"));
        if (endDate != null) filter.setEndDate(LocalDateTime.parse(endDate + "T23:59:59"));
        
        List<AuditLog> logs = fetchFilteredLogs(filter);
        
        // Set response headers
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=audit-logs-" + System.currentTimeMillis() + ".csv");
        
        // Write CSV
        try (CSVPrinter printer = new CSVPrinter(response.getWriter(), CSVFormat.DEFAULT)) {
            // Header
            printer.printRecord("ID", "Entity", "Entity ID", "Action", "Performed By", "Performed At", "Old Value", "New Value");
            
            // Data
            for (AuditLog log : logs) {
                printer.printRecord(
                    log.getId(),
                    log.getEntityName(),
                    log.getEntityId(),
                    log.getAction(),
                    log.getPerformedBy(),
                    log.getPerformedAt(),
                    log.getOldValue(),
                    log.getNewValue()
                );
            }
        }
    }

    /**
     * Get audit log details.
     */
    @GetMapping("/log/{id}")
    public String viewAuditLog(@PathVariable Long id, Model model) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit log not found: " + id));
        
        model.addAttribute("log", log);
        return "audit-log-detail";
    }

    /**
     * Search audit logs by entity ID.
     */
    @GetMapping("/search-entity/{entityId}")
    public String searchByEntity(@PathVariable Long entityId, Model model) {
        List<AuditLog> logs = auditLogRepository.findByEntityId(entityId);
        model.addAttribute("logs", logs);
        model.addAttribute("entityId", entityId);
        return "audit-logs-enhanced";
    }

    // Helper method to fetch filtered logs
    private List<AuditLog> fetchFilteredLogs(AuditLogFilterDTO filter) {
        Sort sort = Sort.by(Sort.Direction.DESC, "performedAt");
        
        // Apply filters - you'll need to add custom repository methods for complex filtering
        if (filter.getEntityName() != null && filter.getAction() != null && 
            filter.getStartDate() != null && filter.getEndDate() != null) {
            return auditLogRepository.findByEntityNameAndActionAndPerformedAtBetween(
                filter.getEntityName(), 
                filter.getAction(), 
                filter.getStartDate(), 
                filter.getEndDate(),
                sort
            );
        } else if (filter.getEntityName() != null && filter.getAction() != null) {
            return auditLogRepository.findByEntityNameAndAction(filter.getEntityName(), filter.getAction(), sort);
        } else if (filter.getStartDate() != null && filter.getEndDate() != null) {
            return auditLogRepository.findByPerformedAtBetween(filter.getStartDate(), filter.getEndDate(), sort);
        } else {
            return auditLogRepository.findAll(sort);
        }
    }
}
