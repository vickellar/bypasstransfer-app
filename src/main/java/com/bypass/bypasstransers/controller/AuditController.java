package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.repository.AuditLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/audit")
    public String listAuditLogs(Model model) {
        model.addAttribute("logs", auditLogRepository.findAll());
        return "audit-logs";
    }
}
