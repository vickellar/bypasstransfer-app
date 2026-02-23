package com.bypass.bypasstransers.dto;

public class StaffPerformanceDTO {
    private Long staffId;
    private String staffName;
    private int totalTransactions;
    private double totalAmount;
    private double totalFees;
    private double totalNet;
    private int walletCount;
    private double walletBalance;
    private String performanceLevel; // High, Medium, Low
    
    public StaffPerformanceDTO() {}
    
    public StaffPerformanceDTO(Long staffId, String staffName, int totalTransactions, 
                               double totalAmount, double totalFees, double totalNet) {
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
    
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    public double getTotalFees() { return totalFees; }
    public void setTotalFees(double totalFees) { this.totalFees = totalFees; }
    
    public double getTotalNet() { return totalNet; }
    public void setTotalNet(double totalNet) { this.totalNet = totalNet; }
    
    public int getWalletCount() { return walletCount; }
    public void setWalletCount(int walletCount) { this.walletCount = walletCount; }
    
    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) { this.walletBalance = walletBalance; }
    
    public String getPerformanceLevel() { return performanceLevel; }
    public void setPerformanceLevel(String performanceLevel) { this.performanceLevel = performanceLevel; }
}
