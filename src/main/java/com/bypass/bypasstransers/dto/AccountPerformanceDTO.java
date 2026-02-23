package com.bypass.bypasstransers.dto;

public class AccountPerformanceDTO {
    private String accountType; // Mukuru, Econet, Innbucks
    private int totalTransactions;
    private double totalAmount;
    private double totalFees;
    private double totalNet;
    private int activeUsers;
    private double percentageOfTotal;
    private String performanceLevel; // High, Medium, Low
    
    public AccountPerformanceDTO() {}
    
    public AccountPerformanceDTO(String accountType, int totalTransactions, 
                                 double totalAmount, double totalFees, double totalNet) {
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
    
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    
    public double getTotalFees() { return totalFees; }
    public void setTotalFees(double totalFees) { this.totalFees = totalFees; }
    
    public double getTotalNet() { return totalNet; }
    public void setTotalNet(double totalNet) { this.totalNet = totalNet; }
    
    public int getActiveUsers() { return activeUsers; }
    public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }
    
    public double getPercentageOfTotal() { return percentageOfTotal; }
    public void setPercentageOfTotal(double percentageOfTotal) { this.percentageOfTotal = percentageOfTotal; }
    
    public String getPerformanceLevel() { return performanceLevel; }
    public void setPerformanceLevel(String performanceLevel) { this.performanceLevel = performanceLevel; }
}
