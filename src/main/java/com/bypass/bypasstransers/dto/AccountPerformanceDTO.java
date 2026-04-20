package com.bypass.bypasstransers.dto;

import java.math.BigDecimal;

public class AccountPerformanceDTO {
    private String accountType; // Mukuru, Econet, Innbucks
    private int totalTransactions;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal totalFees = BigDecimal.ZERO;
    private BigDecimal totalNet = BigDecimal.ZERO;
    private int activeUsers;
    private BigDecimal percentageOfTotal = BigDecimal.ZERO;
    private String performanceLevel; // High, Medium, Low
    
    public AccountPerformanceDTO() {}
    
    public AccountPerformanceDTO(String accountType, int totalTransactions, 
                                 BigDecimal totalAmount, BigDecimal totalFees, BigDecimal totalNet) {
        this.accountType = accountType;
        this.totalTransactions = totalTransactions;
        this.totalAmount = totalAmount;
        this.totalFees = totalFees;
        this.totalNet = totalNet;
    }

    // Getters and Setters
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    
    public int getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public BigDecimal getTotalFees() { return totalFees; }
    public void setTotalFees(BigDecimal totalFees) { this.totalFees = totalFees; }
    
    public BigDecimal getTotalNet() { return totalNet; }
    public void setTotalNet(BigDecimal totalNet) { this.totalNet = totalNet; }
    
    public int getActiveUsers() { return activeUsers; }
    public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }
    
    public BigDecimal getPercentageOfTotal() { return percentageOfTotal; }
    public void setPercentageOfTotal(BigDecimal percentageOfTotal) { this.percentageOfTotal = percentageOfTotal; }
    
    public String getPerformanceLevel() { return performanceLevel; }
    public void setPerformanceLevel(String performanceLevel) { this.performanceLevel = performanceLevel; }
}
