package com.bypass.bypasstransers.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MatchedTransactionExportDTO {
    private Long matchId;
    private Long incomingTransactionId;
    private Long outgoingTransactionId;
    private BigDecimal incomingAmount;
    private BigDecimal outgoingAmount;
    private String incomingCurrency;
    private String outgoingCurrency;
    private String incomingAccount;
    private String outgoingAccount;
    private LocalDateTime incomingDate;
    private LocalDateTime outgoingDate;
    private String matchedBy;
    private LocalDateTime matchedAt;
    private String description;
    private String note;
    private String status;
    private BigDecimal discrepancy;

    // Constructors
    public MatchedTransactionExportDTO() {}

    // Getters and Setters
    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public Long getIncomingTransactionId() { return incomingTransactionId; }
    public void setIncomingTransactionId(Long incomingTransactionId) { this.incomingTransactionId = incomingTransactionId; }

    public Long getOutgoingTransactionId() { return outgoingTransactionId; }
    public void setOutgoingTransactionId(Long outgoingTransactionId) { this.outgoingTransactionId = outgoingTransactionId; }

    public BigDecimal getIncomingAmount() { return incomingAmount; }
    public void setIncomingAmount(BigDecimal incomingAmount) { this.incomingAmount = incomingAmount; }

    public BigDecimal getOutgoingAmount() { return outgoingAmount; }
    public void setOutgoingAmount(BigDecimal outgoingAmount) { this.outgoingAmount = outgoingAmount; }

    public String getIncomingCurrency() { return incomingCurrency; }
    public void setIncomingCurrency(String incomingCurrency) { this.incomingCurrency = incomingCurrency; }

    public String getOutgoingCurrency() { return outgoingCurrency; }
    public void setOutgoingCurrency(String outgoingCurrency) { this.outgoingCurrency = outgoingCurrency; }

    public String getIncomingAccount() { return incomingAccount; }
    public void setIncomingAccount(String incomingAccount) { this.incomingAccount = incomingAccount; }

    public String getOutgoingAccount() { return outgoingAccount; }
    public void setOutgoingAccount(String outgoingAccount) { this.outgoingAccount = outgoingAccount; }

    public LocalDateTime getIncomingDate() { return incomingDate; }
    public void setIncomingDate(LocalDateTime incomingDate) { this.incomingDate = incomingDate; }

    public LocalDateTime getOutgoingDate() { return outgoingDate; }
    public void setOutgoingDate(LocalDateTime outgoingDate) { this.outgoingDate = outgoingDate; }

    public String getMatchedBy() { return matchedBy; }
    public void setMatchedBy(String matchedBy) { this.matchedBy = matchedBy; }

    public LocalDateTime getMatchedAt() { return matchedAt; }
    public void setMatchedAt(LocalDateTime matchedAt) { this.matchedAt = matchedAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getDiscrepancy() { return discrepancy; }
    public void setDiscrepancy(BigDecimal discrepancy) { this.discrepancy = discrepancy; }
}
