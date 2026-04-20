package com.bypass.bypasstransers.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import com.bypass.bypasstransers.enums.Currency;

public class ExpenditureDTO {
    private Long id;
    private String description;
    private String category; // Rent, Utilities, Salaries, Supplies, Other
    private BigDecimal amount = BigDecimal.ZERO;
    private LocalDate date;
    private String recordedBy;
    private String notes;
    private Currency currency;
    private Long walletId;
    
    public ExpenditureDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }

    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }
}
