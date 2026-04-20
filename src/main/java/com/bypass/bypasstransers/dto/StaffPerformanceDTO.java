package com.bypass.bypasstransers.dto;

import java.math.BigDecimal;

public class StaffPerformanceDTO {
    private Long staffId;
    private String staffName;
    private int totalTransactions;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal totalFees = BigDecimal.ZERO;
    private BigDecimal totalNet = BigDecimal.ZERO;
    private int walletCount;
    private BigDecimal walletBalance = BigDecimal.ZERO;
    private String performanceLevel; // High, Medium, Low
    
    public StaffPerformanceDTO() {}
    
    public StaffPerformanceDTO(Long staffId, String staffName, int totalTransactions, 
                               BigDecimal totalAmount, BigDecimal totalFees, BigDecimal totalNet) {
        this.staffId = staffId;
        this.staffName = staffName;
        this.totalTransactions = totalTransactions;
        this.totalAmount = totalAmount;
        this.totalFees = totalFees;
        this.totalNet = totalNet;
    }

    // Getters and Setters
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    
    public int getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public BigDecimal getTotalFees() { return totalFees; }
    public void setTotalFees(BigDecimal totalFees) { this.totalFees = totalFees; }
    
    public BigDecimal getTotalNet() { return totalNet; }
    public void setTotalNet(BigDecimal totalNet) { this.totalNet = totalNet; }
    
    public int getWalletCount() { return walletCount; }
    public void setWalletCount(int walletCount) { this.walletCount = walletCount; }
    
    public BigDecimal getWalletBalance() { return walletBalance; }
    public void setWalletBalance(BigDecimal walletBalance) { this.walletBalance = walletBalance; }
    
    public String getPerformanceLevel() { return performanceLevel; }
    public void setPerformanceLevel(String performanceLevel) { this.performanceLevel = performanceLevel; }
}
