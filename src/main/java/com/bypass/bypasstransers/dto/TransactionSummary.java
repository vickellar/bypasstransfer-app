package com.bypass.bypasstransers.dto;

import java.math.BigDecimal;

public class TransactionSummary {
    private long transactionCount;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal totalFees = BigDecimal.ZERO;

    public TransactionSummary() {
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalFees() {
        return totalFees;
    }

    public void setTotalFees(BigDecimal totalFees) {
        this.totalFees = totalFees;
    }
}
