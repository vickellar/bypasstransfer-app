
package com.bypass.bypasstransers.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class DailyReconciliation {

    @Id
    @GeneratedValue
    private Long id;

    private LocalDate date;
    private double systemBalance;
    private double actualBalance;
    private double difference;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getSystemBalance() {
        return systemBalance;
    }

    public void setSystemBalance(double systemBalance) {
        this.systemBalance = systemBalance;
    }

    public double getActualBalance() {
        return actualBalance;
    }

    public void setActualBalance(double actualBalance) {
        this.actualBalance = actualBalance;
    }

    public double getDifference() {
        return difference;
    }

    public void setDifference(double difference) {
        this.difference = difference;
    }
    
    
}

