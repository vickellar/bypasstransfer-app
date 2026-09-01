package com.bypass.bypasstransers.dto;

import java.time.LocalDateTime;

public class AuditLogFilterDTO {
    private String entityName;
    private String action;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long performedBy;
    private String performedByUsername;
    private int page = 0;
    private int size = 50;

    // Constructors
    public AuditLogFilterDTO() {}

    public AuditLogFilterDTO(String entityName, String action, LocalDateTime startDate, LocalDateTime endDate) {
        this.entityName = entityName;
        this.action = action;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public Long getPerformedBy() { return performedBy; }
    public void setPerformedBy(Long performedBy) { this.performedBy = performedBy; }

    public String getPerformedByUsername() { return performedByUsername; }
    public void setPerformedByUsername(String performedByUsername) { this.performedByUsername = performedByUsername; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
