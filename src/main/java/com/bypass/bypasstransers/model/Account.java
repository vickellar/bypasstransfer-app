package com.bypass.bypasstransers.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Account {

    public Account() {
    }

    public Account(Long id, String name, double balance, double transferFee) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.transferFee = transferFee;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column
    private double balance;
    private double transferFee;

    @ManyToOne
    private User owner;
    private double lowBalanceThreshold = 50; // default
    private boolean lowBalanceAlertSent = false;

    // getters & setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getTransferFee() {
        return transferFee;
    }

    public void setTransferFee(double transferFee) {
        this.transferFee = transferFee;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public double getLowBalanceThreshold() {
        return lowBalanceThreshold;
    }

    public void setLowBalanceThreshold(double lowBalanceThreshold) {
        this.lowBalanceThreshold = lowBalanceThreshold;
    }

    public boolean isLowBalanceAlertSent() {
        return lowBalanceAlertSent;
    }

    public void setLowBalanceAlertSent(boolean lowBalanceAlertSent) {
        this.lowBalanceAlertSent = lowBalanceAlertSent;
    }
}